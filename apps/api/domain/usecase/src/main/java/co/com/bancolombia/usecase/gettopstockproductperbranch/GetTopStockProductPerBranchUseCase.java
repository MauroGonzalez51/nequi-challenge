package co.com.bancolombia.usecase.gettopstockproductperbranch;

import co.com.bancolombia.model.branch.BranchTopProduct;
import co.com.bancolombia.model.branch.gateways.BranchRepository;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetTopStockProductPerBranchUseCase {
    private final FranchiseRepository franchiseRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    /**
     * Top-stock product of every branch of a franchise, in branch id order.
     * A branch without products still appears, with a null product.
     */
    public Flux<BranchTopProduct> execute(Long franchiseId) {
        return Mono.justOrEmpty(franchiseId)
                .switchIfEmpty(Mono.error(() -> new ValidationException("Franchise id cannot be null")))
                .flatMap(this.franchiseRepository::findById)
                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Franchise not found")))
                .flatMapMany(franchise -> this.branchRepository.findByFranchiseId(franchise.getId()))
                // concatMap, not flatMap: one branch at a time keeps the branch order and never has more than one query in flight.
                .concatMap(branch -> this.productRepository.findTopByBranchId(branch.getId())
                        .map(product -> new BranchTopProduct(branch, product))
                        .defaultIfEmpty(new BranchTopProduct(branch, null)));
    }
}
