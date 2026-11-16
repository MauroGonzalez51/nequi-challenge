package co.com.bancolombia.r2dbc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.com.bancolombia.r2dbc.helper.PersistenceFailures;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@Configuration
public class PersistenceResilienceConfig {

    public static final String CIRCUIT_BREAKER_NAME = "persistence";

    /**
     * Circuit breaker shared by every database call. Its numbers come from application.yaml.
     */
    @Bean
    public CircuitBreaker persistenceCircuitBreaker(PersistenceResilienceProperties properties) {
        PersistenceResilienceProperties.Breaker breaker = properties.circuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(breaker.failureRateThreshold())
                .slidingWindowSize(breaker.slidingWindowSize())
                .minimumNumberOfCalls(breaker.minimumNumberOfCalls())
                .waitDurationInOpenState(breaker.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(breaker.permittedNumberOfCallsInHalfOpenState())
                // Only infrastructure failures count. A duplicate key, or any other answer from the database, must not open the circuit.
                .recordException(PersistenceFailures::isInfrastructureFailure)
                .build();

        return CircuitBreaker.of(CIRCUIT_BREAKER_NAME, config);
    }
}
