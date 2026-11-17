package co.com.bancolombia.usecase.addbranch;

import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.branch.gateways.BranchRepository;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AddBranchUseCase {
    private final FranchiseRepository franchiseRepository;
    private final BranchRepository branchRepository;

    /**
     * Creates a branch under an existing franchise.
     * Checks run in order and the first failure ends the chain: null or blank name
     * (400), null franchise id (400), franchise not found (404). Only then is the
     * branch built, with the trimmed name, and saved.
     */
    public Mono<Branch> execute(Long franchiseId, String name) {
        // justOrEmpty (not just): a null name must become an empty Mono, so it ends in
        // the ValidationException below instead of throwing a NullPointerException.
        return Mono.justOrEmpty(name)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Branch name must not be blank")))
                .map(String::trim)
                // The inner chain lives inside flatMap so the trimmed name stays in scope
                // and the later steps run only if the name check passed.
                .flatMap(trimmed -> Mono.justOrEmpty(franchiseId)
                        .switchIfEmpty(Mono.error(() -> new ValidationException("Franchise id cannot be null")))
                        .flatMap(this.franchiseRepository::findById)
                        .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Franchise not found")))
                        .map(franchise -> Branch.builder().name(trimmed).franchiseId(franchise.getId()).build())
                        .flatMap(this.branchRepository::save));
    }
}
