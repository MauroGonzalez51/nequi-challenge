package co.com.bancolombia.r2dbc.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import co.com.bancolombia.model.exception.DataConflictException;
import co.com.bancolombia.model.exception.ServiceUnavailableException;
import co.com.bancolombia.r2dbc.config.PersistenceResilienceProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistenceResilienceTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    private final PersistenceResilienceProperties properties = TestResilience.properties(Duration.ofMillis(100), 3, 2);
    private final CircuitBreaker breaker = TestResilience.breaker(properties);
    private final PersistenceResilience resilience = new PersistenceResilience(breaker, properties);

    private DataAccessResourceFailureException down() {
        return new DataAccessResourceFailureException("db down");
    }

    private Consumer<Throwable> unavailableCausedBy(Class<? extends Throwable> cause) {
        return error -> assertThat(error)
                .isInstanceOf(ServiceUnavailableException.class)
                .hasCauseInstanceOf(cause);
    }

    // ---- retry

    @Test
    void shouldReturnTheValueWhenTheCallSucceeds() {
        StepVerifier.create(resilience.read(() -> Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();
    }

    @Test
    void shouldRetryAReadUntilItSucceeds() {
        AtomicInteger calls = new AtomicInteger();

        StepVerifier.create(resilience.read(() -> calls.incrementAndGet() < 3 ? Mono.<String>error(down())
                : Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();

        assertThat(calls).hasValue(3);
    }

    @Test
    void shouldGiveUpAfterMaxAttemptsAndFailAsServiceUnavailable() {
        AtomicInteger calls = new AtomicInteger();

        StepVerifier.create(resilience.read(() -> {
            calls.incrementAndGet();
            return Mono.<String>error(down());
        }))
                .expectErrorSatisfies(unavailableCausedBy(DataAccessResourceFailureException.class))
                .verify(WAIT);

        assertThat(calls).hasValue(3);
    }

    @Test
    void shouldNotRetryAnIntegrityViolationAndFailAsDataConflict() {
        AtomicInteger calls = new AtomicInteger();

        StepVerifier.create(resilience.read(() -> {
            calls.incrementAndGet();
            return Mono.<String>error(new DataIntegrityViolationException("duplicate"));
        }))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(DataConflictException.class)
                        .hasCauseInstanceOf(DataIntegrityViolationException.class))
                .verify(WAIT);

        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldNotRetryAWrite() {
        AtomicInteger calls = new AtomicInteger();

        StepVerifier.create(resilience.write(() -> {
            calls.incrementAndGet();
            return Mono.<String>error(down());
        }))
                .expectErrorSatisfies(unavailableCausedBy(DataAccessResourceFailureException.class))
                .verify(WAIT);

        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldRetryAManyReadAsAWholeWithoutDuplicatingItems() {
        AtomicInteger calls = new AtomicInteger();

        StepVerifier.create(resilience.readMany(() -> calls.incrementAndGet() == 1
                ? Flux.concat(Flux.just(1), Flux.<Integer>error(down()))
                : Flux.just(1, 2)))
                .expectNext(1, 2)
                .verifyComplete();

        assertThat(calls).hasValue(2);
    }

    // ---- timeout

    @Test
    void shouldTimeOutAWrite() {
        StepVerifier.create(resilience.write(Mono::never))
                .expectErrorSatisfies(unavailableCausedBy(TimeoutException.class))
                .verify(WAIT);
    }

    @Test
    void shouldTimeOutAReadAfterTheRetriesAreSpent() {
        AtomicInteger calls = new AtomicInteger();

        StepVerifier.create(resilience.read(() -> {
            calls.incrementAndGet();
            return Mono.never();
        }))
                .expectErrorSatisfies(unavailableCausedBy(TimeoutException.class))
                .verify(WAIT);

        assertThat(calls).hasValue(3);
    }

    // ---- circuit breaker

    @Test
    void shouldOpenTheCircuitAfterRepeatedFailuresAndFailFast() {
        AtomicInteger calls = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            StepVerifier.create(resilience.write(() -> {
                calls.incrementAndGet();
                return Mono.<String>error(down());
            }))
                    .expectErrorSatisfies(unavailableCausedBy(DataAccessResourceFailureException.class))
                    .verify(WAIT);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        StepVerifier.create(resilience.write(() -> {
            calls.incrementAndGet();
            return Mono.just("never reached");
        }))
                .expectErrorSatisfies(unavailableCausedBy(CallNotPermittedException.class))
                .verify(WAIT);

        assertThat(calls).hasValue(2);
    }

    @Test
    void shouldNotOpenTheCircuitForErrorsThatAreNotInfrastructureFailures() {
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(resilience.write(() -> Mono.<String>error(new DataIntegrityViolationException("dup"))))
                    .expectError(DataConflictException.class)
                    .verify(WAIT);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
