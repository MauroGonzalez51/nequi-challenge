package co.com.bancolombia.api.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.com.bancolombia.api.dto.request.AddProductRequest;
import co.com.bancolombia.api.dto.response.ProductResponse;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.api.exception.InvalidRequestException;
import co.com.bancolombia.usecase.addproduct.AddProductUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductHandler {
    private final AddProductUseCase addProductUseCase;
    private final ErrorMapper errorMapper;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(AddProductRequest.class)
                .switchIfEmpty(Mono.error(() -> new InvalidRequestException("Request body is required")))
                .flatMap(body -> Mono
                        .fromCallable(() -> Long.valueOf(request.pathVariable("branchId")))
                        .onErrorMap(NumberFormatException.class,
                                ignored -> new InvalidRequestException("Branch id must be a number"))
                        .flatMap(branchId -> this.addProductUseCase.execute(branchId, body.name(), body.stock())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse
                        .created(request.uriBuilder().path("/{id}").build(response.id()))
                        .bodyValue(response))
                .onErrorResume(this.errorMapper::toResponse);
    }
}
