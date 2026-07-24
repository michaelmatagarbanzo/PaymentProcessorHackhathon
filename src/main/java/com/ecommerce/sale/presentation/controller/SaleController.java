package com.ecommerce.sale.presentation.controller;

import com.ecommerce.sale.application.usecase.ProcessSaleUseCase;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.sale.presentation.dto.SaleRequest;
import com.ecommerce.sale.presentation.dto.SaleResponse;
import com.ecommerce.sale.presentation.mapper.SaleMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/sales")
@Tag(name = "Transacciones SALE")
@SecurityRequirement(name = "bearerAuth")
public class SaleController {

    private static final Logger LOG = LoggerFactory.getLogger(SaleController.class);

    private final ProcessSaleUseCase processSaleUseCase;
    private final SaleMapper saleMapper;
    private final ObjectMapper objectMapper;
    private final org.springframework.core.env.Environment environment;
    private final boolean appDebugEnabled;

    @Autowired
    public SaleController(ProcessSaleUseCase processSaleUseCase, SaleMapper saleMapper,
                          org.springframework.core.env.Environment environment,
                          @org.springframework.beans.factory.annotation.Value("${app.debug.enabled:false}") boolean appDebugEnabled) {
        this.processSaleUseCase = processSaleUseCase;
        this.saleMapper = saleMapper;
        this.objectMapper = new ObjectMapper();
        this.environment = environment;
        this.appDebugEnabled = appDebugEnabled;
    }

