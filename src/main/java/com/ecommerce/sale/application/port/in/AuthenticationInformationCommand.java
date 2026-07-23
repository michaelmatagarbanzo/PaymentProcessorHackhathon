package com.ecommerce.sale.application.port.in;

/**
 * Comando de entrada para datos de autenticación 3DS.
 */
public record AuthenticationInformationCommand(
    String eci,
    String cavv,
    String xid,
    String enrollmentStatus
) {}
