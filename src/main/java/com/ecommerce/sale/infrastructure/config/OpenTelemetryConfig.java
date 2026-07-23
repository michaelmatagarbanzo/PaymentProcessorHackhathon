package com.ecommerce.sale.infrastructure.config;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporter;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;
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
        try {
            AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
                .setResultAsGlobal()
                .addPropertiesSupplier(() -> {
                    Map<String, String> properties = new HashMap<>();
                    properties.put("otel.service.name", serviceName);
                    properties.put("otel.traces.exporter", "none");
                    properties.put("otel.metrics.exporter", "none");
                    properties.put("otel.logs.exporter", "none");
                    return properties;
                });

            if (connectionString == null || connectionString.isBlank()) {
                LOG.info("AI_EXPORTER_ENABLED=false");
                LOG.warn("Application Insights exporter disabled: APPLICATIONINSIGHTS_CONNECTION_STRING is not set");
            } else {
                LOG.info("AI_EXPORTER_ENABLED=true");
                LOG.info("AI_EXPORTER_CONNECTION_ACTIVE");
                AzureMonitorExporter.customize(builder, connectionString);
                LOG.info("AI_LOG_EXPORT_START");
            }

            OpenTelemetry openTelemetry = builder.build().getOpenTelemetrySdk();
            if (connectionString == null || connectionString.isBlank()) {
                LOG.info("OpenTelemetry SDK initialized without Azure exporter");
            } else {
                LOG.info("Application Insights exporter enabled");
                LOG.info("AI_LOG_EXPORT_SUCCESS");
            }
            return openTelemetry;
        } catch (RuntimeException ex) {
            LOG.error("AI_LOG_EXPORT_FAILURE", ex);
            throw ex;
        }
    }

    @Bean
    ApplicationRunner applicationInsightsConnectivityLog() {
        return args -> LOG.info("Application Insights connectivity test");
    }

    @Bean
    OncePerRequestFilter traceAttributeFilter(OpenTelemetry openTelemetry) {
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
                    MDC.put("correlationId", correlationId);
                }

                Span requestSpan = openTelemetry.getTracer("sale-api")
                    .spanBuilder(request.getMethod() + " " + request.getRequestURI())
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan();
                if (correlationId != null && !correlationId.isBlank()) {
                    requestSpan.setAttribute("correlation.id", correlationId);
                }

                try (Scope scope = requestSpan.makeCurrent()) {
                    SpanContext spanContext = requestSpan.getSpanContext();
                    MDC.put("traceId", spanContext.getTraceId());
                    MDC.put("spanId", spanContext.getSpanId());
                    response.setHeader("traceparent",
                        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-01");
                    filterChain.doFilter(request, response);
                } finally {
                    requestSpan.end();
                    MDC.remove("traceId");
                    MDC.remove("spanId");
                }
            }
        };
    }

}
