package co.com.bancolombia.usecase.removeproduct;

import co.com.bancolombia.model.branch.gateways.BranchRepository;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RemoveProductUseCase {

    private final BranchRepository branchRepository;

    private final ProductRepository productRepository;

    /**
     * Deletes a product, only if it belongs to the given branch. A product of another branch gets the same
     * "Product not found" as a missing one, so ids that belong to other branches are not revealed.
     */
    public Mono<Void> execute(Long branchId, Long productId) {
        return Mono.justOrEmpty(branchId)
                .switchIfEmpty(Mono.error(() -> new ValidationException("Branch id cannot be null")))
                .flatMap(this.branchRepository::findById)
                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Branch not found")))
                .then(
                        Mono.justOrEmpty(productId)
                                .switchIfEmpty(Mono.error(() -> new ValidationException("Product id cannot be null"))))
                .flatMap(this.productRepository::findById)
                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Product not found")))
                // Ownership check: empty here (wrong branch) falls into the "Product not found" error below.
                .filter(product -> branchId.equals(product.getBranchId()))
                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Product not found")))
                .flatMap(product -> this.productRepository.deleteById(product.getId()));
    }
}
