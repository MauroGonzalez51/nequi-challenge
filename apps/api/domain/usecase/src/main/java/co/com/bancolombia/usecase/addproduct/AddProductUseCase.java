package co.com.bancolombia.usecase.addproduct;

import co.com.bancolombia.model.branch.gateways.BranchRepository;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AddProductUseCase {
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    /**
     * Creates a product under an existing branch.
     * Checks in order, first failure wins: null or blank name, null or negative
     * stock, null branch id (all 400), then branch not found (404). The product is
     * built from the trimmed name, the validated stock and the stored branch id.
     */
    public Mono<Product> execute(Long branchId, String name, Integer stock) {
        return Mono.justOrEmpty(name)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Product name must not be blank")))
                .map(String::trim)
                // Nested flatMaps keep trimmed and validStock in scope for the final builder
                // each level runs only if the previous check passed.
                .flatMap(trimmed -> Mono.justOrEmpty(stock)
                        .filter(value -> value >= 0)
                        .switchIfEmpty(
                                Mono.error(() -> new ValidationException(
                                        "Product stock must be greater than or equal to 0")))
                        .flatMap(validStock -> Mono.justOrEmpty(branchId)
                                .switchIfEmpty(Mono.error(() -> new ValidationException("Branch id cannot be null")))
                                .flatMap(this.branchRepository::findById)
                                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Branch not found")))
                                .map(branch -> Product.builder().name(trimmed).stock(validStock)
                                        .branchId(branch.getId()).build())
                                .flatMap(this.productRepository::save)));
    }
}
