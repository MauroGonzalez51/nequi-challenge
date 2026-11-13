package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import co.com.bancolombia.api.dto.request.CreateFranchiseRequest;
import co.com.bancolombia.api.dto.request.PatchNameRequest;
import co.com.bancolombia.api.dto.response.FranchiseResponse;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.patchfranchisename.PatchFranchiseNameUseCase;
import org.springframework.web.reactive.function.server.ServerRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseHandler {
    private final CreateFranchiseUseCase createUseCase;
    private final PatchFranchiseNameUseCase patchFranchiseNameUseCase;
    private final ErrorMapper errorMapper;

    public Mono<ServerResponse> create(ServerRequest request) {
        return RequestParams.body(request, CreateFranchiseRequest.class)
                .flatMap(body -> this.createUseCase.execute(body.name()))
                .map(FranchiseResponse::from)
                .flatMap(response -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }

    public Mono<ServerResponse> patchName(ServerRequest request) {
        return RequestParams.body(request, PatchNameRequest.class)
                .flatMap(body -> RequestParams.franchiseId(request)
                        .flatMap(franchiseId -> this.patchFranchiseNameUseCase
                                .execute(franchiseId, body.name())))
                .map(FranchiseResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }
}
