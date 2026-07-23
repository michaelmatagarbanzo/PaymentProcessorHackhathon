package com.ecommerce.sale.infrastructure.persistence.document;

import com.ecommerce.sale.domain.model.AuthorizationSource;
import com.ecommerce.sale.domain.model.TransactionStatus;
import com.ecommerce.sale.domain.model.TransactionType;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Transactions")
public class SaleTransactionDocument {

    @Id
    private String id;
    private String transactionId;
    private String correlationId;
    private String terminalId;
    private TransactionType transactionType;
    private Long totalAmount;
    private String accountNumber;
    private String expirationDate;
    private Long invoice;
    private String securityValidationResponse;
    private Boolean binValidate;
    private TransactionStatus status;
    private AuthorizationResponseDocument authorizationResult;
    private Instant createdAt;
    private Instant processingDateTime;
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getInvoice() {
        return invoice;
    }

    public void setInvoice(Long invoice) {
        this.invoice = invoice;
    }

    public String getSecurityValidationResponse() {
        return securityValidationResponse;
    }

    public void setSecurityValidationResponse(String securityValidationResponse) {
        this.securityValidationResponse = securityValidationResponse;
    }

    public Boolean getBinValidate() {
        return binValidate;
    }

    public void setBinValidate(Boolean binValidate) {
        this.binValidate = binValidate;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public AuthorizationResponseDocument getAuthorizationResult() {
        return authorizationResult;
    }

    public void setAuthorizationResult(AuthorizationResponseDocument authorizationResult) {
        this.authorizationResult = authorizationResult;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessingDateTime() {
        return processingDateTime;
    }

    public void setProcessingDateTime(Instant processingDateTime) {
        this.processingDateTime = processingDateTime;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class AuthorizationResponseDocument {

        private AuthorizationSource authorizationSource;
        private String authorizationNumber;
        private String responseCode;
        private String responseDescription;
        private String referenceNumber;
        private String hostDate;
        private String hostTime;

        public AuthorizationSource getAuthorizationSource() {
            return authorizationSource;
        }

        public void setAuthorizationSource(AuthorizationSource authorizationSource) {
            this.authorizationSource = authorizationSource;
        }

        public String getAuthorizationNumber() {
            return authorizationNumber;
        }

        public void setAuthorizationNumber(String authorizationNumber) {
            this.authorizationNumber = authorizationNumber;
        }

        public String getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(String responseCode) {
            this.responseCode = responseCode;
        }

        public String getResponseDescription() {
            return responseDescription;
        }

        public void setResponseDescription(String responseDescription) {
            this.responseDescription = responseDescription;
        }

        public String getReferenceNumber() {
            return referenceNumber;
        }

        public void setReferenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
        }

        public String getHostDate() {
            return hostDate;
        }

        public void setHostDate(String hostDate) {
            this.hostDate = hostDate;
        }

        public String getHostTime() {
            return hostTime;
        }

        public void setHostTime(String hostTime) {
            this.hostTime = hostTime;
        }
    }
}