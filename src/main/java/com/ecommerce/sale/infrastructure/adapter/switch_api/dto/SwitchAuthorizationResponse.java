package com.ecommerce.sale.infrastructure.adapter.switch_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SwitchAuthorizationResponse(
    String authorizationSource,
    String authorizationNumber,
    String responseCode,
    String responseDescription,
    String referenceNumber,
    String hostDate,
    String hostTime
) {}