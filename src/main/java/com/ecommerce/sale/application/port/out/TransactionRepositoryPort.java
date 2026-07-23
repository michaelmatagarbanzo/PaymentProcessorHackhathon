package com.ecommerce.sale.application.port.out;

import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import java.util.Optional;

/**
 * Puerto de salida: abstracción de persistencia de transacciones.
 * La implementación reside en infrastructure (MongoTransactionAdapter).
 */
public interface TransactionRepositoryPort {

    void save(SaleTransaction transaction);

    Optional<SaleTransaction> findByTransactionId(String transactionId);

    Optional<SaleTransaction> findDuplicate(
        String terminalId,
        Long invoice,
        Long totalAmount,
        String accountNumber,
        String transactionType
    );

    void updateStatus(
        String transactionId,
        TransactionStatus status,
        AuthorizationResponse authorizationResponse
    );
}
