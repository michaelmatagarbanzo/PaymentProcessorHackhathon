package com.ecommerce.sale.application.usecase;

import com.ecommerce.sale.application.port.out.TransactionRepositoryPort;
import com.ecommerce.sale.domain.exception.TransactionPersistenceException;
import com.ecommerce.sale.domain.model.SaleTransaction;

/**
 * Caso de uso: persiste la transacción en el repositorio.
 * Delega al puerto TransactionRepositoryPort (implementado en infrastructure).
 */
public class PersistTransactionUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;

    public PersistTransactionUseCase(TransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    public void execute(SaleTransaction transaction) {
        try {
            transactionRepositoryPort.save(transaction);
        } catch (RuntimeException ex) {
            throw new TransactionPersistenceException(
                "Error al persistir la transacción: " + transaction.transactionId(), ex);
        }
    }
}
