package com.ecommerce.sale.application.usecase;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.application.port.out.TransactionRepositoryPort;
import com.ecommerce.sale.domain.exception.DuplicateTransactionException;
import com.ecommerce.sale.domain.model.SaleTransaction;
import java.util.Optional;

/**
 * T044 - Detección de duplicados por clave de negocio.
 *
 * Clave exclusiva de idempotencia:
 * terminalId + invoice + totalAmount + accountNumber + transactionType
 */
public class DuplicateDetectionService {

    private final TransactionRepositoryPort transactionRepositoryPort;

    public DuplicateDetectionService(TransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    public void execute(ProcessSaleCommand command) {
        Optional<SaleTransaction> duplicate = transactionRepositoryPort.findDuplicate(
            command.terminalId(),
            command.invoice(),
            command.totalAmount(),
            command.accountNumber(),
            command.transactionType()
        );
        duplicate.ifPresent(existing -> {
            throw new DuplicateTransactionException(existing.transactionId());
        });
    }
}