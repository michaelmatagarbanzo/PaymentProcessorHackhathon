package com.ecommerce.sale.domain.model;

import java.util.Objects;

/**
 * Value Object que encapsula la respuesta del API Switch.
 * responseCode "00" → AUTHORIZED; cualquier otro código de negocio → DECLINED;
 * errores técnicos/timeout se mapean a ERROR en la capa de caso de uso.
 */
public record AuthorizationResponse(
    AuthorizationSource authorizationSource,
    String authorizationNumber,
    String responseCode,
    String responseDescription,
    String referenceNumber,
    String hostDate,
    String hostTime
) {

    public AuthorizationResponse {
        Objects.requireNonNull(responseCode, "responseCode no puede ser null");
        Objects.requireNonNull(authorizationSource, "authorizationSource no puede ser null");
    }

    public boolean isApproved() {
        return "00".equals(responseCode);
    }

    public TransactionStatus toTransactionStatus() {
        return isApproved() ? TransactionStatus.AUTHORIZED : TransactionStatus.DECLINED;
    }
}
