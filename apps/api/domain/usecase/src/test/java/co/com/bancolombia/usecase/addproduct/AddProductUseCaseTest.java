package co.com.bancolombia.usecase.addproduct;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AddProductUseCaseTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AddProductUseCase useCase;

    @Test
    void shouldSaveProductWhenInputIsValid() {
        Branch branch = Branch.builder().id(1L).name("Cartagena").franchiseId(1L).build();
        Product saved = Product.builder().id(10L).name("Arepa").stock(5).branchId(1L).build();

        when(branchRepository.findById(1L)).thenReturn(Mono.just(branch));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(useCase.execute(1L, "Arepa", 5))
                .expectNext(saved)
                .verifyComplete();
    }

    @Test
    void shouldSaveProductWhenStockIsZero() {
        Branch branch = Branch.builder().id(1L).name("Cartagena").franchiseId(1L).build();
        Product saved = Product.builder().id(10L).name("Arepa").stock(0).branchId(1L).build();

        when(branchRepository.findById(1L)).thenReturn(Mono.just(branch));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(useCase.execute(1L, "Arepa", 0))
                .expectNext(saved)
                .verifyComplete();
    }

    @Test
    void shouldTrimNameBeforeSaving() {
        Branch branch = Branch.builder().id(1L).name("Cartagena").franchiseId(1L).build();

        when(branchRepository.findById(1L)).thenReturn(Mono.just(branch));
        when(productRepository.save(any(Product.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(useCase.execute(1L, "  Arepa  ", 5))
                .expectNextMatches(saved -> saved.getName().equals("Arepa"))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        StepVerifier.create(useCase.execute(1L, "   ", 5))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenNameIsNull() {
        StepVerifier.create(useCase.execute(1L, null, 5))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenStockIsNull() {
        StepVerifier.create(useCase.execute(1L, "Arepa", null))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenStockIsNegative() {
        StepVerifier.create(useCase.execute(1L, "Arepa", -1))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenBranchIdIsNull() {
        StepVerifier.create(useCase.execute(null, "Arepa", 5))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenBranchDoesNotExist() {
        when(branchRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(99L, "Arepa", 5))
                .expectError(RecordNotFoundException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }
}
