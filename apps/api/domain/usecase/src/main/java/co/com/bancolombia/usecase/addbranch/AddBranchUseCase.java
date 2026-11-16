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
     * Checks run in order and the first failure ends the chain: blank name (400), null franchise id (400),
     * franchise not found (404). Only then is the branch built and saved.
     */
    public Mono<Branch> execute(Long franchiseId, String name) {
        return Mono.justOrEmpty(name)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Branch name must not be blank")))
                // then() waits for the previous check to pass, drops its value and moves on to the next check;
                // if the previous step errored, the error skips this step and goes straight out.
                .then(
                        Mono.justOrEmpty(franchiseId)
                                .switchIfEmpty(
                                        Mono.error(() -> new ValidationException("Franchise id cannot be null"))))
                .flatMap(this.franchiseRepository::findById)
                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Franchise not found")))
                .map(franchise -> Branch.builder().name(name).franchiseId(franchise.getId()).build())
                .flatMap(this.branchRepository::save);
    }
}
