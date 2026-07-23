package com.ecommerce.sale.presentation.dto;

import jakarta.validation.constraints.Size;

public record ProcessingInformationDto(
    @Size(max = 50) String errorCentinel,
    @Size(max = 100) String statusReason
) {}