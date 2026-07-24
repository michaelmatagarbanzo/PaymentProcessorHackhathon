package com.ecommerce.sale.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import com.ecommerce.sale.presentation.dto.SaleResponse;
import com.ecommerce.sale.presentation.mapper.SaleMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SaleMapperTest {

    private final SaleMapper mapper = new SaleMapper();

    @Test
    void shouldMapSaleTransactionToSaleResponseInExpectedFieldOrder() {
        Instant now = Instant.now();
        SaleTransaction transaction = new SaleTransaction(
            "txn-123",
            "corr-123",
            "TERM-0001",
            TransactionType.SALE,
            5633L,
            "55189800****2751",
            "2805",
            14611279L,
            "1",
            true,
            TransactionStatus.AUTHORIZED,
            null,
            now,
            now,
            now
        );

        SaleResponse response = mapper.toResponse(transaction);

        assertEquals("txn-123", response.transactionId());
        assertEquals("corr-123", response.correlationId());
        assertEquals("AUTHORIZED", response.status());
        assertEquals("TERM-0001", response.terminalId());
        assertEquals(new BigDecimal("56.33"), response.totalAmount());
        assertEquals(now, response.processingDateTime());
        assertEquals(now, response.createdAt());
    }
}
