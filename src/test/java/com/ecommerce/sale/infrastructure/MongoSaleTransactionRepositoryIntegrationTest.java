package com.ecommerce.sale.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import com.ecommerce.sale.infrastructure.persistence.document.SaleTransactionDocument;
import com.ecommerce.sale.infrastructure.persistence.repository.MongoSaleTransactionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
class MongoSaleTransactionRepositoryIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @Autowired
    private MongoSaleTransactionRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void shouldFindTransactionByTransactionId() {
        SaleTransactionDocument document = buildDocument(
            "txn-001",
            "TERM-0001",
            14611279L,
            5633L,
            "55189800****2751"
        );
        repository.save(document);

        Optional<SaleTransactionDocument> found = repository.findByTransactionId("txn-001");

        assertTrue(found.isPresent());
        assertEquals("txn-001", found.get().getTransactionId());
    }

    @Test
    void shouldFindDuplicateByBusinessKey() {
        SaleTransactionDocument document = buildDocument(
            "txn-002",
            "TERM-0001",
            14611279L,
            5633L,
            "55189800****2751"
        );
        repository.save(document);

        Optional<SaleTransactionDocument> duplicate =
            repository.findByTerminalIdAndInvoiceAndTotalAmountAndAccountNumberAndTransactionType(
                "TERM-0001",
                14611279L,
                5633L,
                "55189800****2751",
                TransactionType.SALE
            );

        assertTrue(duplicate.isPresent());
        assertEquals("txn-002", duplicate.get().getTransactionId());
    }

    private SaleTransactionDocument buildDocument(
        String transactionId,
        String terminalId,
        Long invoice,
        Long totalAmount,
        String accountNumber
    ) {
        Instant now = Instant.now();
        SaleTransactionDocument document = new SaleTransactionDocument();
        document.setTransactionId(transactionId);
        document.setCorrelationId("corr-001");
        document.setTerminalId(terminalId);
        document.setTransactionType(TransactionType.SALE);
        document.setTotalAmount(totalAmount);
        document.setAccountNumber(accountNumber);
        document.setExpirationDate("2805");
        document.setInvoice(invoice);
        document.setSecurityValidationResponse("1");
        document.setBinValidate(true);
        document.setStatus(TransactionStatus.PENDING);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }
}
