package co.com.bancolombia.usecase.patchbranchname;

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
import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.branch.gateways.BranchRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PatchBranchNameUseCaseTest {

    @Mock
    private BranchRepository repository;

    @InjectMocks
    private PatchBranchNameUseCase useCase;

    private final Branch existing = Branch.builder().id(10L).name("Old").franchiseId(1L).build();

    @Test
    void shouldRenameWhenNameChanges() {
        when(repository.findById(10L)).thenReturn(Mono.just(existing));
        when(repository.save(any(Branch.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(useCase.execute(10L, "New"))
                .expectNextMatches(updated -> updated.getName().equals("New") && updated.getId().equals(10L))
                .verifyComplete();
    }

    @Test
    void shouldDoNothingWhenNameIsTheSame() {
        when(repository.findById(10L)).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.execute(10L, "Old"))
                .expectNext(existing)
                .verifyComplete();

        verify(repository, never()).save(any());
    }

    @Test
    void shouldTrimNameWhenRenaming() {
        when(repository.findById(10L)).thenReturn(Mono.just(existing));
        when(repository.save(any(Branch.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(useCase.execute(10L, "  New  "))
                .expectNextMatches(updated -> updated.getName().equals("New"))
                .verifyComplete();
    }

    @Test
    void shouldDoNothingWhenTrimmedNameIsTheSame() {
        when(repository.findById(10L)).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.execute(10L, "  Old  "))
                .expectNext(existing)
                .verifyComplete();

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailWhenNameIsNull() {
        StepVerifier.create(useCase.execute(10L, null))
                .expectError(ValidationException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        StepVerifier.create(useCase.execute(10L, "   "))
                .expectError(ValidationException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFailWhenIdIsNull() {
        StepVerifier.create(useCase.execute(null, "New"))
                .expectError(ValidationException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFailWhenEntityDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(99L, "New"))
                .expectError(RecordNotFoundException.class)
                .verify();

        verify(repository, never()).save(any());
    }
}
