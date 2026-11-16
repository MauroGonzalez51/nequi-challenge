package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.com.bancolombia.api.dto.request.AddProductRequest;
import co.com.bancolombia.api.dto.request.PatchNameRequest;
import co.com.bancolombia.api.dto.request.PatchProductStockRequest;
import co.com.bancolombia.api.dto.response.ProductResponse;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.usecase.addproduct.AddProductUseCase;
import co.com.bancolombia.usecase.patchproductname.PatchProductNameUseCase;
import co.com.bancolombia.usecase.patchproductstock.PatchProductStockUseCase;
import co.com.bancolombia.usecase.removeproduct.RemoveProductUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Product endpoints.
 * Every chain ends in onErrorResume(errorMapper::toResponse): errors become HTTP responses in one place.
 */
@Component
@RequiredArgsConstructor
public class ProductHandler {
    private final AddProductUseCase addProductUseCase;
    private final RemoveProductUseCase removeProductUseCase;
    private final PatchProductStockUseCase patchProductStockUseCase;
    private final PatchProductNameUseCase patchProductNameUseCase;
    private final ErrorMapper errorMapper;

    /**
     * POST: validates the body, then the path id, creates, and answers 201 with a Location header
     * pointing to the new resource.
     */
    public Mono<ServerResponse> create(ServerRequest request) {
        return RequestParams.body(request, AddProductRequest.class)
                .flatMap(body -> RequestParams.branchId(request)
                        .flatMap(branchId -> this.addProductUseCase.execute(branchId,
                                body.name(), body.stock())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }

    /**
     * DELETE: both ids are read together (Mono.zip). The use case returns Mono<Void>, so then() supplies the 204.
     */
    public Mono<ServerResponse> delete(ServerRequest request) {
        return Mono.zip(RequestParams.branchId(request),
                RequestParams.productId(request))
                .flatMap(ids -> this.removeProductUseCase.execute(ids.getT1(), ids.getT2()))
                .then(ServerResponse.noContent().build())
                .onErrorResume(this.errorMapper::toResponse);
    }

    /**
     * PATCH: body is checked before the path id. Answers 200 with the stored resource.
     */
    public Mono<ServerResponse> patchStock(ServerRequest request) {
        return RequestParams.body(request, PatchProductStockRequest.class)
                .flatMap(body -> RequestParams.productId(request)
                        .flatMap(productId -> this.patchProductStockUseCase.execute(productId, body.stock())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }

    /**
     * PATCH: body is checked before the path id. Answers 200 with the stored resource.
     */
    public Mono<ServerResponse> patchName(ServerRequest request) {
        return RequestParams.body(request, PatchNameRequest.class)
                .flatMap(body -> RequestParams.productId(request)
                        .flatMap(productId -> this.patchProductNameUseCase.execute(productId, body.name())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }
}
