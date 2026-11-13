package co.com.bancolombia.usecase.gettopstockproductperbranch;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.branch.gateways.BranchRepository;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductPerBranchUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetTopStockProductPerBranchUseCase useCase;

    private final Franchise franchise = Franchise.builder().id(1L).name("Nequi").build();

    @Test
    void shouldReturnEachBranchWithItsTopProductInOrder() {
        Branch cartagena = Branch.builder().id(1L).name("Cartagena").franchiseId(1L).build();
        Branch medellin = Branch.builder().id(2L).name("Medellin").franchiseId(1L).build();
        Product top = Product.builder().id(7L).name("Arepa").stock(20).branchId(1L).build();

        when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
        when(branchRepository.findByFranchiseId(1L)).thenReturn(Flux.just(cartagena, medellin));
        when(productRepository.findTopByBranchId(1L)).thenReturn(Mono.just(top));
        when(productRepository.findTopByBranchId(2L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(1L))
                .expectNextMatches(result -> result.getBranch() == cartagena && result.getProduct() == top)
                .expectNextMatches(result -> result.getBranch() == medellin && result.getProduct() == null)
                .verifyComplete();
    }

    @Test
    void shouldReturnNothingWhenFranchiseHasNoBranches() {
        when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
        when(branchRepository.findByFranchiseId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute(1L))
                .verifyComplete();

        verifyNoInteractions(productRepository);
    }

    @Test
    void shouldFailWhenFranchiseIdIsNull() {
        StepVerifier.create(useCase.execute(null))
                .expectError(ValidationException.class)
                .verify();

        verifyNoInteractions(franchiseRepository, branchRepository, productRepository);
    }

    @Test
    void shouldFailWhenFranchiseDoesNotExist() {
        when(franchiseRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(99L))
                .expectError(RecordNotFoundException.class)
                .verify();

        verifyNoInteractions(branchRepository, productRepository);
    }
}
