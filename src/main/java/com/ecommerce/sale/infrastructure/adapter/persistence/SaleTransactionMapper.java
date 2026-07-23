package com.ecommerce.sale.infrastructure.adapter.persistence;

import com.ecommerce.sale.domain.model.AuthorizationResponse;
import com.ecommerce.sale.domain.model.SaleTransaction;
import com.ecommerce.sale.infrastructure.persistence.document.SaleTransactionDocument;
import org.springframework.stereotype.Component;

@Component
public class SaleTransactionMapper {

    private final PanMaskingService panMaskingService;

    public SaleTransactionMapper(PanMaskingService panMaskingService) {
        this.panMaskingService = panMaskingService;
    }

    public SaleTransactionDocument toDocument(SaleTransaction transaction) {
        SaleTransactionDocument document = new SaleTransactionDocument();
        document.setTransactionId(transaction.transactionId());
        document.setCorrelationId(transaction.correlationId());
        document.setTerminalId(transaction.terminalId());
        document.setTransactionType(transaction.transactionType());
        document.setTotalAmount(transaction.totalAmount());
        document.setAccountNumber(maskAccountNumber(transaction.accountNumber()));
        document.setExpirationDate(transaction.expirationDate());
        document.setInvoice(transaction.invoice());
        document.setSecurityValidationResponse(transaction.securityValidationResponse());
        document.setBinValidate(transaction.binValidate());
        document.setStatus(transaction.status());
        document.setAuthorizationResult(toDocument(transaction.authorizationResult()));
        document.setCreatedAt(transaction.createdAt());
        document.setProcessingDateTime(transaction.processingDateTime());
        document.setUpdatedAt(transaction.updatedAt());
        return document;
    }

    public SaleTransaction toDomain(SaleTransactionDocument document) {
        return new SaleTransaction(
            document.getTransactionId(),
            document.getCorrelationId(),
            document.getTerminalId(),
            document.getTransactionType(),
            document.getTotalAmount(),
            document.getAccountNumber(),
            document.getExpirationDate(),
            document.getInvoice(),
            document.getSecurityValidationResponse(),
            document.getBinValidate(),
            document.getStatus(),
            toDomain(document.getAuthorizationResult()),
            document.getCreatedAt(),
            document.getProcessingDateTime(),
            document.getUpdatedAt()
        );
    }

    public String maskAccountNumber(String accountNumber) {
        return panMaskingService.mask(accountNumber);
    }

    private SaleTransactionDocument.AuthorizationResponseDocument toDocument(AuthorizationResponse response) {
        if (response == null) {
            return null;
        }
        SaleTransactionDocument.AuthorizationResponseDocument document =
            new SaleTransactionDocument.AuthorizationResponseDocument();
        document.setAuthorizationSource(response.authorizationSource());
        document.setAuthorizationNumber(response.authorizationNumber());
        document.setResponseCode(response.responseCode());
        document.setResponseDescription(response.responseDescription());
        document.setReferenceNumber(response.referenceNumber());
        document.setHostDate(response.hostDate());
        document.setHostTime(response.hostTime());
        return document;
    }

    private AuthorizationResponse toDomain(SaleTransactionDocument.AuthorizationResponseDocument document) {
        if (document == null) {
            return null;
        }
        return new AuthorizationResponse(
            document.getAuthorizationSource(),
            document.getAuthorizationNumber(),
            document.getResponseCode(),
            document.getResponseDescription(),
            document.getReferenceNumber(),
            document.getHostDate(),
            document.getHostTime()
        );
    }
}