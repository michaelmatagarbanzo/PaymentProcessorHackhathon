package com.ecommerce.sale.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.application.port.out.ObservabilityPort;
import com.ecommerce.sale.application.usecase.AuthorizeTransactionUseCase;
import com.ecommerce.sale.application.usecase.DuplicateDetectionService;
import com.ecommerce.sale.application.usecase.GenerateTransactionIdUseCase;
import com.ecommerce.sale.application.usecase.PersistTransactionUseCase;
import com.ecommerce.sale.application.usecase.ProcessSaleUseCase;
import com.ecommerce.sale.application.usecase.ValidateSaleRequestUseCase;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.AuthorizationSource;
import com.ecommerce.sale.domain.model.SaleTransaction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProcessSaleUseCasePerformanceTest {

    @Test
    void shouldMeetBasicLatencyAndErrorRateTargets() {
        ValidateSaleRequestUseCase validateSaleRequestUseCase = Mockito.mock(ValidateSaleRequestUseCase.class);
        DuplicateDetectionService duplicateDetectionService = Mockito.mock(DuplicateDetectionService.class);
        GenerateTransactionIdUseCase generateTransactionIdUseCase = Mockito.mock(GenerateTransactionIdUseCase.class);
        AuthorizeTransactionUseCase authorizeTransactionUseCase = Mockito.mock(AuthorizeTransactionUseCase.class);
        PersistTransactionUseCase persistTransactionUseCase = Mockito.mock(PersistTransactionUseCase.class);

        doNothing().when(validateSaleRequestUseCase).execute(any());
        doNothing().when(duplicateDetectionService).execute(any());
        doNothing().when(persistTransactionUseCase).execute(any());
        when(generateTransactionIdUseCase.execute()).thenAnswer(inv -> UUID.randomUUID().toString());
        when(authorizeTransactionUseCase.execute(any())).thenAnswer(invocation -> {
            SaleTransaction pending = invocation.getArgument(0);
            AuthorizationResponse response = new AuthorizationResponse(
                AuthorizationSource.AS400,
                "AUTH001",
                "00",
                "Aprobado",
                "REF001",
                "0722",
                "143052"
            );
            return pending.withAuthorizationResult(response, Instant.now());
        });

        ProcessSaleUseCase useCase = new ProcessSaleUseCase(
            validateSaleRequestUseCase,
            duplicateDetectionService,
            generateTransactionIdUseCase,
            authorizeTransactionUseCase,
            persistTransactionUseCase,
            new NoOpObservabilityPort()
        );

        ProcessSaleCommand command = new ProcessSaleCommand(
            "550e8400-e29b-41d4-a716-446655440000",
            "TERM-0001",
            "SALE",
            5633L,
            "55189800****2751",
            "2805",
            14611279L,
            "123",
            "1",
            true,
            null,
            null,
            null
        );

        int iterations = 120;
        int failures = 0;
        List<Long> latenciesMs = new ArrayList<>(iterations);

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            try {
                useCase.execute(command);
            } catch (RuntimeException ex) {
                failures++;
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            latenciesMs.add(elapsedMs);
        }

        latenciesMs.sort(Comparator.naturalOrder());

        long p95 = percentile(latenciesMs, 95);
        long p99 = percentile(latenciesMs, 99);
        double errorRate = (double) failures / iterations;

        assertTrue(p95 < 3000, "P95 debe ser menor a 3000ms. Actual=" + p95);
        assertTrue(p99 < 5000, "P99 debe ser menor a 5000ms. Actual=" + p99);
        assertTrue(errorRate < 0.01, "Error rate debe ser menor a 1%. Actual=" + errorRate);
    }

    private long percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        int safeIndex = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(safeIndex);
    }

    private static class NoOpObservabilityPort implements ObservabilityPort {
        @Override
        public void event(String eventName, Map<String, String> attributes) {
            // no-op for performance test
        }

        @Override
        public void increment(String metricName, Map<String, String> tags) {
            // no-op for performance test
        }

        @Override
        public void timing(String metricName, long durationMs, Map<String, String> tags) {
            // no-op for performance test
        }
    }
}
