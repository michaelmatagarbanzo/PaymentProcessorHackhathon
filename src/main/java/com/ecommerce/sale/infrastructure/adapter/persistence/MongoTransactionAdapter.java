package com.ecommerce.sale.infrastructure.adapter.persistence;

import com.ecommerce.sale.application.port.out.TransactionRepositoryPort;
import com.ecommerce.sale.domain.exception.TransactionPersistenceException;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import com.ecommerce.sale.infrastructure.adapter.observability.ApplicationInsightsAdapter;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import com.ecommerce.sale.infrastructure.persistence.document.SaleTransactionDocument;
import com.ecommerce.sale.infrastructure.persistence.repository.MongoSaleTransactionRepository;
import com.mongodb.MongoException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

@Component
public class MongoTransactionAdapter implements TransactionRepositoryPort {

    private static final Logger LOG = LoggerFactory.getLogger(MongoTransactionAdapter.class);

    private final MongoSaleTransactionRepository repository;
    private final SaleTransactionMapper mapper;
    private final ApplicationInsightsAdapter applicationInsightsAdapter;
    private final String dependencyName;

    public MongoTransactionAdapter(MongoSaleTransactionRepository repository,
                                   SaleTransactionMapper mapper,
                                   ApplicationInsightsAdapter applicationInsightsAdapter,
                                   @Value("${spring.data.mongodb.uri:}") String mongoUri) {
        this.repository = repository;
        this.mapper = mapper;
        this.applicationInsightsAdapter = applicationInsightsAdapter;
        this.dependencyName = resolveDependencyName(mongoUri);
    }

    @Override
    @CircuitBreaker(name = "mongoDb")
    @Retry(name = "mongoDb")
    @Bulkhead(name = "mongoDb")
    @TimeLimiter(name = "mongoDb")
    @RateLimiter(name = "mongoDb")
    public void save(SaleTransaction transaction) {
        long startedAt = System.currentTimeMillis();
        try {
            SaleTransactionDocument document = mapper.toDocument(transaction);
            repository.findByTransactionId(transaction.transactionId())
                .ifPresent(existing -> document.setId(existing.getId()));
            repository.save(document);
            long duration = System.currentTimeMillis() - startedAt;
            LOG.info("event=mongo.save.completed correlationId={} transactionId={} status={} durationMs={}",
                transaction.correlationId(), transaction.transactionId(), transaction.status(), duration);
            applicationInsightsAdapter.increment("mongo.transaction.save.total",
                Map.of("status", transaction.status().name()));
            applicationInsightsAdapter.timing("mongo.transaction.save.latency", duration,
                Map.of("status", transaction.status().name()));
            applicationInsightsAdapter.event("mongo.save.completed", Map.of(
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "status", transaction.status().name()
            ));
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startedAt;
            LOG.error("event=mongo.save.failed dependency={} correlationId={} transactionId={} durationMs={} error={}",
                dependencyName, transaction.correlationId(), transaction.transactionId(), duration, ex.getMessage());
            LOG.error("event=sale.error correlationId={} transactionId={} errorType={} errorCode={} errorMessage={}",
                safe(transaction.correlationId()),
                safe(transaction.transactionId()),
                ex.getClass().getSimpleName(),
                "DB_SAVE_FAILED",
                ex.getMessage(),
                ex);
            applicationInsightsAdapter.increment("mongo.transaction.save.total", Map.of("status", "ERROR"));
            applicationInsightsAdapter.timing("mongo.transaction.save.latency", duration,
                Map.of("status", "ERROR"));
            applicationInsightsAdapter.event("mongo.save.failed", Map.of(
                "dependency", dependencyName,
                "correlationId", safe(transaction.correlationId()),
                "transactionId", safe(transaction.transactionId()),
                "error", ex.getClass().getSimpleName()
            ));
            if (isExternalDependencyFailure(ex)) {
                throw new ExternalDependencyUnavailableException(dependencyName, ex);
            }
            throw new TransactionPersistenceException(
                "Error al guardar la transacción: " + transaction.transactionId(), ex);
        }
    }

    @Override
    @CircuitBreaker(name = "mongoDb")
    @Retry(name = "mongoDb")
    @Bulkhead(name = "mongoDb")
    @TimeLimiter(name = "mongoDb")
    @RateLimiter(name = "mongoDb")
    public Optional<SaleTransaction> findByTransactionId(String transactionId) {
        try {
            LOG.debug("event=mongo.findByTransactionId dependency={} transactionId={}", dependencyName, transactionId);
            return repository.findByTransactionId(transactionId).map(mapper::toDomain);
        } catch (RuntimeException ex) {
            LOG.error("event=mongo.findByTransactionId.failed dependency={} correlationId={} transactionId={} error={}",
                dependencyName, "unknown", transactionId, ex.getMessage());
            LOG.error("event=sale.error correlationId={} transactionId={} errorType={} errorCode={} errorMessage={}",
                "unknown",
                safe(transactionId),
                ex.getClass().getSimpleName(),
                "DB_FIND_BY_TRANSACTION_ID_FAILED",
                ex.getMessage(),
                ex);
            if (isExternalDependencyFailure(ex)) {
                throw new ExternalDependencyUnavailableException(dependencyName, ex);
            }
            throw new TransactionPersistenceException(
                "Error al consultar la transacción: " + transactionId, ex);
        }
    }

