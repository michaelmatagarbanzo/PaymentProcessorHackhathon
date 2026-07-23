package com.ecommerce.sale.application.usecase;

import java.util.UUID;

/**
 * Caso de uso: genera un transactionId (UUID v4) para cada nueva transacción.
 * El commercio NO envía transactionId; el sistema lo produce aquí.
 * UUID.randomUUID() usa SecureRandom internamente → criptográficamente seguro.
 */
public class GenerateTransactionIdUseCase {

    public String execute() {
        return UUID.randomUUID().toString();
    }
}
