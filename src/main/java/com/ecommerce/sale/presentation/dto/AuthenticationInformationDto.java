package com.ecommerce.sale.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationInformationDto(
    @NotBlank @Size(max = 10) String eci,
    @NotBlank @Size(max = 255) String cavv,
    @Size(max = 255) String xid,
    @Size(max = 10) String enrollmentStatus
) {}