package co.com.bancolombia.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.com.bancolombia.api.handlers.BranchHandler;
import co.com.bancolombia.api.handlers.FranchiseHandler;
import co.com.bancolombia.api.handlers.ProductHandler;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> franchiseRoutes(FranchiseHandler handler) {
        return route()
                .path("/api/franchises", builder -> builder.POST("", handler::create)).build();
    }

    @Bean
    public RouterFunction<ServerResponse> branchRoutes(BranchHandler handler) {
        return route()
                .path("/api/franchises/{franchiseId}/branches", builder -> builder.POST("", handler::create))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return route()
                .path("/api/branches",
                        builder -> builder
                                .POST("/{branchId}/products", handler::create)
                                .DELETE("/{branchId}/products/{productId}", handler::delete))
                .path("/api/products",
                        builder -> builder
                                .PATCH("/{productId}/stock", handler::patchStock))
                .build();
    }
}
