package co.com.bancolombia.usecase.addbranch;

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
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AddBranchUseCaseTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private AddBranchUseCase useCase;

    @Test
    void shouldSaveBranchWhenInputIsValid() {
        Franchise franchise = Franchise.builder().id(1L).name("Nequi").build();
        Branch saved = Branch.builder().id(10L).name("Cartagena").franchiseId(1L).build();

        when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
        when(branchRepository.save(any(Branch.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(useCase.execute(1L, "Cartagena"))
                .expectNext(saved)
                .verifyComplete();
    }

    @Test
    void shouldTrimNameBeforeSaving() {
        Franchise franchise = Franchise.builder().id(1L).name("Nequi").build();

        when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
        when(branchRepository.save(any(Branch.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(useCase.execute(1L, "  Cartagena  "))
                .expectNextMatches(saved -> saved.getName().equals("Cartagena"))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        StepVerifier.create(useCase.execute(1L, "   "))
                .expectError(ValidationException.class)
                .verify();

        verify(branchRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenNameIsNull() {
        StepVerifier.create(useCase.execute(1L, null))
                .expectError(ValidationException.class)
                .verify();

        verify(branchRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenFranchiseIdIsNull() {
        StepVerifier.create(useCase.execute(null, "Cartagena"))
                .expectError(ValidationException.class)
                .verify();

        verify(branchRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenFranchiseDoesNotExist() {
        when(franchiseRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(99L, "Cartagena"))
                .expectError(RecordNotFoundException.class)
                .verify();

        verify(branchRepository, never()).save(any());
    }
}
