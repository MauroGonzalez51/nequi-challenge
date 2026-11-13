package co.com.bancolombia.r2dbc.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Resilience settings for every call to the database. Values come from
 * {@code resilience.persistence.*} in application.yaml.
 */
@ConfigurationProperties(prefix = "resilience.persistence")
public record PersistenceResilienceProperties(
        Duration timeout,
        Retry retry,
        Breaker circuitBreaker) {

    public record Retry(int maxAttempts, Duration backoff) {
    }

    public record Breaker(
            float failureRateThreshold,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            Duration waitDurationInOpenState,
            int permittedNumberOfCallsInHalfOpenState) {
    }
}