    @Override
    @CircuitBreaker(name = "mongoDb")
    @Retry(name = "mongoDb")
    @Bulkhead(name = "mongoDb")
    @TimeLimiter(name = "mongoDb")
    @RateLimiter(name = "mongoDb")
    public Optional<SaleTransaction> findDuplicate(String terminalId,
                                                   Long invoice,
                                                   Long totalAmount,
                                                   String accountNumber,
                                                   String transactionType) {
        long startedAt = System.currentTimeMillis();
        try {
            String maskedAccountNumber = mapper.maskAccountNumber(accountNumber);
            TransactionType domainTransactionType = TransactionType.valueOf(transactionType.toUpperCase());
            Optional<SaleTransaction> result = repository.findByTerminalIdAndInvoiceAndTotalAmountAndAccountNumberAndTransactionType(
                terminalId,
                invoice,
                totalAmount,
                maskedAccountNumber,
                domainTransactionType
            ).map(mapper::toDomain);
            long duration = System.currentTimeMillis() - startedAt;
            String duplicateFound = result.isPresent() ? "true" : "false";
            LOG.info("event=mongo.findDuplicate.completed dependency={} terminalId={} invoice={} transactionType={} duplicateFound={} durationMs={}",
                dependencyName, terminalId, invoice, transactionType, duplicateFound, duration);
            applicationInsightsAdapter.increment("mongo.transaction.duplicate.lookup.total",
                Map.of("duplicateFound", duplicateFound));
            applicationInsightsAdapter.timing("mongo.transaction.duplicate.lookup.latency", duration,
                Map.of("duplicateFound", duplicateFound));
            return result;
        } catch (RuntimeException ex) {
            LOG.error("event=mongo.findDuplicate.failed dependency={} correlationId={} terminalId={} invoice={} error={}",
                dependencyName, "unknown", terminalId, invoice, ex.getMessage());
            LOG.error("event=sale.error correlationId={} transactionId={} errorType={} errorCode={} errorMessage={}",
                "unknown",
                "unknown",
                ex.getClass().getSimpleName(),
                "DB_DUPLICATE_LOOKUP_FAILED",
                ex.getMessage(),
                ex);
            if (isExternalDependencyFailure(ex)) {
                throw new ExternalDependencyUnavailableException(dependencyName, ex);
            }
            throw new TransactionPersistenceException("Error al consultar duplicados", ex);
        }
    }

    @Override
    @CircuitBreaker(name = "mongoDb")
    @Retry(name = "mongoDb")
    @Bulkhead(name = "mongoDb")
    @TimeLimiter(name = "mongoDb")
    @RateLimiter(name = "mongoDb")
    public void updateStatus(String transactionId,
                             TransactionStatus status,
                             AuthorizationResponse authorizationResponse) {
        long startedAt = System.currentTimeMillis();
        try {
            SaleTransactionDocument document = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionPersistenceException(
                    "No existe transacción para actualizar estado: " + transactionId));
            document.setStatus(status);
            if (authorizationResponse != null) {
                SaleTransactionDocument.AuthorizationResponseDocument authDocument =
                    new SaleTransactionDocument.AuthorizationResponseDocument();
                authDocument.setAuthorizationSource(authorizationResponse.authorizationSource());
                authDocument.setAuthorizationNumber(authorizationResponse.authorizationNumber());
                authDocument.setResponseCode(authorizationResponse.responseCode());
                authDocument.setResponseDescription(authorizationResponse.responseDescription());
                authDocument.setReferenceNumber(authorizationResponse.referenceNumber());
                authDocument.setHostDate(authorizationResponse.hostDate());
                authDocument.setHostTime(authorizationResponse.hostTime());
                document.setAuthorizationResult(authDocument);
            }
            repository.save(document);
            long duration = System.currentTimeMillis() - startedAt;
            applicationInsightsAdapter.increment("mongo.transaction.update.total", Map.of("status", status.name()));
            applicationInsightsAdapter.timing("mongo.transaction.update.latency", duration,
                Map.of("status", status.name()));
            LOG.info("event=mongo.updateStatus.completed transactionId={} status={} durationMs={}",
                transactionId, status, duration);
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startedAt;
            applicationInsightsAdapter.increment("mongo.transaction.update.total", Map.of("status", "ERROR"));
            applicationInsightsAdapter.timing("mongo.transaction.update.latency", duration,
                Map.of("status", "ERROR"));
            LOG.error("event=mongo.updateStatus.failed dependency={} correlationId={} transactionId={} durationMs={} error={}",
                dependencyName, "unknown", transactionId, duration, ex.getMessage());
            LOG.error("event=sale.error correlationId={} transactionId={} errorType={} errorCode={} errorMessage={}",
                "unknown",
                safe(transactionId),
                ex.getClass().getSimpleName(),
                "DB_UPDATE_STATUS_FAILED",
                ex.getMessage(),
                ex);
            if (ex instanceof TransactionPersistenceException) {
                throw ex;
            }
            if (isExternalDependencyFailure(ex)) {
                throw new ExternalDependencyUnavailableException(dependencyName, ex);
            }
            throw new TransactionPersistenceException(
                "Error al actualizar el estado de la transacción: " + transactionId, ex);
        }
    }

    private String resolveDependencyName(String mongoUri) {
        if (mongoUri != null && mongoUri.toLowerCase().contains("cosmos.azure.com")) {
            return "cosmos";
        }
        return "mongodb";
    }

    private boolean isExternalDependencyFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ExternalDependencyUnavailableException
                || current instanceof MongoTimeoutException
                || current instanceof MongoSocketOpenException
                || current instanceof MongoException
                || current instanceof DataAccessResourceFailureException
                || current instanceof DataAccessException
                || current instanceof CallNotPermittedException
                || current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "unknown" : value;
    }
}