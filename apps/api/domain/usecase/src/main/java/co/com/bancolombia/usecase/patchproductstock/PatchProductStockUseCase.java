package co.com.bancolombia.usecase.patchproductstock;

import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class PatchProductStockUseCase {
    private final ProductRepository repository;

    public Mono<Product> execute(Long productId, Integer stock) {
        return Mono.justOrEmpty(stock)
                .switchIfEmpty(
                        Mono.error(() -> new ValidationException("Product stock cannot be null")))
                .filter(value -> value >= 0)
                .switchIfEmpty(Mono.error(() -> new ValidationException(
                        "Product stock must be greater than or equal to 0")))
                .then(
                        Mono.justOrEmpty(productId)
                                .switchIfEmpty(Mono.error(() -> new ValidationException("Product id cannot be null")))
                                .flatMap(this.repository::findById)
                                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Product not found"))))
                .flatMap(existing -> Mono.just(existing)
                        .filter(product -> !stock.equals(product.getStock()))
                        .map(product -> product.toBuilder().stock(stock).build())
                        .flatMap(this.repository::save)
                        .switchIfEmpty(Mono.just(existing)));
    }
}
