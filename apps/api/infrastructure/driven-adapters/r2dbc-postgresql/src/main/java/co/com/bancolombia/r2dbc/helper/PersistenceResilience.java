package co.com.bancolombia.r2dbc.helper;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import co.com.bancolombia.model.exception.ServiceUnavailableException;
import co.com.bancolombia.r2dbc.config.PersistenceResilienceProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Wraps every database call with the resilience patterns, in this order from
 * the inside out: timeout (per attempt), retry with backoff, circuit breaker.
 * The breaker is outermost so it counts one failure per whole operation and,
 * when open, fails fast without retrying.
 *
 * Reads are retried. Writes are not: an insert whose answer was lost to a
 * timeout may have been committed, and retrying it would duplicate the row.
 * Writes still get the timeout and the circuit breaker.
 *
 * Whatever ends up failing because the database is not usable (open circuit,
 * timeout, connection errors, retries spent) leaves as a domain
 * ServiceUnavailableException, so upper layers never see persistence or
 * Resilience4j types. Other errors, like a duplicate key, pass through as is.
 *
 * Every method takes a Supplier so each attempt calls the repository again
 * (Mono.defer); retrying an already built Mono would not re-run the call.
 */
@Component
public class PersistenceResilience {

    private final CircuitBreaker circuitBreaker;
    private final PersistenceResilienceProperties properties;

    public PersistenceResilience(CircuitBreaker circuitBreaker, PersistenceResilienceProperties properties) {
        this.circuitBreaker = circuitBreaker;
        this.properties = properties;
    }

    /**
     * One value: timeout per attempt, then retries, all inside the circuit breaker.
     */
    public <T> Mono<T> read(Supplier<Mono<T>> call) {
        return withCircuitBreaker(Mono.defer(call)
                .timeout(properties.timeout())
                .retryWhen(retrySpec()));
    }

    /**
     * Many values. Collected to a list first, so a retry re-runs the whole query instead of resuming a
     * half-emitted stream.
     */
    public <T> Flux<T> readMany(Supplier<Flux<T>> call) {
        return read(() -> call.get().collectList()).flatMapIterable(items -> items);
    }

    /**
     * One value, no retry (see class doc).
     */
    public <T> Mono<T> write(Supplier<Mono<T>> call) {
        return withCircuitBreaker(Mono.defer(call)
                .timeout(properties.timeout()));
    }

    /**
     * Like readMany, without retry.
     */
    public <T> Flux<T> writeMany(Supplier<Flux<T>> call) {
        return write(() -> call.get().collectList()).flatMapIterable(items -> items);
    }

    /**
     * Applies the breaker and turns its failures into the domain ServiceUnavailableException.
     */
    private <T> Mono<T> withCircuitBreaker(Mono<T> call) {
        return call.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(CallNotPermittedException.class,
                        error -> new ServiceUnavailableException("Database circuit breaker is open", error))
                .onErrorMap(PersistenceFailures::isInfrastructureFailure,
                        error -> new ServiceUnavailableException("Database is unavailable", error));
    }

    // maxAttempts counts the first call too, so retries = maxAttempts - 1.
    private Retry retrySpec() {
        return Retry.backoff(properties.retry().maxAttempts() - 1L, properties.retry().backoff())
                .filter(PersistenceFailures::isInfrastructureFailure)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }
}
