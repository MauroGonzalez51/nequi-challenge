package co.com.bancolombia.usecase.createfranchise;

import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {
    private final FranchiseRepository repository;

    /**
     * Creates a franchise. Only rule: the name must not be null or blank (400).
     */
    public Mono<Franchise> execute(String name) {
        return Mono.justOrEmpty(name)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(() -> new ValidationException("Franchise name must not be blank")))
                .map(value -> Franchise.builder().name(value).build())
                .flatMap(repository::save);
    }
}
