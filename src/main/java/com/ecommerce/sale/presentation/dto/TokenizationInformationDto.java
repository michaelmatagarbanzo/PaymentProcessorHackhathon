package com.ecommerce.sale.presentation.dto;

import jakarta.validation.constraints.Size;

public record TokenizationInformationDto(
    @Size(max = 50) String wallet,
    @Size(max = 50) String device,
    @Size(max = 50) String paymentIndicator,
    @Size(max = 10) String cryptogramEci,
    @Size(max = 255) String cryptogram
) {}