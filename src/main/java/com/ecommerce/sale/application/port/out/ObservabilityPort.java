package com.ecommerce.sale.application.port.out;

import java.util.Map;

/**
 * Puerto de observabilidad para publicación de eventos y métricas desde application.
 */
public interface ObservabilityPort {

    void event(String eventName, Map<String, String> attributes);

    void increment(String metricName, Map<String, String> tags);

    void timing(String metricName, long durationMs, Map<String, String> tags);
}