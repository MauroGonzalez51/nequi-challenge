package co.com.bancolombia.r2dbc.repositories;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import co.com.bancolombia.r2dbc.entities.FranchiseEntity;

public interface FranchiseEntityRepository
                extends ReactiveCrudRepository<FranchiseEntity, Long>, ReactiveQueryByExampleExecutor<FranchiseEntity> {
}
