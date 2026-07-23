package com.ecommerce.sale.infrastructure.adapter.switch_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SwitchAuthorizationRequest(
    ClientReferenceInformation clientReferenceInformation,
    TransactionInformation transactionInformation,
    PaymentInformation paymentInformation,
    OrderInformation orderInformation,
    AuthenticationInformation authenticationInformation,
    TokenizationInformation tokenizationInformation,
    ProcessingInformation processingInformation
) {

    public record ClientReferenceInformation(
        String code,
        String correlationId,
        String terminalId
    ) {}

    public record TransactionInformation(
        Long totalAmount,
        String transactionType,
        String currency
    ) {}

    public record PaymentInformation(
        Card card,
        String securityValidationResponse,
        Boolean binValidate
    ) {}

    public record Card(
        String accountNumber,
        String expirationDate
    ) {}

    public record OrderInformation(
        Long invoice,
        String terminalId,
        String transactionType
    ) {}

    public record AuthenticationInformation(
        String eci,
        String cavv,
        String xid,
        String enrollmentStatus
    ) {}

    public record TokenizationInformation(
        String wallet,
        String device,
        String paymentIndicator,
        String cryptogramEci,
        String cryptogram
    ) {}

    public record ProcessingInformation(
        String errorCentinel,
        String statusReason
    ) {}
}