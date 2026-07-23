package com.ecommerce.sale.infrastructure.security;

import com.ecommerce.sale.infrastructure.config.SecurityProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE =
        new OAuth2Error("invalid_token", "Invalid audience", null);
    private static final OAuth2Error INVALID_TENANT =
        new OAuth2Error("invalid_token", "Invalid tenant", null);
    private static final OAuth2Error EXPIRED_TOKEN =
        new OAuth2Error("invalid_token", "Expired token", null);

    private final SecurityProperties securityProperties;

    public JwtClaimsValidator(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            return OAuth2TokenValidatorResult.failure(EXPIRED_TOKEN);
        }

        List<String> audience = token.getAudience();
        if (securityProperties.getAudience() != null
            && !securityProperties.getAudience().isBlank()
            && (audience == null || !audience.contains(securityProperties.getAudience()))) {
            return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
        }

        String tenant = token.getClaimAsString("tid");
        if (securityProperties.getTenantId() != null
            && !securityProperties.getTenantId().isBlank()
            && !"common".equalsIgnoreCase(securityProperties.getTenantId())
            && !securityProperties.getTenantId().equalsIgnoreCase(tenant)) {
            return OAuth2TokenValidatorResult.failure(INVALID_TENANT);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
