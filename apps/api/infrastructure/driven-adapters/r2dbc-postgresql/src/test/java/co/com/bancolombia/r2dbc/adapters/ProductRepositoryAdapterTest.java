package co.com.bancolombia.r2dbc.adapters;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;

import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.r2dbc.entities.ProductEntity;
import co.com.bancolombia.r2dbc.repositories.ProductEntityRepository;
import co.com.bancolombia.r2dbc.helper.TestResilience;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryAdapterTest {

    @Mock
    private ProductEntityRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Test
    void shouldMapTheTopProductOfTheBranchToDomain() {
        ProductEntity entity = new ProductEntity(7L, "Arepa", 20, 1L);
        Product product = Product.builder().id(7L).name("Arepa").stock(20).branchId(1L).build();

        when(repository.findFirstByBranchIdOrderByStockDescIdAsc(1L)).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Product.class)).thenReturn(product);

        ProductRepositoryAdapter adapter = new ProductRepositoryAdapter(repository, mapper, TestResilience.create());

        StepVerifier.create(adapter.findTopByBranchId(1L))
                .expectNext(product)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenTheBranchHasNoProducts() {
        when(repository.findFirstByBranchIdOrderByStockDescIdAsc(2L)).thenReturn(Mono.empty());

        ProductRepositoryAdapter adapter = new ProductRepositoryAdapter(repository, mapper, TestResilience.create());

        StepVerifier.create(adapter.findTopByBranchId(2L))
                .verifyComplete();
    }
}
