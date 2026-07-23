package com.ecommerce.sale.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GrafanaMetricsConfig {

    private final MeterRegistry meterRegistry;

    public GrafanaMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementCounter(String metricName, Map<String, String> tags) {
        meterRegistry.counter(metricName, toTags(tags)).increment();
    }

    public void recordTiming(String metricName, long durationMs, Map<String, String> tags) {
        Timer.builder(metricName)
            .tags(toTags(tags))
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
    }

    private Iterable<Tag> toTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Tags.empty();
        }
        List<Tag> result = new ArrayList<>();
        tags.forEach((k, v) -> result.add(Tag.of(k, v == null ? "unknown" : v)));
        return result;
    }
}