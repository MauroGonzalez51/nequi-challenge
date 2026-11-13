package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import co.com.bancolombia.api.dto.request.CreateFranchiseRequest;
import co.com.bancolombia.api.dto.response.CreateFranchiseResponse;
import co.com.bancolombia.api.dto.response.ErrorResponse;
import co.com.bancolombia.api.exception.InvalidRequestException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
import org.springframework.web.reactive.function.server.ServerRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseHandler {
    private final CreateFranchiseUseCase createUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CreateFranchiseRequest.class)
                .switchIfEmpty(Mono
                        .error(() -> new InvalidRequestException("Request body is required")))
                .flatMap(body -> this.createUseCase.execute(body.name()))
                .map(CreateFranchiseResponse::from)
                .flatMap(response -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(ValidationException.class,
                        exception -> ServerResponse.badRequest()
                                .bodyValue(new ErrorResponse(exception.getMessage())))
                .onErrorResume(InvalidRequestException.class,
                        exception -> ServerResponse.badRequest()
                                .bodyValue(new ErrorResponse(exception.getMessage())))
                .onErrorResume(ServerWebInputException.class,
                        ignored -> ServerResponse.badRequest()
                                .bodyValue(new ErrorResponse("Invalid Request Body")));

    }
}
