package com.ecommerce.sale.application.port.out;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;

/**
 * Puerto de salida: delegación de autorización al API Switch (AS400/Cybersource).
 * La implementación reside en infrastructure (SwitchApiAdapter).
 */
public interface AuthorizationSwitchPort {

    AuthorizationResponse authorize(SaleTransaction transaction, ProcessSaleCommand command, String accessToken);
}
