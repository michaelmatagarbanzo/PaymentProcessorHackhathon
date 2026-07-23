package com.ecommerce.sale.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import com.ecommerce.sale.infrastructure.adapter.observability.ApplicationInsightsAdapter;
import com.ecommerce.sale.infrastructure.adapter.persistence.MongoTransactionAdapter;
import com.ecommerce.sale.infrastructure.adapter.persistence.SaleTransactionMapper;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import com.ecommerce.sale.infrastructure.persistence.document.SaleTransactionDocument;
import com.ecommerce.sale.infrastructure.persistence.repository.MongoSaleTransactionRepository;
import com.mongodb.MongoTimeoutException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class MongoTransactionAdapterTest {

    @Mock
    private MongoSaleTransactionRepository repository;

    @Mock
    private SaleTransactionMapper mapper;

    @Mock
    private ApplicationInsightsAdapter applicationInsightsAdapter;

    @Test
    void shouldMapMongoUnavailableToExternalDependencyException() {
        MongoTransactionAdapter adapter = new MongoTransactionAdapter(
            repository,
            mapper,
            applicationInsightsAdapter,
            "mongodb://localhost:27017/sale_db"
        );
        SaleTransaction transaction = pendingTransaction();
        SaleTransactionDocument document = new SaleTransactionDocument();

        when(mapper.toDocument(transaction)).thenReturn(document);
        when(repository.findByTransactionId(transaction.transactionId())).thenReturn(Optional.empty());
        when(repository.save(document)).thenThrow(new DataAccessResourceFailureException("mongo down"));

        ExternalDependencyUnavailableException ex = assertThrows(
            ExternalDependencyUnavailableException.class,
            () -> adapter.save(transaction)
        );

        assertEquals("mongodb", ex.getDependency());
    }

    @Test
    void shouldMapCosmosTimeoutToExternalDependencyException() {
        MongoTransactionAdapter adapter = new MongoTransactionAdapter(
            repository,
            mapper,
            applicationInsightsAdapter,
            "mongodb://user:pass@account.mongo.cosmos.azure.com:10255/?ssl=true"
        );

        when(mapper.maskAccountNumber("55189800****2751")).thenReturn("55189800****2751");
        when(repository.findByTerminalIdAndInvoiceAndTotalAmountAndAccountNumberAndTransactionType(
            "TERM-0001",
            14611279L,
            5633L,
            "55189800****2751",
            TransactionType.SALE
        )).thenThrow(new MongoTimeoutException("timeout"));

        ExternalDependencyUnavailableException ex = assertThrows(
            ExternalDependencyUnavailableException.class,
            () -> adapter.findDuplicate("TERM-0001", 14611279L, 5633L, "55189800****2751", "SALE")
        );

        assertEquals("cosmos", ex.getDependency());
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
