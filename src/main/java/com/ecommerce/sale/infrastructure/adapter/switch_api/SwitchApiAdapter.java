package com.ecommerce.sale.infrastructure.adapter.switch_api;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.application.port.out.AuthorizationSwitchPort;
import com.ecommerce.sale.domain.exception.AuthorizationSwitchException;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.infrastructure.adapter.observability.ApplicationInsightsAdapter;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationRequest;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationResponse;
import com.ecommerce.sale.infrastructure.config.SwitchProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class SwitchApiAdapter implements AuthorizationSwitchPort {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchApiAdapter.class);
    private static final String AUTHORIZATION_PATH = "/api/v1/payments";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String EXPECTED_PROVIDER = "appconnector";

    private final RestClient restClient;
    private final String endpoint;
    private final String apiKey;
    private final SwitchRequestMapper switchRequestMapper;
    private final ApplicationInsightsAdapter applicationInsightsAdapter;
    private final ObjectMapper objectMapper;

    public SwitchApiAdapter(SwitchProperties switchProperties,
                            SwitchRequestMapper switchRequestMapper,
                            ApplicationInsightsAdapter applicationInsightsAdapter) {
        String baseUrl = switchProperties.resolveAppconnectorBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("switch.appconnector.base-url no está configurado");
        }
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
        this.endpoint = baseUrl + AUTHORIZATION_PATH;
        this.apiKey = switchProperties.resolveAppconnectorApiKey();
        this.switchRequestMapper = switchRequestMapper;
        this.applicationInsightsAdapter = applicationInsightsAdapter;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @CircuitBreaker(name = "switchApi")
    @Retry(name = "switchApi")
    @Bulkhead(name = "switchApi")
    @TimeLimiter(name = "switchApi")
    @RateLimiter(name = "switchApi")
    public AuthorizationResponse authorize(SaleTransaction transaction, ProcessSaleCommand command) {
        long startedAt = System.currentTimeMillis();
        try {
            validateApiKey();
            SwitchAuthorizationRequest payload = switchRequestMapper.toRequest(transaction, command);
            LOG.info(
                "event=switch.request correlationId={} terminalId={} endpoint={} expectedProvider={} requestPayload={}",
                transaction.correlationId(),
                transaction.terminalId(),
                endpoint,
                EXPECTED_PROVIDER,
                toJson(sanitizeRequestPayload(payload))
            );
            applicationInsightsAdapter.event("switch.request", Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "endpoint", endpoint,
                "provider", EXPECTED_PROVIDER
            ));

            RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri(AUTHORIZATION_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, apiKey);

            if (transaction.correlationId() != null && !transaction.correlationId().isBlank()) {
                requestSpec.header(CORRELATION_HEADER, transaction.correlationId());
            }

            // Build diagnostics and publish event
            Map<String, Object> diag = Map.of(
                "endpoint", endpoint,
                "method", "POST",
                "provider", EXPECTED_PROVIDER,
                "headersSent", Map.of(
                    "Content-Type", "application/json",
                    API_KEY_HEADER, maskApiKey(apiKey),
                    CORRELATION_HEADER, transaction.correlationId() == null ? "" : transaction.correlationId()
                ),
                "requestPayload", sanitizeRequestPayload(payload),
                "timestamp", System.currentTimeMillis()
            );

            // store diagnostics for controller to include in API response if needed
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("correlationId", safe(transaction.correlationId()));
            context.put("transactionId", safe(transaction.transactionId()));
            context.put("diagnostics", new LinkedHashMap<>(diag));
            SwitchDiagnosticsContext.set(context);

            LOG.info("event=switch.http.request payload={}", toJson(Map.of(
                "event", "switch.http.request",
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "endpoint", endpoint,
                "method", "POST",
                "provider", EXPECTED_PROVIDER,
                "headers", Map.of(API_KEY_HEADER, maskApiKey(apiKey), CORRELATION_HEADER, transaction.correlationId() == null ? "" : transaction.correlationId()),
                "body", sanitizeRequestPayload(payload),
                "timestamp", System.currentTimeMillis()
            )));

            // application insights event (string attributes)
            applicationInsightsAdapter.event("switch.http.request", Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "endpoint", endpoint,
                "method", "POST",
                "provider", EXPECTED_PROVIDER
            ));

            SwitchAuthorizationResponse response = requestSpec
                .body(payload)
                .retrieve()
                .body(SwitchAuthorizationResponse.class);

            if (response == null) {
                throw new AuthorizationSwitchException("El API Switch devolvió una respuesta vacía");
            }

            long duration = System.currentTimeMillis() - startedAt;
            // Log and publish response diagnostics
            Map<String, Object> responseDiag = Map.of(
                "endpoint", endpoint,
                "statusCode", "UNKNOWN",
                "durationMs", duration,
                "responsePayload", response
            );
            // augment stored diagnostics
            Map<String, Object> stored = SwitchDiagnosticsContext.get();
            if (stored != null && stored.get("diagnostics") instanceof Map<?, ?> diagnosticsMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mutableDiagnostics = (Map<String, Object>) diagnosticsMap;
                mutableDiagnostics.put("response", responseDiag);
            }

            LOG.info("event=switch.http.response payload={}", toJson(Map.of(
                "event", "switch.http.response",
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "endpoint", endpoint,
                "statusCode", "UNKNOWN",
                "durationMs", duration,
                "response", response
            )));

            applicationInsightsAdapter.event("switch.http.response", Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "endpoint", endpoint,
                "provider", safe(response.provider())
            ));
            applicationInsightsAdapter.increment("switch.authorization.total",
                Map.of("status", response.providerResponseCode() == null ? "UNKNOWN" : response.providerResponseCode()));
            applicationInsightsAdapter.timing("switch.authorization.latency", duration,
                Map.of("status", response.providerResponseCode() == null ? "UNKNOWN" : response.providerResponseCode()));
            LOG.info(
                "event=switch.response correlationId={} transactionId={} provider={} status={} responseCode={} responsePayload={}",
                transaction.correlationId(),
                transaction.transactionId(),
                safe(response.provider()),
                safe(response.status()),
                safe(response.providerResponseCode()),
                toJson(response)
            );
            applicationInsightsAdapter.event("switch.response", Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "provider", safe(response.provider()),
                "status", safe(response.status()),
                "responseCode", response.providerResponseCode() == null ? "UNKNOWN" : response.providerResponseCode()
            ));

            return switchRequestMapper.toDomainResponse(response);
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startedAt;
            LOG.error("event=switch.authorization.failed dependency=switch correlationId={} transactionId={} durationMs={} error={}",
                transaction.correlationId(), transaction.transactionId(), duration, ex.getMessage());
            LOG.error("event=sale.error correlationId={} transactionId={} errorType={} errorCode={} errorMessage={}",
                safe(transaction.correlationId()),
                safe(transaction.transactionId()),
                ex.getClass().getSimpleName(),
                "SWITCH_AUTHORIZATION_FAILED",
                ex.getMessage(),
                ex);
            // publish http error diagnostics
            Map<String, Object> stored = SwitchDiagnosticsContext.get();
            Map<String, Object> headersSent = Map.of(
                "Content-Type", "application/json",
                API_KEY_HEADER, maskApiKey(apiKey),
                CORRELATION_HEADER, transaction.correlationId() == null ? "" : transaction.correlationId()
            );
            Map<String, String> aiAttrs = Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "endpoint", endpoint,
                "error", ex.getClass().getSimpleName()
            );
            applicationInsightsAdapter.event("switch.http.error", aiAttrs);
            Map<String, Object> httpErrorLog = new LinkedHashMap<>();
            httpErrorLog.put("event", "switch.http.error");
            httpErrorLog.put("correlationId", safe(transaction.correlationId()));
            httpErrorLog.put("transactionId", safe(transaction.transactionId()));
            httpErrorLog.put("endpoint", endpoint);
            httpErrorLog.put("headersSent", headersSent);
            httpErrorLog.put("statusCode", "UNKNOWN");
            httpErrorLog.put("exceptionType", ex.getClass().getSimpleName());
            httpErrorLog.put("exceptionMessage", ex.getMessage());
            httpErrorLog.put("responseBody", stored != null ? stored.get("response") : null);
            LOG.info("event=switch.http.error payload={}", toJson(httpErrorLog));
            applicationInsightsAdapter.increment("switch.authorization.total", Map.of("status", "ERROR"));
            applicationInsightsAdapter.timing("switch.authorization.latency", duration, Map.of("status", "ERROR"));
            applicationInsightsAdapter.event("switch.authorization.failed", Map.of(
                "dependency", "switch",
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "error", ex.getClass().getSimpleName()
            ));
            if (isExternalDependencyFailure(ex)) {
                throw new ExternalDependencyUnavailableException("switch", ex);
            }
            if (ex instanceof AuthorizationSwitchException) {
                throw ex;
            }
            throw new AuthorizationSwitchException(
                "Error técnico al invocar autorización del API Switch para transactionId="
                    + transaction.transactionId(), ex);
        }
    }

    private String safe(String value) {
        return value == null ? "unknown" : value;
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AuthorizationSwitchException("switch.appconnector.api-key no está configurado");
        }
    }

    private SwitchAuthorizationRequest sanitizeRequestPayload(SwitchAuthorizationRequest payload) {
        if (payload == null || payload.paymentInformation() == null || payload.paymentInformation().card() == null) {
            return payload;
        }

        SwitchAuthorizationRequest.Card safeCard = new SwitchAuthorizationRequest.Card(
            maskAccountNumber(payload.paymentInformation().card().accountNumber()),
            payload.paymentInformation().card().expirationDate()
        );

        SwitchAuthorizationRequest.PaymentInformation safePayment = new SwitchAuthorizationRequest.PaymentInformation(
            safeCard,
            payload.paymentInformation().securityValidationResponse(),
            payload.paymentInformation().binValidate()
        );

        return new SwitchAuthorizationRequest(
            payload.clientReferenceInformation(),
            payload.transactionInformation(),
            safePayment,
            payload.orderInformation(),
            payload.authenticationInformation(),
            payload.tokenizationInformation(),
            payload.processingInformation()
        );
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "[REDACTED]";
        }

        String digitsOnly = accountNumber.replaceAll("\\D", "");
        if (digitsOnly.length() < 10) {
            return "[REDACTED]";
        }

        int prefixLength = Math.min(6, digitsOnly.length() - 4);
        int maskedLength = digitsOnly.length() - prefixLength - 4;
        return digitsOnly.substring(0, prefixLength)
            + "*".repeat(Math.max(maskedLength, 0))
            + digitsOnly.substring(digitsOnly.length() - 4);
    }

    private String maskApiKey(String key) {
        if (key == null || key.isBlank()) {
            return "[REDACTED]";
        }
        int visible = Math.min(7, key.length());
        String prefix = key.substring(0, visible);
        int maskedLen = Math.max(0, key.length() - visible);
        return prefix + "*".repeat(maskedLen);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getClass().getSimpleName() + "\"}";
        }
    }

    private boolean isExternalDependencyFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ExternalDependencyUnavailableException
                || current instanceof ResourceAccessException
                || current instanceof HttpServerErrorException
                || current instanceof CallNotPermittedException
                || current instanceof TimeoutException
                || current instanceof ConnectException
                || current instanceof SocketTimeoutException
                || current.getClass().getName().equals("org.springframework.web.reactive.function.client.WebClientRequestException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}