package com.ecommerce.sale.domain.exception;

/**
 * Se lanza cuando una solicitud SALE no supera la validación de negocio.
 */
public final class InvalidSaleRequestException extends SaleDomainException {

    public InvalidSaleRequestException(String message) {
        super(message);
    }
}
