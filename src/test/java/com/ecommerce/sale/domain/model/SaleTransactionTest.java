package com.ecommerce.sale.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SaleTransactionTest {

    @Test
    void shouldCreatePendingSaleTransaction() {
        SaleTransaction transaction = pendingTransaction();

        assertEquals(TransactionStatus.PENDING, transaction.status());
        assertEquals(TransactionType.SALE, transaction.transactionType());
        assertEquals(5633L, transaction.totalAmount());
    }

    @Test
    void shouldRejectNullTransactionType() {
        NullPointerException ex = assertThrows(
            NullPointerException.class,
            () -> new SaleTransaction(
                "txn-1",
                "corr-1",
                "TERM-001",
                null,
                100L,
                "55189800****2751",
                "2805",
                1001L,
                "1",
                true,
                TransactionStatus.PENDING,
                null,
                Instant.now(),
                null,
                Instant.now()
            )
        );

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("transactionType"));
    }

    @Test
    void shouldMoveToAuthorizedWhenSwitchApproves() {
        SaleTransaction transaction = pendingTransaction();
        AuthorizationResponse response = new AuthorizationResponse(
            AuthorizationSource.AS400,
            "AUTH001",
            "00",
            "Aprobado",
            "REF001",
            "0722",
            "143000"
        );

        SaleTransaction authorized = transaction.withAuthorizationResult(response, Instant.now());

        assertEquals(TransactionStatus.AUTHORIZED, authorized.status());
        assertEquals("00", authorized.authorizationResult().responseCode());
    }

    @Test
    void shouldNotMutateWhenAlreadyTerminal() {
        SaleTransaction terminal = pendingTransaction().withError(Instant.now());

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> terminal.withError(Instant.now())
        );

        assertTrue(ex.getMessage().contains("estado terminal"));
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
