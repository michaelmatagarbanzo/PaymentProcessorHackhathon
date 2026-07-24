package com.ecommerce.sale.presentation.dto;

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