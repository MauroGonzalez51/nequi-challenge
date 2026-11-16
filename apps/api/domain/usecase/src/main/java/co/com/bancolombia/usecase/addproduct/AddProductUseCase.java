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
     * Checks in order, first failure wins: blank name, stock below 0, null branch id (all 400), then
     * branch not found (404). The product is built from the arguments plus the stored branch id.
     */
    public Mono<Product> execute(Long branchId, String name, Integer stock) {
        return Mono.justOrEmpty(name).filter(value -> !value.isBlank())
                .switchIfEmpty(Mono
                        .error(() -> new ValidationException("Product name must not be blank")))
                // Each .then() chains the next check after the previous one passed (see AddBranchUseCase).
                .then(Mono.justOrEmpty(stock).filter(value -> value >= 0).switchIfEmpty(
                        Mono.error(() -> new ValidationException(
                                "Product stock must be greater than or equal to 0"))))
                .then(
                        Mono.justOrEmpty(branchId)
                                .switchIfEmpty(Mono.error(() -> new ValidationException(
                                        "Branch id cannot be null"))))
                .flatMap(this.branchRepository::findById)
                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Branch not found")))
                .map(branch -> Product.builder().name(name).stock(stock).branchId(branch.getId())
                        .build())
                .flatMap(this.productRepository::save);
    }
}
