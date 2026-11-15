package co.com.bancolombia.r2dbc.adapters;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;

import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.r2dbc.entities.FranchiseEntity;
import co.com.bancolombia.r2dbc.helper.TestResilience;
import co.com.bancolombia.r2dbc.repositories.FranchiseEntityRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FranchiseRepositoryAdapterTest {

    @Mock
    private FranchiseEntityRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Test
    void shouldMapTheFoundEntityToDomain() {
        FranchiseEntity entity = new FranchiseEntity(1L, "Nequi");
        Franchise franchise = Franchise.builder().id(1L).name("Nequi").build();

        when(repository.findById(1L)).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Franchise.class)).thenReturn(franchise);

        FranchiseRepositoryAdapter adapter = new FranchiseRepositoryAdapter(repository, mapper,
                TestResilience.create());

        StepVerifier.create(adapter.findById(1L))
                .expectNext(franchise)
                .verifyComplete();
    }
}
