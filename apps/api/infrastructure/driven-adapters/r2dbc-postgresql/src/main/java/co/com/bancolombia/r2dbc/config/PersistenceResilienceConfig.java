package co.com.bancolombia.r2dbc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.com.bancolombia.r2dbc.helper.PersistenceFailures;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@Configuration
public class PersistenceResilienceConfig {

    public static final String CIRCUIT_BREAKER_NAME = "persistence";

    @Bean
    public CircuitBreaker persistenceCircuitBreaker(PersistenceResilienceProperties properties) {
        PersistenceResilienceProperties.Breaker breaker = properties.circuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(breaker.failureRateThreshold())
                .slidingWindowSize(breaker.slidingWindowSize())
                .minimumNumberOfCalls(breaker.minimumNumberOfCalls())
                .waitDurationInOpenState(breaker.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(breaker.permittedNumberOfCallsInHalfOpenState())
                .recordException(PersistenceFailures::isInfrastructureFailure)
                .build();

        return CircuitBreaker.of(CIRCUIT_BREAKER_NAME, config);
    }
}
