package co.com.bancolombia.api.dto.response;

import java.util.Optional;
import co.com.bancolombia.model.branch.BranchTopProduct;

public record BranchTopProductResponse(Long id, String name, ProductResponse product) {
    public static BranchTopProductResponse from(BranchTopProduct result) {
        return new BranchTopProductResponse(
                result.getBranch().getId(),
                result.getBranch().getName(),
                Optional.ofNullable(result.getProduct()).map(ProductResponse::from).orElse(null));
    }
}
