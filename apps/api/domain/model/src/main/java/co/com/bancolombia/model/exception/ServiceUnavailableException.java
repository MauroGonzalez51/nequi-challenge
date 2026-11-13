package co.com.bancolombia.model.exception;

/**
 * A dependency the service needs (the database) is not usable right now:
 * unreachable, too slow, or cut off by an open circuit breaker.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
