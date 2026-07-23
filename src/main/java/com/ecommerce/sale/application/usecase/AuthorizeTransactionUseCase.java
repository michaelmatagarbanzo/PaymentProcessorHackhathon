package com.ecommerce.sale.application.usecase;

import com.ecommerce.sale.application.port.out.AuthorizationSwitchPort;
import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.domain.exception.AuthorizationSwitchException;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caso de uso: delega la autorización de la transacción al API Switch.
 *
 * Responsabilidades:
 * 1. Invocar AuthorizationSwitchPort.authorize(transaction, command, accessToken).
 * 2. Devolver la AuthorizationResponse recibida.
 * 3. Traducir excepciones técnicas a AuthorizationSwitchException.
 *
 * La interpretación de responseCode y determinación de TransactionStatus
 * se realiza en AuthorizationResponse.toTransactionStatus().
 */
public class AuthorizeTransactionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizeTransactionUseCase.class);

    private final AuthorizationSwitchPort authorizationSwitchPort;
    private final GetSwitchAccessTokenUseCase getSwitchAccessTokenUseCase;

    public AuthorizeTransactionUseCase(AuthorizationSwitchPort authorizationSwitchPort,
                                       GetSwitchAccessTokenUseCase getSwitchAccessTokenUseCase) {
        this.authorizationSwitchPort = authorizationSwitchPort;
        this.getSwitchAccessTokenUseCase = getSwitchAccessTokenUseCase;
    }

    public SaleTransaction execute(SaleTransaction transaction, ProcessSaleCommand command) {
        try {
            String accessToken = getSwitchAccessTokenUseCase.execute();
            LOG.info("event=sale.authorize.beforeSwitch correlationId={} transactionId={}",
                transaction.correlationId(), transaction.transactionId());
            AuthorizationResponse authorizationResponse = authorizationSwitchPort.authorize(transaction, command, accessToken);
            return transaction.withAuthorizationResult(authorizationResponse, java.time.Instant.now());
        } catch (AuthorizationSwitchException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AuthorizationSwitchException(
                "Error al invocar el API Switch para la transacción: " + transaction.transactionId(), ex);
        }
    }
}
