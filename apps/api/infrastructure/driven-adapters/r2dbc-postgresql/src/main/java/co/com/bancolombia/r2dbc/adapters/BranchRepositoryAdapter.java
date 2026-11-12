package co.com.bancolombia.r2dbc.adapters;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.r2dbc.entities.BranchEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.repositories.BranchEntityRepository;

@Repository
public class BranchRepositoryAdapter
        extends ReactiveAdapterOperations<Branch, BranchEntity, Long, BranchEntityRepository> {

    public BranchRepositoryAdapter(BranchEntityRepository repository, ObjectMapper mapper) {
        super(repository, mapper, data -> mapper.map(data, Branch.class));
    }
}
