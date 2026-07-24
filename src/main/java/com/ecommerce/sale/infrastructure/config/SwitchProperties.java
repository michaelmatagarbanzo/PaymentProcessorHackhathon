package com.ecommerce.sale.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "switch")
public class SwitchProperties {

    private String baseUrl;
    private String apiKey;
    private int timeoutMs;
    private final Appconnector appconnector = new Appconnector();
    private final OAuth oauth = new OAuth();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public OAuth getOauth() {
        return oauth;
    }

    public Appconnector getAppconnector() {
        return appconnector;
    }

    public String resolveAppconnectorBaseUrl() {
        if (appconnector.baseUrl != null && !appconnector.baseUrl.isBlank()) {
            return appconnector.baseUrl;
        }
        return baseUrl;
    }

    public String resolveAppconnectorApiKey() {
        if (appconnector.apiKey != null && !appconnector.apiKey.isBlank()) {
            return appconnector.apiKey;
        }
        return apiKey;
    }

    public String resolveAppconnectorHealthUrl() {
        if (appconnector.healthUrl != null && !appconnector.healthUrl.isBlank()) {
            return appconnector.healthUrl;
        }
        String resolvedBaseUrl = resolveAppconnectorBaseUrl();
        if (resolvedBaseUrl == null || resolvedBaseUrl.isBlank()) {
            return null;
        }
        return resolvedBaseUrl + "/api/health";
    }

    public static class Appconnector {

        private String baseUrl;
        private String apiKey;
        private String healthUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getHealthUrl() {
            return healthUrl;
        }

        public void setHealthUrl(String healthUrl) {
            this.healthUrl = healthUrl;
        }
    }

    public static class OAuth {

        private String tokenEndpoint;
        private String clientId;
        private String clientSecret;
        private String scope;

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public void setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
