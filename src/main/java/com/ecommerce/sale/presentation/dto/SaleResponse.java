package com.ecommerce.sale.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

public record SaleResponse(
    String transactionId,
    String correlationId,
    String status,
    String terminalId,
    Long totalAmount,
    String currency,
    String cardHolderName,
    AuthorizationResultDto authorization,
    Instant processingDateTime,
    Instant createdAt,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    java.util.Map<String, Object> diagnostics
) {

    public record AuthorizationResultDto(
        String authorizationSource,
        String authorizationNumber,
        String responseCode,
        String responseDescription,
        String referenceNumber,
        String hostDate,
        String hostTime
    ) {}
}