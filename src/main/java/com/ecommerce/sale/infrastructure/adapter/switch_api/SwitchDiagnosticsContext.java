package com.ecommerce.sale.infrastructure.adapter.switch_api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SwitchDiagnosticsContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = new ThreadLocal<>();
    private static final ConcurrentMap<String, Map<String, Object>> BY_CORRELATION_ID = new ConcurrentHashMap<>();

    private SwitchDiagnosticsContext() {}

    public static void set(Map<String, Object> diagnostics) {
        CONTEXT.set(diagnostics);
        if (diagnostics == null) {
            return;
        }
        Object correlationId = diagnostics.get("correlationId");
        if (correlationId instanceof String id && !id.isBlank()) {
            BY_CORRELATION_ID.put(id, diagnostics);
        }
    }

    public static Map<String, Object> get() {
        return CONTEXT.get();
    }

    public static Map<String, Object> pop() {
        Map<String, Object> value = CONTEXT.get();
        CONTEXT.remove();
        if (value != null) {
            Object correlationId = value.get("correlationId");
            if (correlationId instanceof String id && !id.isBlank()) {
                BY_CORRELATION_ID.remove(id);
            }
        }
        return value;
    }

    public static Map<String, Object> popByCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }
        return BY_CORRELATION_ID.remove(correlationId);
    }
}
