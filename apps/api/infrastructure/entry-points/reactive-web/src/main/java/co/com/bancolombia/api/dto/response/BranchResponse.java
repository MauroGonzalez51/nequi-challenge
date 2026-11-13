package co.com.bancolombia.api.dto.response;

import co.com.bancolombia.model.branch.Branch;
import java.util.List;

public record BranchResponse(Long id, String name, List<ProductResponse> products) {
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getName(), List.of());
    }
}
