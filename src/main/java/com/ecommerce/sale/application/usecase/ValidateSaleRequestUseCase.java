package com.ecommerce.sale.application.usecase;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.domain.exception.InvalidSaleRequestException;

/**
 * T043 - Validación integral del payload de entrada para SALE.
 */
public class ValidateSaleRequestUseCase {

    public void execute(ProcessSaleCommand command) {
        validateRequired(command.terminalId(), "terminalId es obligatorio");
        validateRequired(command.transactionType(), "transactionType es obligatorio");
        validateRequired(command.accountNumber(), "accountNumber es obligatorio");
        validateRequired(command.expirationDate(), "expirationDate es obligatorio");
        validateRequired(command.securityCodeEntry(), "securityCodeEntry es obligatorio");
        validateRequired(command.securityValidationResponse(), "securityValidationResponse es obligatorio");

        if (!"SALE".equalsIgnoreCase(command.transactionType())) {
            throw new InvalidSaleRequestException("transactionType debe ser SALE");
        }
        if (command.totalAmount() == null || command.totalAmount() <= 0) {
            throw new InvalidSaleRequestException("totalAmount debe ser mayor que 0");
        }
        if (command.invoice() == null || command.invoice() <= 0) {
            throw new InvalidSaleRequestException("invoice debe ser mayor que 0");
        }
        if (!command.expirationDate().matches("^(\\d{2})(0[1-9]|1[0-2])$")) {
            throw new InvalidSaleRequestException("expirationDate debe cumplir formato YYMM");
        }
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidSaleRequestException(message);
        }
    }
}