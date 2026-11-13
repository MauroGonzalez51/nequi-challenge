package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.com.bancolombia.api.dto.request.AddBranchRequest;
import co.com.bancolombia.api.dto.request.PatchNameRequest;
import co.com.bancolombia.api.dto.response.BranchResponse;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.usecase.addbranch.AddBranchUseCase;
import co.com.bancolombia.usecase.patchbranchname.PatchBranchNameUseCase;
import org.springframework.web.reactive.function.server.ServerRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchHandler {
    private final AddBranchUseCase addBranchUseCase;
    private final PatchBranchNameUseCase patchBranchNameUseCase;
    private final ErrorMapper errorMapper;

    public Mono<ServerResponse> create(ServerRequest request) {
        return RequestParams.body(request, AddBranchRequest.class)
                .flatMap(body -> RequestParams.franchiseId(request)
                        .flatMap(franchiseId -> this.addBranchUseCase.execute(franchiseId,
                                body.name())))
                .map(BranchResponse::from)
                .flatMap(response -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }

    public Mono<ServerResponse> patchName(ServerRequest request) {
        return RequestParams.body(request, PatchNameRequest.class)
                .flatMap(body -> RequestParams.branchId(request)
                        .flatMap(branchId -> this.patchBranchNameUseCase.execute(branchId,
                                body.name())))
                .map(BranchResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }
}
