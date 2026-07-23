package com.ecommerce.sale.infrastructure.config;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class OpenTelemetryConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    private final String serviceName;

    public OpenTelemetryConfig(@Value("${spring.application.name:sale-api}") String serviceName) {
        this.serviceName = serviceName;
    }

    @Bean
    OpenTelemetry openTelemetry() {
        String connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");
        if (connectionString == null || connectionString.isBlank()) {
            LOG.warn("Application Insights exporter disabled: APPLICATIONINSIGHTS_CONNECTION_STRING is not set");
            return GlobalOpenTelemetry.get();
        }

        try {
            AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
                .setResultAsGlobal()
                .addPropertiesSupplier(() -> {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("otel.service.name", serviceName);
                    properties.put("otel.logs.exporter", "azuremonitor");
                    properties.put("otel.traces.exporter", "none");
                    properties.put("otel.metrics.exporter", "none");
                    return properties;
                });

            AzureMonitorExporter.customize(builder, connectionString);
            OpenTelemetry openTelemetry = builder.build().getOpenTelemetrySdk();
            LOG.info("Application Insights exporter enabled");
            return openTelemetry;
        } catch (RuntimeException ex) {
            LOG.warn("Global OpenTelemetry already initialized externally; reusing existing global instance");
            return GlobalOpenTelemetry.get();
        }
    }

    @Bean
    ApplicationRunner applicationInsightsConnectivityLog() {
        return args -> LOG.info("Application Insights connectivity test");
    }

    @Bean
    OncePerRequestFilter traceAttributeFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {
                String correlationId = request.getHeader("X-Correlation-Id");
                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = MDC.get("correlationId");
                }
                if (correlationId != null && !correlationId.isBlank()) {
                    Span.current().setAttribute("correlation.id", correlationId);
                    MDC.put("correlationId", correlationId);
                }

                SpanContext spanContext = Span.current().getSpanContext();
                if (spanContext.isValid()) {
                    MDC.put("traceId", spanContext.getTraceId());
                    MDC.put("spanId", spanContext.getSpanId());
                    response.setHeader("traceparent",
                        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-01");
                }

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    MDC.remove("traceId");
                    MDC.remove("spanId");
                }
            }
        };
    }

}
