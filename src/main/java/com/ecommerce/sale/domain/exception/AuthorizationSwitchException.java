package com.ecommerce.sale.domain.exception;

/**
 * Se lanza cuando el API Switch no está disponible o responde con error técnico/timeout.
 */
public final class AuthorizationSwitchException extends SaleDomainException {

    public AuthorizationSwitchException(String message) {
        super(message);
    }

    public AuthorizationSwitchException(String message, Throwable cause) {
        super(message, cause);
    }
}
