package co.com.bancolombia.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;

import co.com.bancolombia.api.dto.response.ErrorResponse;
import co.com.bancolombia.model.exception.DataConflictException;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ServiceUnavailableException;
import co.com.bancolombia.model.exception.ValidationException;
import reactor.test.StepVerifier;

class ErrorMapperTest {

    private final ErrorMapper errorMapper = new ErrorMapper();

    @Test
    void shouldMapRecordNotFoundToNotFound() {
        StepVerifier.create(errorMapper.toResponse(new RecordNotFoundException("Franchise not found")))
                .expectNextMatches(response -> hasError(response, HttpStatus.NOT_FOUND,
                        "Franchise not found", "RECORD_NOT_FOUND"))
                .verifyComplete();
    }

    @Test
    void shouldMapValidationToBadRequest() {
        StepVerifier.create(errorMapper.toResponse(new ValidationException("bad")))
                .expectNextMatches(response -> hasError(response, HttpStatus.BAD_REQUEST,
                        "bad", "VALIDATION_ERROR"))
                .verifyComplete();
    }

    @Test
    void shouldMapInvalidRequestToBadRequest() {
        StepVerifier.create(errorMapper.toResponse(new InvalidRequestException("bad")))
                .expectNextMatches(response -> hasError(response, HttpStatus.BAD_REQUEST,
                        "bad", "INVALID_REQUEST"))
                .verifyComplete();
    }

    @Test
    void shouldMapServerWebInputToBadRequest() {
        StepVerifier.create(errorMapper.toResponse(new ServerWebInputException("bad")))
                .expectNextMatches(response -> hasError(response, HttpStatus.BAD_REQUEST,
                        "Invalid Request Body", "INVALID_BODY"))
                .verifyComplete();
    }

    @Test
    void shouldMapDataConflictToConflict() {
        StepVerifier.create(errorMapper.toResponse(
                new DataConflictException("conflict", new IllegalStateException("fk"))))
                .expectNextMatches(response -> hasError(response, HttpStatus.CONFLICT,
                        "conflict", "DATA_CONFLICT"))
                .verifyComplete();
    }

    @Test
    void shouldMapServiceUnavailableToServiceUnavailable() {
        StepVerifier.create(errorMapper.toResponse(
                new ServiceUnavailableException("Database is unavailable", new IllegalStateException("boom"))))
                .expectNextMatches(response -> hasError(response, HttpStatus.SERVICE_UNAVAILABLE,
                        "Service temporarily unavailable", "SERVICE_UNAVAILABLE"))
                .verifyComplete();
    }

    @Test
    void shouldMapUnknownErrorToInternalServerError() {
        StepVerifier.create(errorMapper.toResponse(new IllegalStateException("boom")))
                .expectNextMatches(response -> hasError(response, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error", "INTERNAL_ERROR"))
                .verifyComplete();
    }

    /** The body is built with bodyValue, so the response is an EntityResponse holding the ErrorResponse. */
    private static boolean hasError(ServerResponse response, HttpStatus status, String message, String errorCode) {
        return response.statusCode().equals(status)
                && response instanceof EntityResponse<?> entity
                && entity.entity() instanceof ErrorResponse body
                && body.message().equals(message)
                && body.errorCode().equals(errorCode);
    }
}
