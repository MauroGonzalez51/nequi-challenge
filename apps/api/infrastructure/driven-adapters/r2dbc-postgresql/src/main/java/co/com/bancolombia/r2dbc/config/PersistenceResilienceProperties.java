package co.com.bancolombia.r2dbc.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Resilience settings for every call to the database. Values come from
 * {@code resilience.persistence.*} in application.yaml.
 */
@ConfigurationProperties(prefix = "resilience.persistence")
public record PersistenceResilienceProperties(
        // Per attempt, not for the whole operation: retries add up.
        Duration timeout,
        Retry retry,
        Breaker circuitBreaker) {

    /**
     * maxAttempts includes the first call. backoff is the first wait; later waits grow exponentially.
     */
    public record Retry(int maxAttempts, Duration backoff) {
    }

    /**
     * Opens when failureRateThreshold % of the last slidingWindowSize calls failed (judged only after
     * minimumNumberOfCalls). After waitDurationInOpenState it lets permittedNumberOfCallsInHalfOpenState
     * calls through to test whether the database recovered.
     */
    public record Breaker(
            float failureRateThreshold,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            Duration waitDurationInOpenState,
            int permittedNumberOfCallsInHalfOpenState) {
    }
}
