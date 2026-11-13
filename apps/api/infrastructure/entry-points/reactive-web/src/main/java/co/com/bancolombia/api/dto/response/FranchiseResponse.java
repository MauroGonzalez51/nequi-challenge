package co.com.bancolombia.api.dto.response;

import co.com.bancolombia.model.franchise.Franchise;
import java.util.List;

public record FranchiseResponse(Long id, String name, List<BranchResponse> branches) {
    public static FranchiseResponse from(Franchise franchise) {
        return new FranchiseResponse(franchise.getId(), franchise.getName(), List.of());
    }
}
