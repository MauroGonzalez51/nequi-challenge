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
                .flatMap(existing -> Mono.just(existing)
                        .filter(franchise -> !name.equals(franchise.getName()))
                        .map(franchise -> franchise.toBuilder().name(name).build())
                        .flatMap(this.repository::save)
                        .switchIfEmpty(Mono.just(existing)));
    }
}