    @PostMapping
    @Operation(
        summary = "Procesar transaccion SALE",
        description = "Procesa una solicitud SALE, delega autorizacion al API Switch y persiste el resultado.",
        operationId = "processSaleTransaction",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SaleRequest.class)
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transaccion procesada",
            headers = {
                @Header(name = "X-Correlation-Id", schema = @Schema(type = "string", format = "uuid")),
                @Header(name = "X-Transaction-Id", schema = @Schema(type = "string", format = "uuid"))
            },
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SaleResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error de validacion o regla de negocio",
            content = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "JWT ausente, invalido o expirado",
            content = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "JWT valido pero sin permisos",
            content = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Dependencia externa no disponible",
            headers = {
                @Header(name = "Retry-After", schema = @Schema(type = "integer"))
            },
            content = @Content(mediaType = "application/problem+json")
        )
    })
    public ResponseEntity<SaleResponse> processSale(
        @Parameter(
            name = "X-Correlation-Id",
            description = "UUID para trazabilidad; si no llega, la API lo genera",
            required = false,
            schema = @Schema(type = "string", format = "uuid")
        )
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
        @Valid @RequestBody SaleRequest request
    ) {
        String resolvedCorrelationId = correlationId;
        if (resolvedCorrelationId == null || resolvedCorrelationId.isBlank()) {
            resolvedCorrelationId = MDC.get("correlationId");
        }

        LOG.info(
            "event=sale.request.received correlationId={} transactionId={} terminalId={} transactionType={} invoice={} totalAmount={} currency={} merchantId={} merchantName={} commerceName={} processingCode={} expirationDate={} accountNumberMasked={} requestPayload={}",
            resolvedCorrelationId,
            "pending",
            request.terminalId(),
            request.transactionType(),
            request.invoice(),
            request.totalAmount(),
            null,
            null,
            null,
            null,
            request.processingInformation() != null ? request.processingInformation().statusReason() : null,
            request.expirationDate(),
            maskAccountNumber(request.accountNumber()),
            toJson(sanitizedRequestPayload(request))
        );

        LOG.info("event=sale.controller.processSale.started correlationId={} terminalId={} invoice={} amount={}",
            resolvedCorrelationId, request.terminalId(), request.invoice(), request.totalAmount());
        SaleTransaction transaction = processSaleUseCase.execute(saleMapper.toCommand(request, resolvedCorrelationId));
        LOG.info("event=sale.controller.processSale.beforeResponse correlationId={} transactionId={} status={}",
            transaction.correlationId(), transaction.transactionId(), transaction.status());

        SaleResponse mapped = saleMapper.toResponse(transaction);
        Map<String, Object> switchContext = com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchDiagnosticsContext.pop();
        if (switchContext == null) {
            switchContext = com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchDiagnosticsContext.popByCorrelationId(transaction.correlationId());
        }
        Map<String, Object> diagnostics = switchContext == null ? null : asMap(switchContext.get("diagnostics"));
        SwitchAuthorizationResponse switchResponse = extractSwitchResponse(diagnostics);

        SaleResponse.AuthorizationResultDto authorization = toAuthorizationResultDto(mapped.authorization(), switchResponse);

        SaleResponse response = new SaleResponse(
            mapped.transactionId(),
            mapped.correlationId(),
            mapped.status(),
            mapped.terminalId(),
            mapped.totalAmount(),
            request.currency() == null || request.currency().isBlank() ? "USD" : request.currency(),
            null,
            authorization,
            mapped.processingDateTime(),
            mapped.createdAt(),
            diagnostics
        );

        LOG.info(
            "event=sale.response.generated correlationId={} transactionId={} status={} responseCode={} responseMessage={} authorizationCode={} authorizationSource={} processingDateTime={} switchResponse={} responsePayload={}",
            response.correlationId(),
            response.transactionId(),
            response.status(),
            authorization != null ? authorization.responseCode() : null,
            authorization != null ? authorization.responseDescription() : null,
            authorization != null ? authorization.authorizationNumber() : null,
            authorization != null ? authorization.authorizationSource() : null,
            response.processingDateTime(),
            toJson(switchResponse),
            toJson(response)
        );

        return ResponseEntity.ok()
            .header("X-Transaction-Id", transaction.transactionId())
            .header("X-Correlation-Id", transaction.correlationId())
            .body(response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private SwitchAuthorizationResponse extractSwitchResponse(Map<String, Object> diagnostics) {
        if (diagnostics == null) {
            return null;
        }
        Map<String, Object> response = asMap(diagnostics.get("response"));
        if (response == null) {
            return null;
        }
        Object payload = response.get("responsePayload");
        if (payload instanceof SwitchAuthorizationResponse typedPayload) {
            return typedPayload;
        }
        if (payload instanceof Map<?, ?> payloadMap) {
            return objectMapper.convertValue(payloadMap, SwitchAuthorizationResponse.class);
        }
        return null;
    }


    private SaleResponse.AuthorizationResultDto toAuthorizationResultDto(
        SaleResponse.AuthorizationResultDto mapped,
        SwitchAuthorizationResponse switchResponse
    ) {
        String authorizationSource = firstNonBlank(
            mapProviderToAuthorizationSource(switchResponse == null ? null : switchResponse.provider()),
            mapped == null ? null : mapped.authorizationSource()
        );
        String authorizationNumber = firstNonBlank(
            switchResponse == null ? null : switchResponse.authorizationCode(),
            mapped == null ? null : mapped.authorizationNumber()
        );
        String responseCode = firstNonBlank(
            switchResponse == null ? null : switchResponse.providerResponseCode(),
            switchResponse == null ? null : switchResponse.providerStatus(),
            switchResponse == null ? null : switchResponse.status(),
            mapped == null ? null : mapped.responseCode()
        );
        String responseDescription = firstNonBlank(
            switchResponse == null ? null : switchResponse.providerMessage(),
            switchResponse == null ? null : switchResponse.providerStatus(),
            switchResponse == null ? null : switchResponse.status(),
            mapped == null ? null : mapped.responseDescription()
        );
        String referenceNumber = firstNonBlank(
            switchResponse == null ? null : switchResponse.referenceNumber(),
            mapped == null ? null : mapped.referenceNumber()
        );

        return new SaleResponse.AuthorizationResultDto(
            authorizationSource,
            authorizationNumber,
            responseCode,
            responseDescription,
            referenceNumber,
            mapped == null ? null : mapped.hostDate(),
            mapped == null ? null : mapped.hostTime()
        );
    }

    private String mapProviderToAuthorizationSource(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        if (provider.equalsIgnoreCase("WCF_Pasarelas") || provider.equalsIgnoreCase("AS400")) {
            return "AS400";
        }
        if (provider.equalsIgnoreCase("CyberSource") || provider.equalsIgnoreCase("CYBERSOURCE")) {
            return "CYBERSOURCE";
        }
        return provider;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> sanitizedRequestPayload(SaleRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminalId", request.terminalId());
        payload.put("transactionType", request.transactionType());
        payload.put("totalAmount", request.totalAmount());
        payload.put("currency", request.currency() == null || request.currency().isBlank() ? "USD" : request.currency());
        payload.put("accountNumber", maskAccountNumber(request.accountNumber()));
        payload.put("expirationDate", request.expirationDate());
        payload.put("invoice", request.invoice());
        payload.put("securityCodeEntry", "[REDACTED]");
        payload.put("securityValidationResponse", "[REDACTED]");
        payload.put("binValidate", request.binValidate());
        payload.put("authenticationInformation", request.authenticationInformation());
        payload.put("tokenizationInformation", request.tokenizationInformation());
        payload.put("processingInformation", request.processingInformation());
        return payload;
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
        int suffixLength = 4;
        int maskedLength = digitsOnly.length() - prefixLength - suffixLength;
        return digitsOnly.substring(0, prefixLength)
            + "*".repeat(Math.max(maskedLength, 0))
            + digitsOnly.substring(digitsOnly.length() - suffixLength);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getClass().getSimpleName() + "\"}";
        }
    }
}