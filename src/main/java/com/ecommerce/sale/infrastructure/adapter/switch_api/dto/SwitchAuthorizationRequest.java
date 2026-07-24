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
        String ecspLogId
    ) {}

    public record TransactionInformation(
        String transactionType,
        String terminalId,
        String entryMode
    ) {}

    public record PaymentInformation(
        Card card
    ) {}

    public record Card(
        String number,
        String expirationMonth,
        String expirationYear,
        String securityCode,
        String cardHolderName
    ) {}

    public record OrderInformation(
        AmountDetails amountDetails
    ) {}

    public record AmountDetails(
        String totalAmount,
        String currency
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