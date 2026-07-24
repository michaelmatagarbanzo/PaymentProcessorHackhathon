package com.ecommerce.sale.presentation.config;

import com.ecommerce.sale.application.port.out.AuthorizationSwitchPort;
import com.ecommerce.sale.application.port.out.ObservabilityPort;
import com.ecommerce.sale.application.port.out.SwitchAuthenticationPort;
import com.ecommerce.sale.application.port.out.TransactionRepositoryPort;
import com.ecommerce.sale.application.usecase.AuthorizeTransactionUseCase;
import com.ecommerce.sale.application.usecase.DuplicateDetectionService;
import com.ecommerce.sale.application.usecase.GenerateTransactionIdUseCase;
import com.ecommerce.sale.application.usecase.PersistTransactionUseCase;
import com.ecommerce.sale.application.usecase.ProcessSaleUseCase;
import com.ecommerce.sale.application.usecase.ValidateSaleRequestUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    GenerateTransactionIdUseCase generateTransactionIdUseCase() {
        return new GenerateTransactionIdUseCase();
    }

    @Bean
    AuthorizeTransactionUseCase authorizeTransactionUseCase(
        AuthorizationSwitchPort authorizationSwitchPort
    ) {
        return new AuthorizeTransactionUseCase(authorizationSwitchPort);
    }

    @Bean
    PersistTransactionUseCase persistTransactionUseCase(TransactionRepositoryPort transactionRepositoryPort) {
        return new PersistTransactionUseCase(transactionRepositoryPort);
    }

    @Bean
    ValidateSaleRequestUseCase validateSaleRequestUseCase() {
        return new ValidateSaleRequestUseCase();
    }

    @Bean
    DuplicateDetectionService duplicateDetectionService(TransactionRepositoryPort transactionRepositoryPort) {
        return new DuplicateDetectionService(transactionRepositoryPort);
    }

    @Bean
    ProcessSaleUseCase processSaleUseCase(
        ValidateSaleRequestUseCase validateSaleRequestUseCase,
        DuplicateDetectionService duplicateDetectionService,
        GenerateTransactionIdUseCase generateTransactionIdUseCase,
        AuthorizeTransactionUseCase authorizeTransactionUseCase,
        PersistTransactionUseCase persistTransactionUseCase,
        ObservabilityPort observabilityPort
    ) {
        return new ProcessSaleUseCase(
            validateSaleRequestUseCase,
            duplicateDetectionService,
            generateTransactionIdUseCase,
            authorizeTransactionUseCase,
            persistTransactionUseCase,
            observabilityPort
        );
    }
}