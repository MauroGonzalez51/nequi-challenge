package co.com.bancolombia.usecase.patchbranchname;

import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.branch.gateways.BranchRepository;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class PatchBranchNameUseCase {
    private final BranchRepository repository;

    /**
     * Renames a branch. Validates the new name (null, then blank) and the id, loads the branch, and saves only
     * when the name really changes. Same name: returns the stored branch untouched, skipping a useless UPDATE.
     */
    public Mono<Branch> execute(Long id, String name) {
        return Mono.justOrEmpty(name)
                .switchIfEmpty(Mono.error(() -> new ValidationException("Branch name cannot be null")))
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono
                        .error(() -> new ValidationException("Branch name must not be blank")))
                .then(
                        Mono.justOrEmpty(id)
                                .switchIfEmpty(Mono.error(() -> new ValidationException(
                                        "Branch id cannot be null")))
                                .flatMap(this.repository::findById)
                                .switchIfEmpty(Mono.error(
                                        () -> new RecordNotFoundException(
                                                "Branch not found"))))
                // filter() turns "same name" into an empty Mono, so save() is skipped and switchIfEmpty returns the stored entity.
                .flatMap(existing -> Mono.just(existing)
                        .filter(branch -> !name.equals(branch.getName()))
                        .map(branch -> branch.toBuilder().name(name).build())
                        .flatMap(this.repository::save)
                        .switchIfEmpty(Mono.just(existing)));
    }
}
