package com.ecommerce.sale.infrastructure.adapter.switch_api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SwitchAuthorizationResponse(
    String transactionId,
    @JsonAlias("authorizationSource") String provider,
    String status,
    String providerStatus,
    @JsonAlias("responseCode") String providerResponseCode,
    @JsonAlias("responseDescription") String providerMessage,
    @JsonAlias("authorizationNumber") String authorizationCode,
    String referenceNumber,
    String referenceCode,
    String traceId,
    String processedAt
) {}