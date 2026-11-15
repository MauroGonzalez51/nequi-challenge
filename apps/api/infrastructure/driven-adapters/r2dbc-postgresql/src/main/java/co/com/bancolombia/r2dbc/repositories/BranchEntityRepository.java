package co.com.bancolombia.r2dbc.repositories;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import co.com.bancolombia.r2dbc.entities.BranchEntity;
import reactor.core.publisher.Flux;

public interface BranchEntityRepository
        extends ReactiveCrudRepository<BranchEntity, Long>, ReactiveQueryByExampleExecutor<BranchEntity> {

    Flux<BranchEntity> findByFranchiseIdOrderByIdAsc(Long franchiseId);
}
