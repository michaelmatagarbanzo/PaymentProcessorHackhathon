package com.ecommerce.sale.infrastructure.adapter.switch_api;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.AuthorizationSource;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationRequest;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationResponse;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SwitchRequestMapper {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_ENTRY_MODE = "MNL";

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
                String.valueOf(transaction.invoice()),
                "C2P-" + transaction.invoice()
            ),
            new SwitchAuthorizationRequest.TransactionInformation(
                transaction.transactionType().name(),
                transaction.terminalId(),
                DEFAULT_ENTRY_MODE
            ),
            new SwitchAuthorizationRequest.PaymentInformation(
                new SwitchAuthorizationRequest.Card(
                    transaction.accountNumber(),
                    resolveExpirationMonth(transaction.expirationDate()),
                    resolveExpirationYear(transaction.expirationDate()),
                    command == null ? null : command.securityCodeEntry(),
                    command == null ? null : command.cardHolderName()
                )
            ),
            new SwitchAuthorizationRequest.OrderInformation(
                new SwitchAuthorizationRequest.AmountDetails(
                    formatAmount(transaction.totalAmount()),
                    resolveCurrency(command)
                )
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

    private String formatAmount(Long amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format(Locale.US, "%.2f", amount.doubleValue());
    }

    private String resolveCurrency(ProcessSaleCommand command) {
        if (command == null || command.currency() == null || command.currency().isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return command.currency().toUpperCase(Locale.ROOT);
    }

    private String resolveExpirationMonth(String expirationDate) {
        if (expirationDate == null || expirationDate.length() != 4) {
            return null;
        }
        String first = expirationDate.substring(0, 2);
        String second = expirationDate.substring(2, 4);
        int firstNum = parseTwoDigits(first);
        int secondNum = parseTwoDigits(second);

        if (firstNum > 12) {
            return second;
        }
        if (secondNum > 12) {
            return first;
        }
        return second;
    }

    private String resolveExpirationYear(String expirationDate) {
        if (expirationDate == null || expirationDate.length() != 4) {
            return null;
        }
        String first = expirationDate.substring(0, 2);
        String second = expirationDate.substring(2, 4);
        int firstNum = parseTwoDigits(first);
        int secondNum = parseTwoDigits(second);
        String yy = firstNum > 12 || secondNum <= 12 ? first : second;
        return "20" + yy;
    }

    private int parseTwoDigits(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}