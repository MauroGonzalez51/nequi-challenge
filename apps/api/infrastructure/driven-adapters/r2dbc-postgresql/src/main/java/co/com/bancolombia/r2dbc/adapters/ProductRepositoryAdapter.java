package co.com.bancolombia.r2dbc.adapters;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import co.com.bancolombia.r2dbc.entities.ProductEntity;
import co.com.bancolombia.r2dbc.helper.PersistenceResilience;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.repositories.ProductEntityRepository;
import reactor.core.publisher.Mono;

@Repository
public class ProductRepositoryAdapter
        extends ReactiveAdapterOperations<Product, ProductEntity, Long, ProductEntityRepository>
        implements ProductRepository {

    public ProductRepositoryAdapter(ProductEntityRepository repository, ObjectMapper mapper,
            PersistenceResilience resilience) {
        super(repository, mapper, data -> mapper.map(data, Product.class), resilience);
    }

    @Override
    /**
     * Highest stock first; ties go to the lowest id, so the answer is always the same.
     */
    public Mono<Product> findTopByBranchId(Long branchId) {
        return this.resilience.read(() -> this.repository.findFirstByBranchIdOrderByStockDescIdAsc(branchId))
                .map(this::toEntity);
    }

    @Override
    /**
     * A write: timeout and breaker, never retried.
     */
    public Mono<Void> deleteById(Long id) {
        return this.resilience.write(() -> this.repository.deleteById(id));
    }
}
