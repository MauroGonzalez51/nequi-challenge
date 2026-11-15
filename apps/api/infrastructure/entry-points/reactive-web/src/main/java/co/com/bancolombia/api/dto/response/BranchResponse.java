package co.com.bancolombia.api.dto.response;

import co.com.bancolombia.model.branch.Branch;

public record BranchResponse(Long id, String name) {
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getName());
    }
}
