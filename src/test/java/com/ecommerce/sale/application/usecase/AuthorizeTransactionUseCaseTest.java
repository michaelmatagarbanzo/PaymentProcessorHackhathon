package com.ecommerce.sale.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ecommerce.sale.application.port.out.AuthorizationSwitchPort;
import com.ecommerce.sale.domain.exception.AuthorizationSwitchException;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.AuthorizationSource;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizeTransactionUseCaseTest {

    @Mock
    private AuthorizationSwitchPort authorizationSwitchPort;

    @Mock
    private GetSwitchAccessTokenUseCase getSwitchAccessTokenUseCase;

    private AuthorizeTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthorizeTransactionUseCase(authorizationSwitchPort, getSwitchAccessTokenUseCase);
    }

    @Test
    void shouldAuthorizeTransactionAndReturnUpdatedStatus() {
        SaleTransaction pending = pendingTransaction();
        AuthorizationResponse response = new AuthorizationResponse(
            AuthorizationSource.AS400,
            "AUTH001",
            "00",
            "Aprobado",
            "REF001",
            "0722",
            "143000"
        );

        when(getSwitchAccessTokenUseCase.execute()).thenReturn("token-123");
        when(authorizationSwitchPort.authorize(pending, "token-123")).thenReturn(response);

        SaleTransaction result = useCase.execute(pending);

        assertEquals(TransactionStatus.AUTHORIZED, result.status());
        assertEquals("AUTH001", result.authorizationResult().authorizationNumber());
    }

    @Test
    void shouldPropagateDomainAuthorizationException() {
        SaleTransaction pending = pendingTransaction();

        when(getSwitchAccessTokenUseCase.execute()).thenReturn("token-123");
        when(authorizationSwitchPort.authorize(any(), any()))
            .thenThrow(new AuthorizationSwitchException("switch unavailable"));

        assertThrows(AuthorizationSwitchException.class, () -> useCase.execute(pending));
    }

    @Test
    void shouldWrapUnexpectedExceptionAsAuthorizationSwitchException() {
        SaleTransaction pending = pendingTransaction();

        when(getSwitchAccessTokenUseCase.execute()).thenReturn("token-123");
        when(authorizationSwitchPort.authorize(any(), any()))
            .thenThrow(new IllegalStateException("boom"));

        AuthorizationSwitchException ex = assertThrows(
            AuthorizationSwitchException.class,
            () -> useCase.execute(pending)
        );

        assertEquals("Error al invocar el API Switch para la transacción: " + pending.transactionId(), ex.getMessage());
    }

    private SaleTransaction pendingTransaction() {
        Instant now = Instant.now();
        return new SaleTransaction(
            "7c9e6679-7425-40de-944b-e07fc1f90ae7",
            "550e8400-e29b-41d4-a716-446655440000",
            "TERM-0001",
            TransactionType.SALE,
            5633L,
            "55189800****2751",
            "2805",
            14611279L,
            "1",
            true,
            TransactionStatus.PENDING,
            null,
            now,
            null,
            now
        );
    }
}
