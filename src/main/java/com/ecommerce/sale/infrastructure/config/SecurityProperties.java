package com.ecommerce.sale.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private String audience;
    private String tenantId;
    private String localJwtSecret;

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLocalJwtSecret() {
        return localJwtSecret;
    }

    public void setLocalJwtSecret(String localJwtSecret) {
        this.localJwtSecret = localJwtSecret;
    }
}
