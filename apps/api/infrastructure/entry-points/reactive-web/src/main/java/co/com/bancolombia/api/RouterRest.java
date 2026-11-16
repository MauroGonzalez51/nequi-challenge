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
/**
 * Routes only: each path points to a handler method and there is no logic here (RouterFunctions, no @RestController).
 */
public class RouterRest {

    @Bean
    /**
     * Franchise: create, rename, and top-stock product of each branch.
     */
    public RouterFunction<ServerResponse> franchiseRoutes(FranchiseHandler handler) {
        return route()
                .path("/api/franchises",
                        builder -> builder
                                .POST("", handler::create)
                                .PATCH("/{franchiseId}/name", handler::patchName)
                                .GET("/{franchiseId}/top-stock-products", handler::getTopStockProducts))
                .build();
    }

    @Bean
    /**
     * Branch: create under a franchise, and rename.
     */
    public RouterFunction<ServerResponse> branchRoutes(BranchHandler handler) {
        return route()
                .path("/api/franchises/{franchiseId}/branches", builder -> builder.POST("", handler::create))
                .path("/api/branches", builder -> builder.PATCH("/{branchId}/name", handler::patchName))
                .build();
    }

    @Bean
    /**
     * Product: create and delete under a branch, and change stock or name.
     */
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return route()
                .path("/api/branches",
                        builder -> builder
                                .POST("/{branchId}/products", handler::create)
                                .DELETE("/{branchId}/products/{productId}", handler::delete))
                .path("/api/products",
                        builder -> builder
                                .PATCH("/{productId}/stock", handler::patchStock)
                                .PATCH("/{productId}/name", handler::patchName))
                .build();
    }
}
