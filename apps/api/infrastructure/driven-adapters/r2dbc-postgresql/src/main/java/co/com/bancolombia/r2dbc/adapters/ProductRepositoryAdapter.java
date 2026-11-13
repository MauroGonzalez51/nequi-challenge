package co.com.bancolombia.r2dbc.adapters;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.model.product.gateways.ProductRepository;
import co.com.bancolombia.r2dbc.entities.ProductEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.repositories.ProductEntityRepository;

@Repository
public class ProductRepositoryAdapter
        extends ReactiveAdapterOperations<Product, ProductEntity, Long, ProductEntityRepository>
        implements ProductRepository {

    public ProductRepositoryAdapter(ProductEntityRepository repository, ObjectMapper mapper) {
        super(repository, mapper, data -> mapper.map(data, Product.class));
    }
}
