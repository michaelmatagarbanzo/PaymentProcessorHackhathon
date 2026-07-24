package com.ecommerce.sale.presentation.mapper;

import com.ecommerce.sale.application.port.in.AuthenticationInformationCommand;
import com.ecommerce.sale.application.port.in.ProcessSaleCommand;
import com.ecommerce.sale.application.port.in.ProcessingInformationCommand;
import com.ecommerce.sale.application.port.in.TokenizationInformationCommand;
import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.presentation.dto.AuthenticationInformationDto;
import com.ecommerce.sale.presentation.dto.ProcessingInformationDto;
import com.ecommerce.sale.presentation.dto.SaleRequest;
import com.ecommerce.sale.presentation.dto.SaleResponse;
import com.ecommerce.sale.presentation.dto.TokenizationInformationDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SaleMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SaleMapper.class);

    public ProcessSaleCommand toCommand(SaleRequest request, String correlationId) {
        return new ProcessSaleCommand(
            correlationId,
            request.terminalId(),
            request.transactionType(),
            toMinorUnits(request.totalAmount()),
            request.currency(),
            request.accountNumber(),
            null,
            request.expirationDate(),
            request.invoice(),
            request.securityCodeEntry(),
            request.securityValidationResponse(),
            request.binValidate() != null ? request.binValidate() : Boolean.FALSE,
            toCommand(request.authenticationInformation()),
            toCommand(request.tokenizationInformation()),
            toCommand(request.processingInformation())
        );
    }

    public SaleResponse toResponse(SaleTransaction transaction) {
        LOG.info("event=sale.mapper.toResponse.started correlationId={} transactionId={} status={}",
            transaction.correlationId(), transaction.transactionId(), transaction.status());

        return new SaleResponse(
            transaction.transactionId(),
            transaction.correlationId(),
            transaction.status().name(),
            transaction.terminalId(),
            toMajorUnits(transaction.totalAmount()),
            null,
            null,
            toAuthorizationResult(transaction.authorizationResult()),
            transaction.processingDateTime(),
            transaction.createdAt(),
            null
        );
    }

    private AuthenticationInformationCommand toCommand(AuthenticationInformationDto dto) {
        if (dto == null) {
            return null;
        }
        return new AuthenticationInformationCommand(dto.eci(), dto.cavv(), dto.xid(), dto.enrollmentStatus());
    }

    private TokenizationInformationCommand toCommand(TokenizationInformationDto dto) {
        if (dto == null) {
            return null;
        }
        return new TokenizationInformationCommand(
            dto.wallet(),
            dto.device(),
            dto.paymentIndicator(),
            dto.cryptogramEci(),
            dto.cryptogram()
        );
    }

    private ProcessingInformationCommand toCommand(ProcessingInformationDto dto) {
        if (dto == null) {
            return null;
        }
        return new ProcessingInformationCommand(dto.errorCentinel(), dto.statusReason());
    }

    private SaleResponse.AuthorizationResultDto toAuthorizationResult(AuthorizationResponse response) {
        if (response == null) {
            return null;
        }
        return new SaleResponse.AuthorizationResultDto(
            response.authorizationSource().name(),
            response.authorizationNumber(),
            response.responseCode(),
            response.responseDescription(),
            response.referenceNumber(),
            response.hostDate(),
            response.hostTime()
        );
    }

    private Long toMinorUnits(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact();
    }

    private BigDecimal toMajorUnits(Long amountInCents) {
        if (amountInCents == null) {
            return null;
        }
        return BigDecimal.valueOf(amountInCents, 2);
    }
}