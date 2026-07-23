package com.ecommerce.sale.domain.exception;

/**
 * Se lanza cuando no se puede obtener o renovar el token OAuth hacia el API Switch.
 */
public final class SwitchAuthenticationException extends SaleDomainException {

    public SwitchAuthenticationException(String message) {
        super(message);
    }

    public SwitchAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
