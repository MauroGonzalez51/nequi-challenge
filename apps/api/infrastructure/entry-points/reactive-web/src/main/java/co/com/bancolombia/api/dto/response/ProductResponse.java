package co.com.bancolombia.api.dto.response;

import co.com.bancolombia.model.product.Product;

public record ProductResponse(Long id, String name, Integer stock) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getStock());
    }
}
