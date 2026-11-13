package co.com.bancolombia.r2dbc.helper;

import java.time.Duration;

import co.com.bancolombia.r2dbc.config.PersistenceResilienceConfig;
import co.com.bancolombia.r2dbc.config.PersistenceResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/** Builds the real PersistenceResilience with small values so tests run fast. */
public final class TestResilience {

    private TestResilience() {
    }

    public static PersistenceResilienceProperties properties(Duration timeout, int maxAttempts,
            int minimumNumberOfCalls) {
        return new PersistenceResilienceProperties(
                timeout,
                new PersistenceResilienceProperties.Retry(maxAttempts, Duration.ofMillis(10)),
                new PersistenceResilienceProperties.Breaker(50f, 10, minimumNumberOfCalls,
                        Duration.ofSeconds(30), 1));
    }

    public static CircuitBreaker breaker(PersistenceResilienceProperties properties) {
        return new PersistenceResilienceConfig().persistenceCircuitBreaker(properties);
    }

    public static PersistenceResilience create() {
        PersistenceResilienceProperties properties = properties(Duration.ofSeconds(2), 2, 100);
        return new PersistenceResilience(breaker(properties), properties);
    }
}
