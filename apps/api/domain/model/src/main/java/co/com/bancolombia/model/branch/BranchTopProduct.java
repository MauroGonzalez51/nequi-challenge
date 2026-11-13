package co.com.bancolombia.model.branch;

import co.com.bancolombia.model.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BranchTopProduct {
    Branch branch;
    Product product;
}
