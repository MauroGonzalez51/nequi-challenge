package co.com.bancolombia.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebInputException;

import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ServiceUnavailableException;
import co.com.bancolombia.model.exception.ValidationException;
import reactor.test.StepVerifier;

class ErrorMapperTest {

    private final ErrorMapper errorMapper = new ErrorMapper();

    @Test
    void shouldMapRecordNotFoundToNotFound() {
        StepVerifier.create(errorMapper.toResponse(new RecordNotFoundException("Franchise not found")))
                .expectNextMatches(response -> response.statusCode().equals(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void shouldMapValidationToBadRequest() {
        StepVerifier.create(errorMapper.toResponse(new ValidationException("bad")))
                .expectNextMatches(response -> response.statusCode().equals(HttpStatus.BAD_REQUEST))
                .verifyComplete();
    }

    @Test
    void shouldMapInvalidRequestToBadRequest() {
        StepVerifier.create(errorMapper.toResponse(new InvalidRequestException("bad")))
                .expectNextMatches(response -> response.statusCode().equals(HttpStatus.BAD_REQUEST))
                .verifyComplete();
    }

    @Test
    void shouldMapServerWebInputToBadRequest() {
        StepVerifier.create(errorMapper.toResponse(new ServerWebInputException("bad")))
                .expectNextMatches(response -> response.statusCode().equals(HttpStatus.BAD_REQUEST))
                .verifyComplete();
    }

    @Test
    void shouldMapServiceUnavailableToServiceUnavailable() {
        StepVerifier.create(errorMapper.toResponse(
                new ServiceUnavailableException("Database is unavailable", new IllegalStateException("boom"))))
                .expectNextMatches(response -> response.statusCode().equals(HttpStatus.SERVICE_UNAVAILABLE))
                .verifyComplete();
    }

    @Test
    void shouldMapUnknownErrorToInternalServerError() {
        StepVerifier.create(errorMapper.toResponse(new IllegalStateException("boom")))
                .expectNextMatches(
                        response -> response.statusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR))
                .verifyComplete();
    }
}
