package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;

import co.com.bancolombia.api.dto.request.AddBranchRequest;
import co.com.bancolombia.api.dto.response.AddBranchResponse;
import co.com.bancolombia.api.dto.response.ErrorResponse;
import co.com.bancolombia.api.exception.InvalidRequestException;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.usecase.addbranch.AddBranchUseCase;
import org.springframework.web.reactive.function.server.ServerRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchHandler {
    private final AddBranchUseCase addBranchUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(AddBranchRequest.class)
                .switchIfEmpty(
                        Mono.error(() -> new InvalidRequestException("Request body is required")))
                .flatMap(body -> Mono.fromCallable(() -> Long.valueOf(request.pathVariable("franchiseId")))
                        .onErrorMap(NumberFormatException.class,
                                ignored -> new InvalidRequestException("Franchise id must be a number"))
                        .flatMap(franchiseId -> this.addBranchUseCase.execute(franchiseId, body.name())))
                .map(AddBranchResponse::from)
                .flatMap(response -> ServerResponse.created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(RecordNotFoundException.class,
                        exception -> ServerResponse.unprocessableContent()
                                .bodyValue(new ErrorResponse(exception.getMessage())))
                .onErrorResume(ValidationException.class,
                        exception -> ServerResponse.badRequest().bodyValue(new ErrorResponse(exception.getMessage())))
                .onErrorResume(InvalidRequestException.class,
                        exception -> ServerResponse.badRequest()
                                .bodyValue(new ErrorResponse(exception.getMessage())))
                .onErrorResume(ServerWebInputException.class,
                        ignored -> ServerResponse.badRequest()
                                .bodyValue(new ErrorResponse("Invalid Request Body")));
    }
}
