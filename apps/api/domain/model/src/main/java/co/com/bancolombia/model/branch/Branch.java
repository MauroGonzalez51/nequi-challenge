package co.com.bancolombia.model.branch;

import java.util.List;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import co.com.bancolombia.model.product.Product;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Branch {
    Long id;
    Long franchiseId;
    String name;
    List<Product> products;
}
