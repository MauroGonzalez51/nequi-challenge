package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.com.bancolombia.api.dto.request.AddBranchRequest;
import co.com.bancolombia.api.dto.response.BranchResponse;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.api.exception.InvalidRequestException;
import co.com.bancolombia.usecase.addbranch.AddBranchUseCase;
import org.springframework.web.reactive.function.server.ServerRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchHandler {
    private final AddBranchUseCase addBranchUseCase;
    private final ErrorMapper errorMapper;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(AddBranchRequest.class)
                .switchIfEmpty(
                        Mono.error(() -> new InvalidRequestException(
                                "Request body is required")))
                .flatMap(body -> Mono
                        .fromCallable(() -> Long.valueOf(request.pathVariable("franchiseId")))
                        .onErrorMap(NumberFormatException.class,
                                ignored -> new InvalidRequestException(
                                        "Franchise id must be a number"))
                        .flatMap(franchiseId -> this.addBranchUseCase.execute(franchiseId,
                                body.name())))
                .map(BranchResponse::from)
                .flatMap(response -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }
}
