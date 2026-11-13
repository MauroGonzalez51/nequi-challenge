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

    public Mono<Product> execute(Long id, String name) {
        return Mono.justOrEmpty(name)
                .switchIfEmpty(Mono.error(() -> new ValidationException("Product name cannot be null")))
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Product name must not be blank")))
                .then(
                        Mono.justOrEmpty(id)
                                .switchIfEmpty(Mono.error(() -> new ValidationException("Product id cannot be null")))
                                .flatMap(this.repository::findById)
                                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Product not found"))))
                .flatMap(existing -> Mono.just(existing)
                        .filter(product -> !name.equals(product.getName()))
                        .map(product -> product.toBuilder().name(name).build())
                        .flatMap(this.repository::save)
                        .switchIfEmpty(Mono.just(existing)));
    }
}
