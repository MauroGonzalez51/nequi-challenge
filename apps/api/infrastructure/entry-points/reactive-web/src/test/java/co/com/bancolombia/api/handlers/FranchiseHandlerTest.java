package co.com.bancolombia.api.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import co.com.bancolombia.api.RouterRest;
import co.com.bancolombia.api.dto.request.PatchNameRequest;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.api.dto.request.CreateFranchiseRequest;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.branch.BranchTopProduct;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.usecase.addbranch.AddBranchUseCase;
import co.com.bancolombia.usecase.addproduct.AddProductUseCase;
import co.com.bancolombia.usecase.removeproduct.RemoveProductUseCase;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
import co.com.bancolombia.usecase.gettopstockproductperbranch.GetTopStockProductPerBranchUseCase;
import co.com.bancolombia.usecase.patchbranchname.PatchBranchNameUseCase;
import co.com.bancolombia.usecase.patchfranchisename.PatchFranchiseNameUseCase;
import co.com.bancolombia.usecase.patchproductname.PatchProductNameUseCase;
import co.com.bancolombia.usecase.patchproductstock.PatchProductStockUseCase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = { RouterRest.class, FranchiseHandler.class, BranchHandler.class, ProductHandler.class, ErrorMapper.class })
@WebFluxTest
class FranchiseHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

    // Required only so RouterRest can build its branch routes.
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

    @MockitoBean
    private GetTopStockProductPerBranchUseCase getTopStockProductPerBranchUseCase;

    @Test
    void shouldReturnCreatedWhenFranchiseIsValid() {
        Franchise saved = Franchise.builder().id(1L).name("Nequi").build();
        when(createFranchiseUseCase.execute(any())).thenReturn(Mono.just(saved));

        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateFranchiseRequest("Nequi"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("Location", location -> assertThat(location).endsWith("/api/franchises/1"))
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Nequi")
                .jsonPath("$.branches").isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenNameIsInvalid() {
        when(createFranchiseUseCase.execute(any()))
                .thenReturn(Mono.error(new ValidationException("Franchise name must not be blank")));

        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateFranchiseRequest("  "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise name must not be blank");
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMissing() {
        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Request body is required");
    }

    @Test
    void shouldReturnBadRequestWhenNameIsMissing() {
        when(createFranchiseUseCase.execute(any()))
                .thenReturn(Mono.error(new ValidationException("Franchise name must not be blank")));

        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise name must not be blank");
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformed() {
        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("not json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnOkWhenNameIsPatched() {
        Franchise updated = Franchise.builder().id(5L).name("Renamed").build();
        when(patchFranchiseNameUseCase.execute(5L, "Renamed")).thenReturn(Mono.just(updated));

        webTestClient.patch()
                .uri("/api/franchises/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("Renamed"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.name").isEqualTo("Renamed");
    }

    @Test
    void shouldReturnBadRequestWhenPatchedNameIsInvalid() {
        when(patchFranchiseNameUseCase.execute(5L, "  "))
                .thenReturn(Mono.error(new ValidationException("Franchise name must not be blank")));

        webTestClient.patch()
                .uri("/api/franchises/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("  "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise name must not be blank");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameIdIsNotNumeric() {
        webTestClient.patch()
                .uri("/api/franchises/abc/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("Renamed"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise id must be a number");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameBodyIsMissing() {
        webTestClient.patch()
                .uri("/api/franchises/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Request body is required");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameBodyIsMalformed() {
        webTestClient.patch()
                .uri("/api/franchises/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("not json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnNotFoundWhenEntityToRenameDoesNotExist() {
        when(patchFranchiseNameUseCase.execute(99L, "Renamed"))
                .thenReturn(Mono.error(new RecordNotFoundException("Franchise not found")));

        webTestClient.patch()
                .uri("/api/franchises/99/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("Renamed"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise not found");
    }

    @Test
    void shouldReturnTopStockProductPerBranch() {
        when(getTopStockProductPerBranchUseCase.execute(1L)).thenReturn(Flux.just(
                new BranchTopProduct(Branch.builder().id(1L).name("Cartagena").franchiseId(1L).build(),
                        Product.builder().id(7L).name("Arepa").stock(20).branchId(1L).build()),
                new BranchTopProduct(Branch.builder().id(2L).name("Medellin").franchiseId(1L).build(), null)));

        webTestClient.get()
                .uri("/api/franchises/1/top-stock-products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("[{\"id\":1,\"name\":\"Cartagena\",\"product\":{\"id\":7,\"name\":\"Arepa\",\"stock\":20}},"
                        + "{\"id\":2,\"name\":\"Medellin\",\"product\":null}]");
    }

    @Test
    void shouldReturnEmptyListWhenFranchiseHasNoBranches() {
        when(getTopStockProductPerBranchUseCase.execute(1L)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/franchises/1/top-stock-products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("[]");
    }

    @Test
    void shouldReturnNotFoundWhenFranchiseOfTopStockDoesNotExist() {
        when(getTopStockProductPerBranchUseCase.execute(99L))
                .thenReturn(Flux.error(new RecordNotFoundException("Franchise not found")));

        webTestClient.get()
                .uri("/api/franchises/99/top-stock-products")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise not found");
    }

    @Test
    void shouldReturnBadRequestWhenTopStockFranchiseIdIsNotNumeric() {
        webTestClient.get()
                .uri("/api/franchises/abc/top-stock-products")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise id must be a number");
    }
}
