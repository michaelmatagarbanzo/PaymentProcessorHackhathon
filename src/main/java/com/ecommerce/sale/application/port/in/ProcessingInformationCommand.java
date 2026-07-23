package com.ecommerce.sale.application.port.in;

/**
 * Comando de entrada para información de procesamiento (centinel/switch).
 */
public record ProcessingInformationCommand(
    String errorCentinel,
    String statusReason
) {}
