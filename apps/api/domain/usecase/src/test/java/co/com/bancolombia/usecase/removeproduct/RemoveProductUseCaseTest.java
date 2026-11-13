package co.com.bancolombia.usecase.removeproduct;

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
class RemoveProductUseCaseTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RemoveProductUseCase useCase;

    private final Branch branch = Branch.builder().id(1L).name("Cartagena").franchiseId(1L).build();

    @Test
    void shouldDeleteProductWhenItBelongsToTheBranch() {
        Product product = Product.builder().id(10L).name("Arepa").stock(5).branchId(1L).build();

        when(branchRepository.findById(1L)).thenReturn(Mono.just(branch));
        when(productRepository.findById(10L)).thenReturn(Mono.just(product));
        when(productRepository.deleteById(10L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(1L, 10L))
                .verifyComplete();

        verify(productRepository).deleteById(10L);
    }

    @Test
    void shouldFailWhenBranchIdIsNull() {
        StepVerifier.create(useCase.execute(null, 10L))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void shouldFailWhenProductIdIsNull() {
        when(branchRepository.findById(1L)).thenReturn(Mono.just(branch));

        StepVerifier.create(useCase.execute(1L, null))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void shouldFailWhenBranchDoesNotExist() {
        when(branchRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(99L, 10L))
                .expectError(RecordNotFoundException.class)
                .verify();

        verify(productRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void shouldFailWhenProductDoesNotExist() {
        when(branchRepository.findById(1L)).thenReturn(Mono.just(branch));
        when(productRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(1L, 99L))
                .expectError(RecordNotFoundException.class)
                .verify();

        verify(productRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void shouldFailWithNotFoundWhenProductBelongsToAnotherBranch() {
        Product other = Product.builder().id(10L).name("Arepa").stock(5).branchId(2L).build();

        when(branchRepository.findById(1L)).thenReturn(Mono.just(branch));
        when(productRepository.findById(10L)).thenReturn(Mono.just(other));

        StepVerifier.create(useCase.execute(1L, 10L))
                .expectError(RecordNotFoundException.class)
                .verify();

        verify(productRepository, never()).deleteById(any(Long.class));
    }
}
