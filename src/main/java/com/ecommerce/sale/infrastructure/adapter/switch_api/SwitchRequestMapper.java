package com.ecommerce.sale.infrastructure.adapter.switch_api;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.AuthorizationSource;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationRequest;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationResponse;
import org.springframework.stereotype.Component;

@Component
public class SwitchRequestMapper {

    public SwitchAuthorizationRequest toRequest(SaleTransaction transaction, ProcessSaleCommand command) {
        SwitchAuthorizationRequest.AuthenticationInformation authenticationInformation = command == null
            || command.authenticationInformation() == null
            ? null
            : new SwitchAuthorizationRequest.AuthenticationInformation(
                command.authenticationInformation().eci(),
                command.authenticationInformation().cavv(),
                command.authenticationInformation().xid(),
                command.authenticationInformation().enrollmentStatus()
            );

        SwitchAuthorizationRequest.TokenizationInformation tokenizationInformation = command == null
            || command.tokenizationInformation() == null
            ? null
            : new SwitchAuthorizationRequest.TokenizationInformation(
                command.tokenizationInformation().wallet(),
                command.tokenizationInformation().device(),
                command.tokenizationInformation().paymentIndicator(),
                command.tokenizationInformation().cryptogramEci(),
                command.tokenizationInformation().cryptogram()
            );

        SwitchAuthorizationRequest.ProcessingInformation processingInformation = command == null
            || command.processingInformation() == null
            ? null
            : new SwitchAuthorizationRequest.ProcessingInformation(
                command.processingInformation().errorCentinel(),
                command.processingInformation().statusReason()
            );

        return new SwitchAuthorizationRequest(
            new SwitchAuthorizationRequest.ClientReferenceInformation(
                transaction.transactionId(),
                transaction.correlationId(),
                transaction.terminalId()
            ),
            new SwitchAuthorizationRequest.TransactionInformation(
                transaction.totalAmount(),
                transaction.transactionType().name(),
                null
            ),
            new SwitchAuthorizationRequest.PaymentInformation(
                new SwitchAuthorizationRequest.Card(
                    transaction.accountNumber(),
                    transaction.expirationDate()
                ),
                transaction.securityValidationResponse(),
                transaction.binValidate()
            ),
            new SwitchAuthorizationRequest.OrderInformation(
                transaction.invoice(),
                transaction.terminalId(),
                transaction.transactionType().name()
            ),
            authenticationInformation,
            tokenizationInformation,
            processingInformation
        );
    }

    public SwitchAuthorizationRequest toRequest(SaleTransaction transaction) {
        return toRequest(transaction, null);
    }

    public AuthorizationResponse toDomainResponse(SwitchAuthorizationResponse response) {
        return new AuthorizationResponse(
            toAuthorizationSource(response.provider()),
            firstNonBlank(response.authorizationCode(), response.referenceCode()),
            firstNonBlank(response.providerResponseCode(), response.providerStatus(), response.status()),
            firstNonBlank(response.providerMessage(), response.status()),
            response.referenceNumber(),
            null,
            null
        );
    }

    private AuthorizationSource toAuthorizationSource(String source) {
        if (source == null || source.isBlank()) {
            return AuthorizationSource.UNKNOWN;
        }
        try {
            return AuthorizationSource.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AuthorizationSource.UNKNOWN;
        }
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
}