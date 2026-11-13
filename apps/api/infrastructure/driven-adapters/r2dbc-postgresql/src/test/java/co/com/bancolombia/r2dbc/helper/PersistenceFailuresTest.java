package co.com.bancolombia.r2dbc.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;

class PersistenceFailuresTest {

    @Test
    void shouldTreatConnectionFailuresAsInfrastructureFailures() {
        assertThat(PersistenceFailures.isInfrastructureFailure(new DataAccessResourceFailureException("down")))
                .isTrue();
    }

    @Test
    void shouldTreatTransientDataAccessErrorsAsInfrastructureFailures() {
        assertThat(PersistenceFailures.isInfrastructureFailure(new QueryTimeoutException("slow"))).isTrue();
    }

    @Test
    void shouldTreatTimeoutsAsInfrastructureFailures() {
        assertThat(PersistenceFailures.isInfrastructureFailure(new TimeoutException())).isTrue();
    }

    @Test
    void shouldNotTreatIntegrityErrorsAsInfrastructureFailures() {
        assertThat(PersistenceFailures.isInfrastructureFailure(new DataIntegrityViolationException("dup")))
                .isFalse();
    }

    @Test
    void shouldNotTreatUnrelatedErrorsAsInfrastructureFailures() {
        assertThat(PersistenceFailures.isInfrastructureFailure(new IllegalStateException())).isFalse();
    }
}
