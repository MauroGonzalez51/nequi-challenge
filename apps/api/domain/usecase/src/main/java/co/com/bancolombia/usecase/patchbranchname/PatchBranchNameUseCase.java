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
                .flatMap(existing -> Mono.just(existing)
                        .filter(branch -> !name.equals(branch.getName()))
                        .map(branch -> branch.toBuilder().name(name).build())
                        .flatMap(this.repository::save)
                        .switchIfEmpty(Mono.just(existing)));
    }
}
