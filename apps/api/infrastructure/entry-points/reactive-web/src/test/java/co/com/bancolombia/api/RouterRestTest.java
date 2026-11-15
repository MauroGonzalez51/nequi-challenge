package co.com.bancolombia.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.com.bancolombia.api.handlers.BranchHandler;
import co.com.bancolombia.api.handlers.FranchiseHandler;
import co.com.bancolombia.api.handlers.ProductHandler;
import reactor.core.publisher.Mono;

// Built by hand (no Spring context) so each route function under test is the one just compiled.
class RouterRestTest {

    private final RouterRest router = new RouterRest();
    private FranchiseHandler franchiseHandler;
    private BranchHandler branchHandler;
    private ProductHandler productHandler;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        franchiseHandler = mock(FranchiseHandler.class);
        branchHandler = mock(BranchHandler.class);
        productHandler = mock(ProductHandler.class);

        when(franchiseHandler.create(any())).thenAnswer(call -> ok());
        when(franchiseHandler.patchName(any())).thenAnswer(call -> ok());
        when(franchiseHandler.getTopStockProducts(any())).thenAnswer(call -> ok());
        when(branchHandler.create(any())).thenAnswer(call -> ok());
        when(branchHandler.patchName(any())).thenAnswer(call -> ok());
        when(productHandler.create(any())).thenAnswer(call -> ok());
        when(productHandler.delete(any())).thenAnswer(call -> ok());
        when(productHandler.patchStock(any())).thenAnswer(call -> ok());
        when(productHandler.patchName(any())).thenAnswer(call -> ok());

        RouterFunction<ServerResponse> all = router.franchiseRoutes(franchiseHandler)
                .and(router.branchRoutes(branchHandler))
                .and(router.productRoutes(productHandler));
        client = WebTestClient.bindToRouterFunction(all).build();
    }

    @Test
    void shouldRouteFranchiseEndpoints() {
        expectRouted(HttpMethod.POST, "/api/franchises");
        expectRouted(HttpMethod.PATCH, "/api/franchises/1/name");
        expectRouted(HttpMethod.GET, "/api/franchises/1/top-stock-products");
    }

    @Test
    void shouldRouteBranchEndpoints() {
        expectRouted(HttpMethod.POST, "/api/franchises/1/branches");
        expectRouted(HttpMethod.PATCH, "/api/branches/1/name");
    }

    @Test
    void shouldRouteProductEndpoints() {
        expectRouted(HttpMethod.POST, "/api/branches/1/products");
        expectRouted(HttpMethod.DELETE, "/api/branches/1/products/2");
        expectRouted(HttpMethod.PATCH, "/api/products/1/stock");
        expectRouted(HttpMethod.PATCH, "/api/products/1/name");
    }

    @Test
    void shouldNotRouteUnknownPaths() {
        client.get().uri("/api/unknown").exchange().expectStatus().isNotFound();
    }

    private void expectRouted(HttpMethod method, String path) {
        client.method(method).uri(path).exchange().expectStatus().isOk();
    }

    private Mono<ServerResponse> ok() {
        return ServerResponse.ok().build();
    }
}
