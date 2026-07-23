package com.ecommerce.sale.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root que representa el ciclo completo de una transacción SALE.
 *
 * Invariantes:
 * - transactionId: generado por el sistema, nunca null ni vacío.
 * - transactionType: siempre SALE.
 * - totalAmount: > 0, en unidad monetaria mínima (centavos). Ej: 56.33 USD = 5633.
 * - accountNumber: tokenizado/enmascarado; nunca PAN en claro.
 * - invoice: obligatorio.
 * - Estados terminales (AUTHORIZED, DECLINED, ERROR) no pueden mutar.
 *
 * Detección de duplicados (clave de negocio):
 *   terminalId + invoice + totalAmount + accountNumber + transactionType
 *
 * transactionId NO participa en la detección de duplicados.
 */
public record SaleTransaction(
    String transactionId,
    String correlationId,
    String terminalId,
    TransactionType transactionType,
    Long totalAmount,
    String accountNumber,
    String expirationDate,
    Long invoice,
    String securityValidationResponse,
    Boolean binValidate,
    TransactionStatus status,
    AuthorizationResponse authorizationResult,
    Instant createdAt,
    Instant processingDateTime,
    Instant updatedAt
) {

    public SaleTransaction {
        Objects.requireNonNull(transactionId, "transactionId no puede ser null");
        Objects.requireNonNull(transactionType, "transactionType no puede ser null");
        Objects.requireNonNull(totalAmount, "totalAmount no puede ser null");
        Objects.requireNonNull(accountNumber, "accountNumber no puede ser null");
        Objects.requireNonNull(invoice, "invoice no puede ser null");
        Objects.requireNonNull(status, "status no puede ser null");

        if (transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId no puede estar vacío");
        }
        if (transactionType != TransactionType.SALE) {
            throw new IllegalArgumentException("transactionType debe ser SALE");
        }
        if (totalAmount <= 0) {
            throw new IllegalArgumentException("totalAmount debe ser mayor que 0");
        }
    }

    public boolean isTerminalStatus() {
        return status == TransactionStatus.AUTHORIZED
            || status == TransactionStatus.DECLINED
            || status == TransactionStatus.ERROR;
    }

    public SaleTransaction withAuthorizationResult(AuthorizationResponse result, Instant processedAt) {
        if (isTerminalStatus()) {
            throw new IllegalStateException(
                "No se puede actualizar una transacción en estado terminal: " + status);
        }
        return new SaleTransaction(
            transactionId, correlationId, terminalId, transactionType,
            totalAmount, accountNumber, expirationDate, invoice, securityValidationResponse,
            binValidate, result.toTransactionStatus(), result, createdAt, processedAt, processedAt
        );
    }

    public SaleTransaction withError(Instant processedAt) {
        if (isTerminalStatus()) {
            throw new IllegalStateException(
                "No se puede actualizar una transacción en estado terminal: " + status);
        }
        return new SaleTransaction(
            transactionId, correlationId, terminalId, transactionType,
            totalAmount, accountNumber, expirationDate, invoice, securityValidationResponse,
            binValidate, TransactionStatus.ERROR, authorizationResult, createdAt, processedAt, processedAt
        );
    }
}
