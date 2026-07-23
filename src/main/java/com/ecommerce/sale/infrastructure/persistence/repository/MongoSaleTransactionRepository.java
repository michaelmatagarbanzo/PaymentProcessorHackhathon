package com.ecommerce.sale.infrastructure.persistence.repository;

import com.ecommerce.sale.domain.model.TransactionType;
import com.ecommerce.sale.infrastructure.persistence.document.SaleTransactionDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoSaleTransactionRepository extends MongoRepository<SaleTransactionDocument, String> {

    Optional<SaleTransactionDocument> findByTransactionId(String transactionId);

    Optional<SaleTransactionDocument> findByTerminalIdAndInvoiceAndTotalAmountAndAccountNumberAndTransactionType(
        String terminalId,
        Long invoice,
        Long totalAmount,
        String accountNumber,
        TransactionType transactionType
    );
}