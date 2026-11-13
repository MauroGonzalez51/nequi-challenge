package co.com.bancolombia.api.dto.response;

import co.com.bancolombia.model.franchise.Franchise;
import java.util.List;

public record CreateFranchiseResponse(Long id, String name, List<BranchResponse> branches) {
    public static CreateFranchiseResponse from(Franchise franchise) {
        return new CreateFranchiseResponse(franchise.getId(), franchise.getName(), List.of());
    }
}
