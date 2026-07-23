package com.ecommerce.sale.presentation.controller;

import com.ecommerce.sale.application.usecase.ProcessSaleUseCase;
import com.ecommerce.sale.domain.model.SaleTransaction;
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

    @Autowired
    public SaleController(ProcessSaleUseCase processSaleUseCase, SaleMapper saleMapper) {
        this.processSaleUseCase = processSaleUseCase;
        this.saleMapper = saleMapper;
        this.objectMapper = new ObjectMapper();
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
        SaleResponse response = saleMapper.toResponse(transaction);
        SaleResponse.AuthorizationResultDto authorization = response.authorization();
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
            toJson(authorization),
            toJson(response)
        );

        return ResponseEntity.ok()
            .header("X-Transaction-Id", transaction.transactionId())
            .header("X-Correlation-Id", transaction.correlationId())
            .body(response);
    }

    private Map<String, Object> sanitizedRequestPayload(SaleRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terminalId", request.terminalId());
        payload.put("transactionType", request.transactionType());
        payload.put("totalAmount", request.totalAmount());
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