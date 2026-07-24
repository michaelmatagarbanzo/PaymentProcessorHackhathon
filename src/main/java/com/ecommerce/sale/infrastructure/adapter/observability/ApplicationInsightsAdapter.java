package com.ecommerce.sale.infrastructure.adapter.observability;

import com.ecommerce.sale.application.port.out.ObservabilityPort;
import com.ecommerce.sale.infrastructure.config.GrafanaMetricsConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInsightsAdapter implements ObservabilityPort {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationInsightsAdapter.class);

    private final GrafanaMetricsConfig grafanaMetricsConfig;

    public ApplicationInsightsAdapter(GrafanaMetricsConfig grafanaMetricsConfig) {
        this.grafanaMetricsConfig = grafanaMetricsConfig;
    }

    @Override
    public void event(String eventName, Map<String, String> attributes) {
        Map<String, String> safeAttributes = sanitize(attributes);
        Span.current().addEvent(eventName, toOtelAttributes(safeAttributes));
        // Keep structured log with complete attributes to ease Grafana/AppTraces filtering.
        LOG.info("event={} attributes={}", eventName, safeAttributes);
    }

    @Override
    public void increment(String metricName, Map<String, String> tags) {
        grafanaMetricsConfig.incrementCounter(metricName, tags);
    }

    @Override
    public void timing(String metricName, long durationMs, Map<String, String> tags) {
        grafanaMetricsConfig.recordTiming(metricName, durationMs, tags);
    }

    private Attributes toOtelAttributes(Map<String, String> attributes) {
        AttributesBuilder builder = Attributes.builder();
        attributes.forEach((k, v) -> builder.put(AttributeKey.stringKey(k), v));
        return builder.build();
    }

    private Map<String, String> sanitize(Map<String, String> attributes) {
        Map<String, String> safe = new LinkedHashMap<>();
        if (attributes == null || attributes.isEmpty()) {
            safe.put("eventPayload", "none");
            return safe;
        }
        attributes.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                return;
            }
            safe.put(key, value == null ? "unknown" : value);
        });
        if (safe.isEmpty()) {
            safe.put("eventPayload", "none");
        }
        return safe;
    }
}