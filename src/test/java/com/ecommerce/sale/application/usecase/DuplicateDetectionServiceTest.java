package com.ecommerce.sale.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.application.port.out.TransactionRepositoryPort;
import com.ecommerce.sale.domain.exception.DuplicateTransactionException;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    private DuplicateDetectionService duplicateDetectionService;

    @BeforeEach
    void setUp() {
        duplicateDetectionService = new DuplicateDetectionService(transactionRepositoryPort);
    }

    @Test
    void shouldContinueWhenNoDuplicateExists() {
        ProcessSaleCommand command = validCommand();
        when(transactionRepositoryPort.findDuplicate(
            command.terminalId(),
            command.invoice(),
            command.totalAmount(),
            command.accountNumber(),
            command.transactionType()
        )).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> duplicateDetectionService.execute(command));
    }

    @Test
    void shouldThrowDuplicateTransactionExceptionWhenDuplicateExists() {
        ProcessSaleCommand command = validCommand();
        SaleTransaction existing = existingTransaction();
        when(transactionRepositoryPort.findDuplicate(
            command.terminalId(),
            command.invoice(),
            command.totalAmount(),
            command.accountNumber(),
            command.transactionType()
        )).thenReturn(Optional.of(existing));

        assertThrows(DuplicateTransactionException.class, () -> duplicateDetectionService.execute(command));
    }

    private ProcessSaleCommand validCommand() {
        return new ProcessSaleCommand(
            "corr-1",
            "TERM-0001",
            "SALE",
            5633L,
            "USD",
            "55189800****2751",
            "Test User",
            "2805",
            14611279L,
            "123",
            "1",
            true,
            null,
            null,
            null
        );
    }

    private SaleTransaction existingTransaction() {
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
