package com.ecommerce.sale.application.port.out;

/**
 * Puerto de salida: obtención del token de acceso OAuth 2.0 para el API Switch.
 * La implementación reside en infrastructure (SwitchOAuthAdapter).
 */
public interface SwitchAuthenticationPort {

    String getAccessToken();
}
