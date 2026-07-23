package com.ecommerce.sale.application.usecase;

import com.ecommerce.sale.application.port.out.SwitchAuthenticationPort;
import com.ecommerce.sale.domain.exception.SwitchAuthenticationException;

/**
 * Caso de uso: obtiene el token de acceso OAuth 2.0 para el API Switch.
 * Delega al puerto SwitchAuthenticationPort (implementado en infrastructure).
 * El caché del token es responsabilidad del adaptador.
 */
public class GetSwitchAccessTokenUseCase {

    private final SwitchAuthenticationPort switchAuthenticationPort;

    public GetSwitchAccessTokenUseCase(SwitchAuthenticationPort switchAuthenticationPort) {
        this.switchAuthenticationPort = switchAuthenticationPort;
    }

    public String execute() {
        try {
            return switchAuthenticationPort.getAccessToken();
        } catch (RuntimeException ex) {
            throw new SwitchAuthenticationException(
                "No se pudo obtener el token de acceso para el API Switch", ex);
        }
    }
}
