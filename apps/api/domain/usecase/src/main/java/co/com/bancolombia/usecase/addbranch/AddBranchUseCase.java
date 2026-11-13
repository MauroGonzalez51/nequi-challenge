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

    public Mono<Branch> execute(Long franchiseId, String name) {
        return Mono.justOrEmpty(name)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Branch name must not be blank")))
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
