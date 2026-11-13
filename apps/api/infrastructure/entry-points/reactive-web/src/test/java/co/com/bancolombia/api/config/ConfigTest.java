package co.com.bancolombia.api.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.com.bancolombia.api.RouterRest;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.api.dto.request.CreateFranchiseRequest;
import co.com.bancolombia.api.handlers.BranchHandler;
import co.com.bancolombia.api.handlers.ProductHandler;
import co.com.bancolombia.api.handlers.FranchiseHandler;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.usecase.addbranch.AddBranchUseCase;
import co.com.bancolombia.usecase.addproduct.AddProductUseCase;
import co.com.bancolombia.usecase.removeproduct.RemoveProductUseCase;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.patchbranchname.PatchBranchNameUseCase;
import co.com.bancolombia.usecase.patchfranchisename.PatchFranchiseNameUseCase;
import co.com.bancolombia.usecase.patchproductname.PatchProductNameUseCase;
import co.com.bancolombia.usecase.patchproductstock.PatchProductStockUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = {RouterRest.class, FranchiseHandler.class, BranchHandler.class, ProductHandler.class, ErrorMapper.class})
@WebFluxTest
@Import({CorsConfig.class, SecurityHeadersConfig.class})
class ConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

    @MockitoBean
    private AddBranchUseCase addBranchUseCase;

    @MockitoBean
    private AddProductUseCase addProductUseCase;

    @MockitoBean
    private RemoveProductUseCase removeProductUseCase;

    @MockitoBean
    private PatchProductStockUseCase patchProductStockUseCase;

    @MockitoBean
    private PatchFranchiseNameUseCase patchFranchiseNameUseCase;

    @MockitoBean
    private PatchBranchNameUseCase patchBranchNameUseCase;

    @MockitoBean
    private PatchProductNameUseCase patchProductNameUseCase;

    @Test
    void securityHeadersShouldBePresent() {
        when(createFranchiseUseCase.execute(any()))
                .thenReturn(Mono.just(Franchise.builder().id(1L).name("Nequi").build()));

        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateFranchiseRequest("Nequi"))
                .exchange()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().doesNotExist("Server")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

}
