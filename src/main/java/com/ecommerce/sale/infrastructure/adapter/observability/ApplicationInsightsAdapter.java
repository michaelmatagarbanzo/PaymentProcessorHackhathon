package com.ecommerce.sale.infrastructure.adapter.observability;

import com.ecommerce.sale.application.port.out.ObservabilityPort;
import com.ecommerce.sale.infrastructure.config.GrafanaMetricsConfig;
import io.opentelemetry.api.trace.Span;
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
        Span.current().addEvent(eventName);
        LOG.info("event={} attributes={}", eventName, attributes);
    }

    @Override
    public void increment(String metricName, Map<String, String> tags) {
        grafanaMetricsConfig.incrementCounter(metricName, tags);
    }

    @Override
    public void timing(String metricName, long durationMs, Map<String, String> tags) {
        grafanaMetricsConfig.recordTiming(metricName, durationMs, tags);
    }
}