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

    /**
     * Sets the stock of a product. Validates the stock (null, then below 0) and the id, loads the product,
     * and saves only when the value really changes (same value: returns the stored product, no UPDATE).
     */
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
                // filter() turns "same stock" into an empty Mono, so save() is skipped and switchIfEmpty returns the stored entity.
                .flatMap(existing -> Mono.just(existing)
                        .filter(product -> !stock.equals(product.getStock()))
                        .map(product -> product.toBuilder().stock(stock).build())
                        .flatMap(this.repository::save)
                        .switchIfEmpty(Mono.just(existing)));
    }
}
