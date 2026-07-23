package com.ecommerce.sale.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    @Bean
    CircuitBreakerConfig switchApiCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .slidingWindowSize(10)
            .build();
    }

    @Bean
    RetryConfig switchApiRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .build();
    }

    @Bean
    io.github.resilience4j.bulkhead.BulkheadConfig switchApiBulkheadConfig() {
        return io.github.resilience4j.bulkhead.BulkheadConfig.custom()
            .maxConcurrentCalls(25)
            .build();
    }

    @Bean
    TimeLimiterConfig switchApiTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
    }

    @Bean
    RateLimiterConfig switchApiRateLimiterConfig() {
        return RateLimiterConfig.custom()
            .limitForPeriod(200)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ZERO)
            .build();
    }
}
