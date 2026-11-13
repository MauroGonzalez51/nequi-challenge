package co.com.bancolombia.r2dbc.helper;

import java.util.concurrent.TimeoutException;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;

/**
 * Single place that says which errors mean "the database is unhealthy" (and so
 * may be retried and count against the circuit breaker). Anything else, like a
 * duplicate key or a bad foreign key, means the database answered: not retried
 * and not a breaker failure.
 */
public final class PersistenceFailures {

    private PersistenceFailures() {
    }

    public static boolean isInfrastructureFailure(Throwable error) {
        return error instanceof DataAccessResourceFailureException
                || error instanceof TransientDataAccessException
                || error instanceof TimeoutException;
    }
}
