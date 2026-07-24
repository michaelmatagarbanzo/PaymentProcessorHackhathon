package com.ecommerce.sale.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.domain.exception.InvalidSaleRequestException;
import org.junit.jupiter.api.Test;

class ValidateSaleRequestUseCaseTest {

    private final ValidateSaleRequestUseCase useCase = new ValidateSaleRequestUseCase();

    @Test
    void shouldAcceptValidSaleRequest() {
        assertDoesNotThrow(() -> useCase.execute(validCommand()));
    }

    @Test
    void shouldRejectInvalidTransactionType() {
        ProcessSaleCommand invalid = new ProcessSaleCommand(
            "corr-1",
            "TERM-0001",
            "REFUND",
            5633L,
            "USD",
            "55189800****2751",
            "Test User",
            "2805",
            14611279L,
            "123",
            "1",
            true,
            null,
            null,
            null
        );

        assertThrows(InvalidSaleRequestException.class, () -> useCase.execute(invalid));
    }

    @Test
    void shouldRejectInvalidExpirationDateFormat() {
        ProcessSaleCommand invalid = new ProcessSaleCommand(
            "corr-1",
            "TERM-0001",
            "SALE",
            5633L,
            "USD",
            "55189800****2751",
            "Test User",
            "2028",
            14611279L,
            "123",
            "1",
            true,
            null,
            null,
            null
        );

        assertThrows(InvalidSaleRequestException.class, () -> useCase.execute(invalid));
    }

    @Test
    void shouldRejectBlankSecurityValidationResponse() {
        ProcessSaleCommand invalid = new ProcessSaleCommand(
            "corr-1",
            "TERM-0001",
            "SALE",
            5633L,
            "USD",
            "55189800****2751",
            "Test User",
            "2805",
            14611279L,
            "123",
            " ",
            true,
            null,
            null,
            null
        );

        assertThrows(InvalidSaleRequestException.class, () -> useCase.execute(invalid));
    }

    private ProcessSaleCommand validCommand() {
        return new ProcessSaleCommand(
            "corr-1",
            "TERM-0001",
            "SALE",
            5633L,
            "USD",
            "55189800****2751",
            "Test User",
            "2805",
            14611279L,
            "123",
            "1",
            true,
            null,
            null,
            null
        );
    }
}
