package co.com.bancolombia.r2dbc.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

class PersistenceResilienceConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestProperties.class, PersistenceResilienceConfig.class)
            .withPropertyValues(
                    "resilience.persistence.timeout=2s",
                    "resilience.persistence.retry.max-attempts=3",
                    "resilience.persistence.retry.backoff=200ms",
                    "resilience.persistence.circuit-breaker.failure-rate-threshold=50",
                    "resilience.persistence.circuit-breaker.sliding-window-size=10",
                    "resilience.persistence.circuit-breaker.minimum-number-of-calls=5",
                    "resilience.persistence.circuit-breaker.wait-duration-in-open-state=10s",
                    "resilience.persistence.circuit-breaker.permitted-number-of-calls-in-half-open-state=3");

    @Test
    void shouldBindTheYamlValuesToTheProperties() {
        runner.run(context -> {
            PersistenceResilienceProperties properties = context.getBean(PersistenceResilienceProperties.class);

            assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.retry().maxAttempts()).isEqualTo(3);
            assertThat(properties.retry().backoff()).isEqualTo(Duration.ofMillis(200));
            assertThat(properties.circuitBreaker().waitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    void shouldBuildAClosedCircuitBreakerWithTheConfiguredValues() {
        runner.run(context -> {
            CircuitBreaker breaker = context.getBean(CircuitBreaker.class);

            assertThat(breaker.getName()).isEqualTo("persistence");
            assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(breaker.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50f);
            assertThat(breaker.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
            assertThat(breaker.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(5);
            assertThat(breaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        });
    }

    @Configuration
    @EnableConfigurationProperties(PersistenceResilienceProperties.class)
    static class TestProperties {
    }
}
