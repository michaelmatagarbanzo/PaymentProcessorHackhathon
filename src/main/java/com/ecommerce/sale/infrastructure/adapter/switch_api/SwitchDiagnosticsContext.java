package com.ecommerce.sale.infrastructure.adapter.switch_api;

import java.util.Map;

public final class SwitchDiagnosticsContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = new ThreadLocal<>();

    private SwitchDiagnosticsContext() {}

    public static void set(Map<String, Object> diagnostics) {
        CONTEXT.set(diagnostics);
    }

    public static Map<String, Object> get() {
        return CONTEXT.get();
    }

    public static Map<String, Object> pop() {
        Map<String, Object> value = CONTEXT.get();
        CONTEXT.remove();
        return value;
    }
}
