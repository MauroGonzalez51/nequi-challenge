package co.com.bancolombia.usecase.patchproductstock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PatchProductStockUseCaseTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private PatchProductStockUseCase useCase;

    private final Product existing = Product.builder().id(10L).branchId(1L).name("Arepa").stock(5).build();

    @Test
    void shouldUpdateOnlyTheStockWhenInputIsValid() {
        when(repository.findById(10L)).thenReturn(Mono.just(existing));
        when(repository.save(any(Product.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(useCase.execute(10L, 20))
                .expectNextMatches(updated -> updated.getStock() == 20
                        && updated.getId().equals(10L)
                        && updated.getBranchId().equals(1L)
                        && updated.getName().equals("Arepa"))
                .verifyComplete();
    }

    @Test
    void shouldUpdateWhenStockIsZero() {
        when(repository.findById(10L)).thenReturn(Mono.just(existing));
        when(repository.save(any(Product.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(useCase.execute(10L, 0))
                .expectNextMatches(updated -> updated.getStock() == 0)
                .verifyComplete();
    }

    @Test
    void shouldDoNothingWhenStockIsTheSame() {
        when(repository.findById(10L)).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.execute(10L, 5))
                .expectNext(existing)
                .verifyComplete();

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenStockIsNull() {
        StepVerifier.create(useCase.execute(10L, null))
                .expectError(ValidationException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFailWhenStockIsNegative() {
        StepVerifier.create(useCase.execute(10L, -1))
                .expectError(ValidationException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFailWhenProductIdIsNull() {
        StepVerifier.create(useCase.execute(null, 5))
                .expectError(ValidationException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFailWhenProductDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(99L, 5))
                .expectError(RecordNotFoundException.class)
                .verify();

        verify(repository, never()).save(any());
    }
}
