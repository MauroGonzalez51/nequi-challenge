package co.com.bancolombia.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;

import co.com.bancolombia.api.dto.response.ErrorResponse;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ServiceUnavailableException;
import co.com.bancolombia.model.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Single place that translates any error of a handler flow into an HTTP
 * response. Handlers end their chain with
 * {@code .onErrorResume(errorMapper::toResponse)}.
 */
@Slf4j
@Component
public class ErrorMapper {

    public Mono<ServerResponse> toResponse(Throwable error) {
        return Mono.<ServerResponse>error(error)
                .onErrorResume(RecordNotFoundException.class,
                        exception -> respond(HttpStatus.NOT_FOUND, exception.getMessage()))
                .onErrorResume(ValidationException.class,
                        exception -> respond(HttpStatus.BAD_REQUEST, exception.getMessage()))
                .onErrorResume(InvalidRequestException.class,
                        exception -> respond(HttpStatus.BAD_REQUEST, exception.getMessage()))
                .onErrorResume(ServerWebInputException.class,
                        ignored -> respond(HttpStatus.BAD_REQUEST, "Invalid Request Body"))
                .onErrorResume(ServiceUnavailableException.class, exception -> {
                    // The cause goes as text: a Throwable as last argument would print a full stack trace per 503.
                    log.warn("Service unavailable: {} (cause: {})", exception.getMessage(),
                            String.valueOf(exception.getCause()));
                    return respond(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
                })
                .onErrorResume(exception -> {
                    log.error("Unexpected error", exception);
                    return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
                });
    }

    private Mono<ServerResponse> respond(HttpStatus status, String message) {
        return ServerResponse.status(status).bodyValue(new ErrorResponse(message));
    }
}
