package co.com.bancolombia.usecase.patchfranchisename;

import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class PatchFranchiseNameUseCase {
    private final FranchiseRepository repository;

    /**
     * Renames a franchise. Validates the new name (null, then blank) and the id, loads the franchise, and saves only
     * when the name really changes. Same name: returns the stored franchise untouched, skipping a useless UPDATE.
     */
    public Mono<Franchise> execute(Long id, String name) {
        return Mono.justOrEmpty(name)
                .switchIfEmpty(Mono.error(() -> new ValidationException("Franchise name cannot be null")))
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Franchise name must not be blank")))
                .then(
                        Mono.justOrEmpty(id)
                                .switchIfEmpty(Mono.error(() -> new ValidationException("Franchise id cannot be null")))
                                .flatMap(this.repository::findById)
                                .switchIfEmpty(Mono.error(() -> new RecordNotFoundException("Franchise not found"))))
                // filter() turns "same name" into an empty Mono, so save() is skipped and switchIfEmpty returns the stored entity.
                .flatMap(existing -> Mono.just(existing)
                        .filter(franchise -> !name.equals(franchise.getName()))
                        .map(franchise -> franchise.toBuilder().name(name).build())
                        .flatMap(this.repository::save)
                        .switchIfEmpty(Mono.just(existing)));
    }
}
