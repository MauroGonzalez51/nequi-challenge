package co.com.bancolombia.r2dbc.adapters;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import co.com.bancolombia.r2dbc.entities.FranchiseEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.repositories.FranchiseEntityRepository;

@Repository
public class FranchiseRepositoryAdapter
        extends ReactiveAdapterOperations<Franchise, FranchiseEntity, Long, FranchiseEntityRepository>
        implements FranchiseRepository {

    public FranchiseRepositoryAdapter(FranchiseEntityRepository repository, ObjectMapper mapper) {
        super(repository, mapper, data -> mapper.map(data, Franchise.class));
    }
}
