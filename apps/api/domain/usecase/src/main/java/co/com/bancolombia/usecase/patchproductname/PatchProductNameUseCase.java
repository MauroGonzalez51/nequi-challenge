package co.com.bancolombia.usecase.patchproductname;

import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class PatchProductNameUseCase {
    private final ProductRepository repository;

    /**
     * Renames a product. Validates the new name (null, then blank) and the id,
     * loads the product, and saves only
     * when the name really changes. Same name: returns the stored product
     * untouched, skipping a useless UPDATE.
     */
    public Mono<Product> execute(Long id, String name) {
        return Mono.justOrEmpty(name)
                .switchIfEmpty(Mono.error(() -> new ValidationException("Product name cannot be null")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Product name must not be blank")))
                .flatMap(trimmed -> Mono.justOrEmpty(id)
                        .switchIfEmpty(Mono.error(() -> new ValidationException("Product id cannot be null")))
                        .flatMap(this.repository::findById)
                        .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Product not found")))
                        // filter() turns "same name" into an empty Mono, so save() is skipped and
                        // switchIfEmpty returns the stored entity.
                        .flatMap(existing -> Mono.just(existing)
                                .filter(product -> !trimmed.equals(product.getName()))
                                .map(product -> product.toBuilder().name(trimmed).build())
                                .flatMap(this.repository::save)
                                .switchIfEmpty(Mono.just(existing))));
    }
}
