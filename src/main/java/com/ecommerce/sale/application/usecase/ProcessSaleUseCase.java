package com.ecommerce.sale.application.usecase;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.application.port.out.ObservabilityPort;
import com.ecommerce.sale.domain.exception.AuthorizationSwitchException;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Caso de uso principal: orquesta el procesamiento de una transacción SALE.
 *
 * Flujo (sin detección de duplicados — se añade en T045):
 * 1. Generar transactionId.
 * 2. Construir SaleTransaction en estado PENDING.
 * 3. Persistir en estado PENDING.
 * 4. Delegar autorización al API Switch.
 * 5. Actualizar la transacción con el resultado de autorización.
 * 6. Persistir estado final.
 * 7. Retornar transacción actualizada.
 *
 * En caso de error en el API Switch: marcar transacción con ERROR y persistir.
 */
public class ProcessSaleUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessSaleUseCase.class);

    private final ValidateSaleRequestUseCase validateSaleRequestUseCase;
    private final DuplicateDetectionService duplicateDetectionService;
    private final GenerateTransactionIdUseCase generateTransactionIdUseCase;
    private final AuthorizeTransactionUseCase authorizeTransactionUseCase;
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ObservabilityPort observabilityPort;

    public ProcessSaleUseCase(
        ValidateSaleRequestUseCase validateSaleRequestUseCase,
        DuplicateDetectionService duplicateDetectionService,
        GenerateTransactionIdUseCase generateTransactionIdUseCase,
        AuthorizeTransactionUseCase authorizeTransactionUseCase,
        PersistTransactionUseCase persistTransactionUseCase,
        ObservabilityPort observabilityPort
    ) {
        this.validateSaleRequestUseCase = validateSaleRequestUseCase;
        this.duplicateDetectionService = duplicateDetectionService;
        this.generateTransactionIdUseCase = generateTransactionIdUseCase;
        this.authorizeTransactionUseCase = authorizeTransactionUseCase;
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.observabilityPort = observabilityPort;
    }

    public SaleTransaction execute(ProcessSaleCommand command) {
        long startedAt = System.currentTimeMillis();
        String correlationId = resolveCorrelationId(command.correlationId());
        LOG.info("event=sale.process.started correlationId={} terminalId={} invoice={} amount={}",
            correlationId, command.terminalId(), command.invoice(), command.totalAmount());
        observabilityPort.event("sale.process.started", Map.of(
            "correlationId", correlationId,
            "terminalId", command.terminalId(),
            "transactionType", command.transactionType()
        ));

        validateSaleRequestUseCase.execute(command);
        duplicateDetectionService.execute(command);

        String transactionId = generateTransactionIdUseCase.execute();
        MDC.put("transactionId", transactionId);
        Instant now = Instant.now();

        SaleTransaction pending = new SaleTransaction(
            transactionId,
            correlationId,
            command.terminalId(),
            TransactionType.valueOf(command.transactionType()),
            command.totalAmount(),
            command.accountNumber(),
            command.expirationDate(),
            command.invoice(),
            command.securityValidationResponse(),
            command.binValidate(),
            TransactionStatus.PENDING,
            null,
            now,
            null,
            now
        );

        LOG.info("event=sale.process.beforePersistPending correlationId={} transactionId={} status={}",
            correlationId, pending.transactionId(), pending.status());
        persistTransactionUseCase.execute(pending);

        try {
            LOG.info("event=sale.process.beforeSwitch correlationId={} transactionId={}",
                correlationId, pending.transactionId());
            SaleTransaction authorized = authorizeTransactionUseCase.execute(pending);
            LOG.info("event=sale.process.beforePersistFinal correlationId={} transactionId={} status={}",
                correlationId, authorized.transactionId(), authorized.status());
            persistTransactionUseCase.execute(authorized);
            long duration = System.currentTimeMillis() - startedAt;
            LOG.info("event=sale.process.completed correlationId={} transactionId={} status={} durationMs={}",
                correlationId, authorized.transactionId(), authorized.status(), duration);
            observabilityPort.event("sale.process.completed", Map.of(
                "correlationId", correlationId,
                "transactionId", authorized.transactionId(),
                "status", authorized.status().name()
            ));
            observabilityPort.increment("sale.process.total", Map.of("status", authorized.status().name()));
            observabilityPort.timing("sale.process.latency", duration, Map.of("status", authorized.status().name()));
            return authorized;

        } catch (AuthorizationSwitchException ex) {
            SaleTransaction failed = pending.withError(Instant.now());
            persistTransactionUseCase.execute(failed);
            long duration = System.currentTimeMillis() - startedAt;
            LOG.error("event=sale.process.failed correlationId={} transactionId={} durationMs={} error={}",
                correlationId, failed.transactionId(), duration, ex.getMessage());
            observabilityPort.event("sale.process.failed", Map.of(
                "correlationId", correlationId,
                "transactionId", failed.transactionId(),
                "error", ex.getClass().getSimpleName()
            ));
            observabilityPort.increment("sale.process.total", Map.of("status", "ERROR"));
            observabilityPort.timing("sale.process.latency", duration, Map.of("status", "ERROR"));
            throw ex;
        } finally {
            MDC.remove("transactionId");
        }
    }

    private String resolveCorrelationId(String commandCorrelationId) {
        if (commandCorrelationId != null && !commandCorrelationId.isBlank()) {
            return commandCorrelationId;
        }
        String mdcCorrelationId = MDC.get("correlationId");
        if (mdcCorrelationId != null && !mdcCorrelationId.isBlank()) {
            return mdcCorrelationId;
        }
        return UUID.randomUUID().toString();
    }
}
