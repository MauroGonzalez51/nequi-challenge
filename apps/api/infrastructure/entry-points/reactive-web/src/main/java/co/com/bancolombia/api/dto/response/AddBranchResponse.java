package co.com.bancolombia.api.dto.response;

import co.com.bancolombia.model.branch.Branch;
import java.util.List;

public record AddBranchResponse(Long id, String name, List<ProductResponse> products) {
    public static AddBranchResponse from(Branch branch) {
        return new AddBranchResponse(branch.getId(), branch.getName(), List.of());
    }
}
