package co.com.bancolombia.model.exception;

/**
 * The write clashes with the current state of the data, for example a parent record removed between the
 * check and the insert. The caller can look at the state again and decide; it is not a server failure.
 */
public class DataConflictException extends RuntimeException {
    public DataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
