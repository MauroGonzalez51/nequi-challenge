package co.com.bancolombia.r2dbc.adapters;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.branch.gateways.BranchRepository;
import co.com.bancolombia.r2dbc.entities.BranchEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.repositories.BranchEntityRepository;
import reactor.core.publisher.Flux;

@Repository
public class BranchRepositoryAdapter
        extends ReactiveAdapterOperations<Branch, BranchEntity, Long, BranchEntityRepository>
        implements BranchRepository {

    public BranchRepositoryAdapter(BranchEntityRepository repository, ObjectMapper mapper) {
        super(repository, mapper, data -> mapper.map(data, Branch.class));
    }

    @Override
    public Flux<Branch> findByFranchiseId(Long franchiseId) {
        return this.repository.findByFranchiseId(franchiseId).map(this::toEntity);
    }
}
