package co.com.bancolombia.usecase.createfranchise;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseUseCaseTest {

    @Mock
    private FranchiseRepository repository;

    @InjectMocks
    private CreateFranchiseUseCase useCase;

    @Test
    void shouldSaveFranchiseWhenNameIsValid() {
        Franchise saved = Franchise.builder().id(1L).name("Nequi").build();
        when(repository.save(any(Franchise.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(useCase.execute("Nequi"))
                .expectNext(saved)
                .verifyComplete();
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        StepVerifier.create(useCase.execute("   "))
                .expectError(ValidationException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenNameIsNull() {
        StepVerifier.create(useCase.execute(null))
                .expectError(ValidationException.class)
                .verify();

        verify(repository, never()).save(any());
    }
}
