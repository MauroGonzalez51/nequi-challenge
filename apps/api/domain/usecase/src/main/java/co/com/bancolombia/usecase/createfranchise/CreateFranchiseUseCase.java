package co.com.bancolombia.usecase.createfranchise;

import lombok.RequiredArgsConstructor;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.franchise.gateways.FranchiseRepository;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {
    private final FranchiseRepository repository;

}
