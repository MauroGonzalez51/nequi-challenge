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

    public Mono<Product> execute(Long branchId, String name, Integer stock) {
        return Mono.justOrEmpty(name).filter(value -> !value.isBlank())
                .switchIfEmpty(Mono
                        .error(() -> new ValidationException("Product name must not be blank")))
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
