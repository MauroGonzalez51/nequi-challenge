package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.com.bancolombia.api.dto.request.AddProductRequest;
import co.com.bancolombia.api.dto.request.PatchProductStockRequest;
import co.com.bancolombia.api.dto.response.ProductResponse;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.api.exception.InvalidRequestException;
import co.com.bancolombia.usecase.addproduct.AddProductUseCase;
import co.com.bancolombia.usecase.patchproductstock.PatchProductStockUseCase;
import co.com.bancolombia.usecase.removeproduct.RemoveProductUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductHandler {
    private final AddProductUseCase addProductUseCase;
    private final RemoveProductUseCase removeProductUseCase;
    private final PatchProductStockUseCase patchProductStockUseCase;

    private final ErrorMapper errorMapper;

    private Mono<Long> pathId(ServerRequest request, String name, String label) {
        return Mono.fromCallable(() -> Long.valueOf(request.pathVariable(name))).onErrorMap(
                NumberFormatException.class,
                ignored -> new InvalidRequestException(label + " must be a number"));
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(AddProductRequest.class)
                .switchIfEmpty(Mono
                        .error(() -> new InvalidRequestException("Request body is required")))
                .flatMap(body -> this.pathId(request, "branchId", "Branch id")
                        .flatMap(branchId -> this.addProductUseCase.execute(branchId,
                                body.name(), body.stock())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        return Mono.zip(this.pathId(request, "branchId", "Branch id"),
                this.pathId(request, "productId", "Product id"))
                .flatMap(ids -> this.removeProductUseCase.execute(ids.getT1(), ids.getT2()))
                .then(ServerResponse.noContent().build())
                .onErrorResume(this.errorMapper::toResponse);
    }

    public Mono<ServerResponse> patchStock(ServerRequest request) {
        return request.bodyToMono(PatchProductStockRequest.class)
                .switchIfEmpty(Mono.error(() -> new InvalidRequestException("Request body is required")))
                .flatMap(body -> this.pathId(request, "productId", "Product id")
                        .flatMap(productId -> this.patchProductStockUseCase.execute(productId, body.stock())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }
}
