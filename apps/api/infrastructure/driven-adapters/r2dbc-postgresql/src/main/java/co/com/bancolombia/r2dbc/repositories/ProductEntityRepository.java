package co.com.bancolombia.r2dbc.repositories;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import co.com.bancolombia.r2dbc.entities.ProductEntity;

public interface ProductEntityRepository
        extends ReactiveCrudRepository<ProductEntity, Long>, ReactiveQueryByExampleExecutor<ProductEntity> {

}
