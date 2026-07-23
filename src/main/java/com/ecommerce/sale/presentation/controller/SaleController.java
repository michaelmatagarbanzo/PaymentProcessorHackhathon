package com.ecommerce.sale.presentation.controller;

import com.ecommerce.sale.application.usecase.ProcessSaleUseCase;
import com.ecommerce.sale.domain.model.SaleTransaction;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    public SaleController(ProcessSaleUseCase processSaleUseCase, SaleMapper saleMapper) {
        this.processSaleUseCase = processSaleUseCase;
        this.saleMapper = saleMapper;
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
        LOG.info("event=sale.controller.processSale.started correlationId={} terminalId={} invoice={} amount={}",
            resolvedCorrelationId, request.terminalId(), request.invoice(), request.totalAmount());
        SaleTransaction transaction = processSaleUseCase.execute(saleMapper.toCommand(request, resolvedCorrelationId));
        LOG.info("event=sale.controller.processSale.beforeResponse correlationId={} transactionId={} status={}",
            transaction.correlationId(), transaction.transactionId(), transaction.status());
        return ResponseEntity.ok()
            .header("X-Transaction-Id", transaction.transactionId())
            .header("X-Correlation-Id", transaction.correlationId())
            .body(saleMapper.toResponse(transaction));
    }
}