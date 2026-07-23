package com.ecommerce.sale.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaleRequest(
    @NotBlank @Size(max = 20) String terminalId,
    @NotBlank @Pattern(regexp = "SALE") String transactionType,
    @NotNull @Min(1) Long totalAmount,
    @NotBlank @Size(min = 8, max = 100) String accountNumber,
    @NotBlank @Pattern(regexp = "^(\\d{2})(0[1-9]|1[0-2])$") String expirationDate,
    @NotNull @Min(1) Long invoice,
    @NotBlank @Size(min = 1, max = 8) String securityCodeEntry,
    @NotBlank @Size(max = 20) String securityValidationResponse,
    Boolean binValidate,
    @Valid AuthenticationInformationDto authenticationInformation,
    @Valid TokenizationInformationDto tokenizationInformation,
    @Valid ProcessingInformationDto processingInformation
) {}