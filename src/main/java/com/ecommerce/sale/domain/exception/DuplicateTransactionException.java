package com.ecommerce.sale.domain.exception;

/**
 * Se lanza cuando se detecta una solicitud duplicada por clave de negocio:
 * terminalId + invoice + totalAmount + accountNumber + transactionType.
 */
public final class DuplicateTransactionException extends SaleDomainException {

    private final String existingTransactionId;

    public DuplicateTransactionException(String existingTransactionId) {
        super("Transacción duplicada detectada. transactionId existente: " + existingTransactionId);
        this.existingTransactionId = existingTransactionId;
    }

    public String getExistingTransactionId() {
        return existingTransactionId;
    }
}
