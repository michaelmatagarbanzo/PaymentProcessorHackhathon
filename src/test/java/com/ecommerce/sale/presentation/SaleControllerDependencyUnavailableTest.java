package com.ecommerce.sale.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.application.usecase.ProcessSaleUseCase;
import com.ecommerce.sale.domain.exception.AuthorizationSwitchException;
import com.ecommerce.sale.domain.exception.TransactionPersistenceException;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import com.ecommerce.sale.presentation.controller.SaleController;
import com.ecommerce.sale.presentation.exception.GlobalExceptionHandler;
import com.ecommerce.sale.presentation.mapper.SaleMapper;
import com.mongodb.MongoTimeoutException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SaleControllerDependencyUnavailableTest {

    @Mock
    private ProcessSaleUseCase processSaleUseCase;

    @Mock
    private SaleMapper saleMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SaleController(processSaleUseCase, saleMapper))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        when(saleMapper.toCommand(any(), anyString())).thenReturn(validCommand());
    }

    @Test
    void shouldReturn503WhenMongoIsUnavailable() throws Exception {
        when(processSaleUseCase.execute(any())).thenThrow(
            new TransactionPersistenceException(
                "mongo unavailable",
                new ExternalDependencyUnavailableException(
                    "mongodb",
                    new DataAccessResourceFailureException("mongo down")
                )
            )
        );

        performRequestAndAssertServiceUnavailable(
            "/errors/database-unavailable",
            "No fue posible acceder a la base de datos"
        );
    }

    @Test
    void shouldReturn503WhenCosmosTimesOut() throws Exception {
        when(processSaleUseCase.execute(any())).thenThrow(
            new TransactionPersistenceException(
                "cosmos timeout",
                new ExternalDependencyUnavailableException(
                    "cosmos",
                    new MongoTimeoutException("timeout")
                )
            )
        );

        performRequestAndAssertServiceUnavailable(
            "/errors/database-unavailable",
            "No fue posible acceder a la base de datos"
        );
    }

    @Test
    void shouldReturn503WhenSwitchTimesOut() throws Exception {
        when(processSaleUseCase.execute(any())).thenThrow(
            new AuthorizationSwitchException(
                "switch timeout",
                new ExternalDependencyUnavailableException("switch", new TimeoutException("timeout"))
            )
        );

        performRequestAndAssertServiceUnavailable(
            "/errors/switch-unavailable",
            "No fue posible comunicarse con el switch de autorización"
        );
    }

    @Test
    void shouldReturn503WhenCircuitBreakerIsOpen() throws Exception {
        CallNotPermittedException exception =
            CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("switchApi"));
        when(processSaleUseCase.execute(any())).thenThrow(exception);

        performRequestAndAssertServiceUnavailable(
            "/errors/switch-unavailable",
            "No fue posible comunicarse con el switch de autorización"
        );
    }

    private void performRequestAndAssertServiceUnavailable(String expectedType, String expectedDetail) throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", "550e8400-e29b-41d4-a716-446655440000")
                .content(validRequestJson()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().string("Retry-After", "30"))
            .andExpect(jsonPath("$.type").value(expectedType))
            .andExpect(jsonPath("$.title").value("Dependencia externa no disponible"))
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.detail").value(expectedDetail))
            .andExpect(jsonPath("$.instance").value("/api/v1/sales"));
    }

    private ProcessSaleCommand validCommand() {
        return new ProcessSaleCommand(
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
    }

    private String validRequestJson() {
        return """
            {
              "terminalId": "TERM-0001",
              "transactionType": "SALE",
              "totalAmount": 5633,
              "accountNumber": "55189800****2751",
              "expirationDate": "2805",
              "invoice": 14611279,
              "securityCodeEntry": "123",
              "securityValidationResponse": "1",
              "binValidate": true
            }
            """;
    }
}
