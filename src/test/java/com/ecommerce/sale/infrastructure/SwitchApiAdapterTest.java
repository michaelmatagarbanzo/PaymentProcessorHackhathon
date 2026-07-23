package com.ecommerce.sale.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import com.ecommerce.sale.infrastructure.adapter.observability.ApplicationInsightsAdapter;
import com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchApiAdapter;
import com.ecommerce.sale.infrastructure.adapter.switch_api.SwitchRequestMapper;
import com.ecommerce.sale.infrastructure.config.SwitchProperties;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SwitchApiAdapterTest {

    @Mock
    private ApplicationInsightsAdapter applicationInsightsAdapter;

    @Test
    void shouldMapSwitchTransportFailureToExternalDependencyException() {
        SwitchProperties properties = new SwitchProperties();
        properties.setBaseUrl("http://127.0.0.1:1");

        SwitchApiAdapter adapter = new SwitchApiAdapter(
            properties,
            new SwitchRequestMapper(),
            applicationInsightsAdapter
        );

        ExternalDependencyUnavailableException ex = assertThrows(
            ExternalDependencyUnavailableException.class,
            () -> adapter.authorize(pendingTransaction(), "token-123")
        );

        assertEquals("switch", ex.getDependency());
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
