package co.com.bancolombia.r2dbc.adapters;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;

import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.r2dbc.entities.BranchEntity;
import co.com.bancolombia.r2dbc.repositories.BranchEntityRepository;
import co.com.bancolombia.r2dbc.helper.TestResilience;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BranchRepositoryAdapterTest {

    @Mock
    private BranchEntityRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Test
    void shouldMapTheBranchesOfTheFranchiseToDomain() {
        BranchEntity entity = new BranchEntity(1L, "Cartagena", 1L);
        Branch branch = Branch.builder().id(1L).name("Cartagena").franchiseId(1L).build();

        when(repository.findByFranchiseIdOrderByIdAsc(1L)).thenReturn(Flux.just(entity));
        when(mapper.map(entity, Branch.class)).thenReturn(branch);

        BranchRepositoryAdapter adapter = new BranchRepositoryAdapter(repository, mapper, TestResilience.create());

        StepVerifier.create(adapter.findByFranchiseId(1L))
                .expectNext(branch)
                .verifyComplete();
    }
}
