package com.ecommerce.sale.infrastructure.adapter.switch_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SwitchAuthorizationRequest(
    String transactionId,
    String correlationId,
    String terminalId,
    String transactionType,
    Long totalAmount,
    String accountNumber,
    String expirationDate,
    Long invoice,
    String securityValidationResponse,
    Boolean binValidate
) {}