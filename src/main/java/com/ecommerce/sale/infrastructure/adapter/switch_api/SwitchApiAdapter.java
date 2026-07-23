package com.ecommerce.sale.infrastructure.adapter.switch_api;

import com.ecommerce.sale.application.port.out.AuthorizationSwitchPort;
import com.ecommerce.sale.domain.exception.AuthorizationSwitchException;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.infrastructure.adapter.observability.ApplicationInsightsAdapter;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationRequest;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationResponse;
import com.ecommerce.sale.infrastructure.config.SwitchProperties;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
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
    private static final String AUTHORIZATION_PATH = "/api/switch/authorize";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final RestClient restClient;
    private final SwitchRequestMapper switchRequestMapper;
    private final ApplicationInsightsAdapter applicationInsightsAdapter;

    public SwitchApiAdapter(SwitchProperties switchProperties,
                            SwitchRequestMapper switchRequestMapper,
                            ApplicationInsightsAdapter applicationInsightsAdapter) {
        this.restClient = RestClient.builder()
            .baseUrl(switchProperties.getBaseUrl())
            .build();
        this.switchRequestMapper = switchRequestMapper;
        this.applicationInsightsAdapter = applicationInsightsAdapter;
    }

    @Override
    @CircuitBreaker(name = "switchApi")
    @Retry(name = "switchApi")
    @Bulkhead(name = "switchApi")
    @TimeLimiter(name = "switchApi")
    @RateLimiter(name = "switchApi")
    public AuthorizationResponse authorize(SaleTransaction transaction, String accessToken) {
        long startedAt = System.currentTimeMillis();
        try {
            SwitchAuthorizationRequest payload = switchRequestMapper.toRequest(transaction);
            LOG.info("event=switch.authorization.started correlationId={} transactionId={}",
                transaction.correlationId(), transaction.transactionId());
            applicationInsightsAdapter.event("switch.authorization.started", Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId())
            ));

            RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri(AUTHORIZATION_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken);

            if (transaction.correlationId() != null && !transaction.correlationId().isBlank()) {
                requestSpec.header(CORRELATION_HEADER, transaction.correlationId());
            }

            SwitchAuthorizationResponse response = requestSpec
                .body(payload)
                .retrieve()
                .body(SwitchAuthorizationResponse.class);

            if (response == null) {
                throw new AuthorizationSwitchException("El API Switch devolvió una respuesta vacía");
            }

            long duration = System.currentTimeMillis() - startedAt;
            applicationInsightsAdapter.increment("switch.authorization.total",
                Map.of("status", response.responseCode() == null ? "UNKNOWN" : response.responseCode()));
            applicationInsightsAdapter.timing("switch.authorization.latency", duration,
                Map.of("status", response.responseCode() == null ? "UNKNOWN" : response.responseCode()));
            LOG.info("event=switch.authorization.completed correlationId={} transactionId={} responseCode={} durationMs={}",
                transaction.correlationId(), transaction.transactionId(), response.responseCode(), duration);
            applicationInsightsAdapter.event("switch.authorization.completed", Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "responseCode", response.responseCode() == null ? "UNKNOWN" : response.responseCode()
            ));

            return switchRequestMapper.toDomainResponse(response);
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startedAt;
            LOG.error("event=switch.authorization.failed dependency=switch correlationId={} transactionId={} durationMs={} error={}",
                transaction.correlationId(), transaction.transactionId(), duration, ex.getMessage());
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