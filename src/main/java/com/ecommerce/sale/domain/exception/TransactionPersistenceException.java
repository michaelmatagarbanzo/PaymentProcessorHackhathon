package com.ecommerce.sale.domain.exception;

/**
 * Se lanza cuando falla la persistencia de la transacción en MongoDB.
 */
public final class TransactionPersistenceException extends SaleDomainException {

    public TransactionPersistenceException(String message) {
        super(message);
    }

    public TransactionPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
