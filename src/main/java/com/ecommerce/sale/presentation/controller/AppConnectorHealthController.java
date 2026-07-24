package com.ecommerce.sale.presentation.controller;

import com.ecommerce.sale.infrastructure.config.SwitchProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1/appconnector")
public class AppConnectorHealthController {

    private static final Logger LOG = LoggerFactory.getLogger(AppConnectorHealthController.class);
    private static final String DEFAULT_HEALTH_URL =
        "https://appconnector-demo-func-fnbpapgcf3h2eza9.eastus-01.azurewebsites.net/api/health";

    private final RestClient restClient;
    private final SwitchProperties switchProperties;

    public AppConnectorHealthController(SwitchProperties switchProperties) {
        this.restClient = RestClient.builder().build();
        this.switchProperties = switchProperties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        String endpoint = resolveHealthEndpoint();
        long startedAt = System.currentTimeMillis();
        try {
            ResponseEntity<String> upstream = restClient.get()
                .uri(endpoint)
                .retrieve()
                .toEntity(String.class);

            long duration = System.currentTimeMillis() - startedAt;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "UP");
            body.put("endpoint", endpoint);
            body.put("httpStatus", upstream.getStatusCode().value());
            body.put("durationMs", duration);
            body.put("response", upstream.getBody());
            body.put("timestamp", Instant.now());

            LOG.info("event=appconnector.health.success endpoint={} statusCode={} durationMs={}",
                endpoint, upstream.getStatusCode().value(), duration);
            return ResponseEntity.status(upstream.getStatusCode()).body(body);
        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - startedAt;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "DOWN");
            body.put("endpoint", endpoint);
            body.put("httpStatus", 503);
            body.put("durationMs", duration);
            body.put("errorType", ex.getClass().getSimpleName());
            body.put("errorMessage", ex.getMessage());
            body.put("timestamp", Instant.now());

            LOG.error("event=appconnector.health.failed endpoint={} durationMs={} errorType={} errorMessage={}",
                endpoint, duration, ex.getClass().getSimpleName(), ex.getMessage());
            return ResponseEntity.status(503).body(body);
        }
    }

    private String resolveHealthEndpoint() {
        String configured = switchProperties.resolveAppconnectorHealthUrl();
        if (configured == null || configured.isBlank()) {
            return DEFAULT_HEALTH_URL;
        }
        return configured;
    }
}
