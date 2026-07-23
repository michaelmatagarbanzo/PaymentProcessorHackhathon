package com.ecommerce.sale.infrastructure.config;

import com.ecommerce.sale.infrastructure.security.EntraJwtAuthenticationConverter;
import com.ecommerce.sale.infrastructure.security.JwtClaimsValidator;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityProperties.class, SwitchProperties.class})
public class SecurityConfig {

    @Bean
    @Profile("local")
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/sales", "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()
                .anyRequest()
                .permitAll());

        return http.build();
    }

    @Bean
    @Profile("!local")
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter entraJwtAuthenticationConverter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()
                .anyRequest()
                .authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(entraJwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter entraJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new com.ecommerce.sale.infrastructure.security.EntraJwtAuthenticationConverter());
        return converter;
    }

    @Bean
    @Profile("!local")
    JwtDecoder jwtDecoder(org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties properties,
                          SecurityProperties securityProperties) {
        String issuer = properties.getJwt().getIssuerUri();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> claimValidator = new JwtClaimsValidator(securityProperties);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, claimValidator));

        return decoder;
    }

    @Bean
    @Profile("local")
    JwtDecoder localJwtDecoder(SecurityProperties securityProperties) {
        String secret = securityProperties.getLocalJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.local-jwt-secret debe configurarse en el profile local");
        }

        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

}
