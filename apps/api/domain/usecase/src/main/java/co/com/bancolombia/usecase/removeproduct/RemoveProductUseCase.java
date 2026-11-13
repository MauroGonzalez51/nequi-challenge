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
                .filter(product -> branchId.equals(product.getBranchId()))
                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Product not found")))
                .flatMap(product -> this.productRepository.deleteById(product.getId()));
    }
}
