package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import co.com.bancolombia.api.dto.request.CreateFranchiseRequest;
import co.com.bancolombia.api.dto.request.PatchNameRequest;
import co.com.bancolombia.api.dto.response.BranchTopProductResponse;
import co.com.bancolombia.api.dto.response.FranchiseResponse;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.gettopstockproductperbranch.GetTopStockProductPerBranchUseCase;
import co.com.bancolombia.usecase.patchfranchisename.PatchFranchiseNameUseCase;
import org.springframework.web.reactive.function.server.ServerRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Franchise endpoints.
 * Every chain ends in onErrorResume(errorMapper::toResponse): errors become HTTP responses in one place.
 */
@Component
@RequiredArgsConstructor
public class FranchiseHandler {
    private final CreateFranchiseUseCase createUseCase;
    private final PatchFranchiseNameUseCase patchFranchiseNameUseCase;
    private final GetTopStockProductPerBranchUseCase getTopStockProductPerBranchUseCase;
    private final ErrorMapper errorMapper;

    /**
     * POST: validates the body, then the path id, creates, and answers 201 with a Location header
     * pointing to the new resource.
     */
    public Mono<ServerResponse> create(ServerRequest request) {
        return RequestParams.body(request, CreateFranchiseRequest.class)
                .flatMap(body -> this.createUseCase.execute(body.name()))
                .map(FranchiseResponse::from)
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
                .flatMap(body -> RequestParams.franchiseId(request)
                        .flatMap(franchiseId -> this.patchFranchiseNameUseCase
                                .execute(franchiseId, body.name())))
                .map(FranchiseResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }

    /**
     * GET: one item per branch, collected into a single JSON array.
     */
    public Mono<ServerResponse> getTopStockProducts(ServerRequest request) {
        return RequestParams.franchiseId(request)
                .flatMapMany(this.getTopStockProductPerBranchUseCase::execute)
                .map(BranchTopProductResponse::from)
                .collectList()
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }
}
