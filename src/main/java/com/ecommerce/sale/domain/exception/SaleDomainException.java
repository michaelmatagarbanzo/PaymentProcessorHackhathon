package com.ecommerce.sale.domain.exception;

/**
 * Excepción base del dominio SALE. No extiende RuntimeException de Spring.
 */
public abstract class SaleDomainException extends RuntimeException {

    protected SaleDomainException(String message) {
        super(message);
    }

    protected SaleDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
