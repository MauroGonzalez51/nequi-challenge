package co.com.bancolombia.usecase.createfranchise;

import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.franchise.exception.InvalidFranchiseNameException;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {
    private final FranchiseRepository repository;

    public Mono<Franchise> execute(String name) {
        return Mono.justOrEmpty(name)
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(InvalidFranchiseNameException::new))
                .map(value -> Franchise.builder().name(value).build())
                .flatMap(repository::save);
    }
}
