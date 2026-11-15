package co.com.bancolombia.api.dto.response;

import co.com.bancolombia.model.franchise.Franchise;

public record FranchiseResponse(Long id, String name) {
    public static FranchiseResponse from(Franchise franchise) {
        return new FranchiseResponse(franchise.getId(), franchise.getName());
    }
}
