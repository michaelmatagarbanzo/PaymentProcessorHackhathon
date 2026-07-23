package com.ecommerce.sale.infrastructure.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
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
                if (correlationId != null && !correlationId.isBlank()) {
                    Span.current().setAttribute("correlation.id", correlationId);
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
