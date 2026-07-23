package com.ecommerce.sale.infrastructure.adapter.switch_api;

import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.AuthorizationSource;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationRequest;
import com.ecommerce.sale.infrastructure.adapter.switch_api.dto.SwitchAuthorizationResponse;
import org.springframework.stereotype.Component;

@Component
public class SwitchRequestMapper {

    public SwitchAuthorizationRequest toRequest(SaleTransaction transaction) {
        return new SwitchAuthorizationRequest(
            transaction.transactionId(),
            transaction.correlationId(),
            transaction.terminalId(),
            transaction.transactionType().name(),
            transaction.totalAmount(),
            transaction.accountNumber(),
            transaction.expirationDate(),
            transaction.invoice(),
            transaction.securityValidationResponse(),
            transaction.binValidate()
        );
    }

    public AuthorizationResponse toDomainResponse(SwitchAuthorizationResponse response) {
        return new AuthorizationResponse(
            toAuthorizationSource(response.authorizationSource()),
            response.authorizationNumber(),
            response.responseCode(),
            response.responseDescription(),
            response.referenceNumber(),
            response.hostDate(),
            response.hostTime()
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
}