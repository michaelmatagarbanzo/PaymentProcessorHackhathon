package com.ecommerce.sale.infrastructure.adapter.switch_api;

import com.ecommerce.sale.application.port.out.SwitchAuthenticationPort;
import com.ecommerce.sale.domain.exception.SwitchAuthenticationException;
import com.ecommerce.sale.infrastructure.adapter.security.KeyVaultAdapter;
import com.ecommerce.sale.infrastructure.config.SwitchProperties;
import com.ecommerce.sale.infrastructure.exception.ExternalDependencyUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class SwitchOAuthAdapter implements SwitchAuthenticationPort {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchOAuthAdapter.class);
    private static final long EXPIRATION_SKEW_SECONDS = 30L;

    private final RestClient restClient;
    private final SwitchProperties switchProperties;
    private final KeyVaultAdapter keyVaultAdapter;
    private volatile CachedToken cachedToken = CachedToken.expired();

    public SwitchOAuthAdapter(SwitchProperties switchProperties, KeyVaultAdapter keyVaultAdapter) {
        this.restClient = RestClient.builder().build();
        this.switchProperties = switchProperties;
        this.keyVaultAdapter = keyVaultAdapter;
    }

    @Override
    public String getAccessToken() {
        if (cachedToken.isValidAt(Instant.now())) {
            return cachedToken.accessToken();
        }
        synchronized (this) {
            if (cachedToken.isValidAt(Instant.now())) {
                return cachedToken.accessToken();
            }
            cachedToken = requestToken();
            return cachedToken.accessToken();
        }
    }

    private CachedToken requestToken() {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("client_id", switchProperties.getOauth().getClientId());
            formData.add("client_secret", resolveClientSecret());
            formData.add("scope", switchProperties.getOauth().getScope());

            TokenResponse response = restClient.post()
                .uri(switchProperties.getOauth().getTokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(TokenResponse.class);

            if (response == null || response.access_token() == null || response.access_token().isBlank()) {
                throw new SwitchAuthenticationException("El endpoint OAuth no devolvió access_token");
            }

            long expiresIn = response.expires_in() > EXPIRATION_SKEW_SECONDS
                ? response.expires_in() - EXPIRATION_SKEW_SECONDS
                : response.expires_in();

            return new CachedToken(response.access_token(), Instant.now().plusSeconds(expiresIn));
        } catch (RuntimeException ex) {
            LOG.error("event=switch.oauth.failed dependency=switch correlationId={} error={}",
                "unknown", ex.getMessage());
            LOG.error("event=sale.error correlationId={} transactionId={} errorType={} errorCode={} errorMessage={}",
                "unknown",
                "unknown",
                ex.getClass().getSimpleName(),
                "SWITCH_OAUTH_FAILED",
                ex.getMessage(),
                ex);
            if (isExternalDependencyFailure(ex)) {
                throw new ExternalDependencyUnavailableException("switch", ex);
            }
            if (ex instanceof SwitchAuthenticationException) {
                throw ex;
            }
            throw new SwitchAuthenticationException("No se pudo autenticar contra el API Switch", ex);
        }
    }

    private String resolveClientSecret() {
        String configuredSecret = switchProperties.getOauth().getClientSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new SwitchAuthenticationException("switch.oauth.client-secret no está configurado");
        }
        if (configuredSecret.startsWith("kv:")) {
            return keyVaultAdapter.getSecretValue(configuredSecret.substring(3));
        }
        return configuredSecret;
    }

    private record CachedToken(String accessToken, Instant expiresAt) {

        static CachedToken expired() {
            return new CachedToken("", Instant.EPOCH);
        }

        boolean isValidAt(Instant now) {
            return accessToken != null && !accessToken.isBlank() && expiresAt.isAfter(now);
        }
    }

    private record TokenResponse(String access_token, long expires_in) {}

    private boolean isExternalDependencyFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ExternalDependencyUnavailableException
                || current instanceof ResourceAccessException
                || current instanceof HttpServerErrorException
                || current instanceof CallNotPermittedException
                || current instanceof TimeoutException
                || current instanceof ConnectException
                || current instanceof SocketTimeoutException
                || current.getClass().getName().equals("org.springframework.web.reactive.function.client.WebClientRequestException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}