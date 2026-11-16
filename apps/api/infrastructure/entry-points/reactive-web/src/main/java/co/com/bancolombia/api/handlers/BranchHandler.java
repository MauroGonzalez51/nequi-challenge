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

/**
 * Branch endpoints.
 * Every chain ends in onErrorResume(errorMapper::toResponse): errors become HTTP responses in one place.
 */
@Component
@RequiredArgsConstructor
public class BranchHandler {
    private final AddBranchUseCase addBranchUseCase;
    private final PatchBranchNameUseCase patchBranchNameUseCase;
    private final ErrorMapper errorMapper;

    /**
     * POST: validates the body, then the path id, creates, and answers 201 with a Location header
     * pointing to the new resource.
     */
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

    /**
     * PATCH: body is checked before the path id. Answers 200 with the stored resource.
     */
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
