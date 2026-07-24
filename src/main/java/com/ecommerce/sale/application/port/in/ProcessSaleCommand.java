package com.ecommerce.sale.application.port.in;

import java.util.Objects;

/**
 * Comando de entrada para procesar una transacción SALE.
 *
 * Contrato:
 * - correlationId puede ser null (la API lo genera si no llega en el header).
 * - securityCodeEntry se usa sólo durante el procesamiento; nunca se persiste.
 * - totalAmount en unidad monetaria mínima (centavos). Ej: 56.33 USD = 5633.
 */
public record ProcessSaleCommand(
    String correlationId,
    String terminalId,
    String transactionType,
    Long totalAmount,
    String currency,
    String accountNumber,
    String cardHolderName,
    String expirationDate,
    Long invoice,
    String securityCodeEntry,
    String securityValidationResponse,
    Boolean binValidate,
    AuthenticationInformationCommand authenticationInformation,
    TokenizationInformationCommand tokenizationInformation,
    ProcessingInformationCommand processingInformation
) {

    public ProcessSaleCommand {
        Objects.requireNonNull(terminalId, "terminalId no puede ser null");
        Objects.requireNonNull(transactionType, "transactionType no puede ser null");
        Objects.requireNonNull(totalAmount, "totalAmount no puede ser null");
        Objects.requireNonNull(accountNumber, "accountNumber no puede ser null");
        Objects.requireNonNull(invoice, "invoice no puede ser null");

        if (totalAmount <= 0) {
            throw new IllegalArgumentException("totalAmount debe ser mayor que 0");
        }
    }
}
