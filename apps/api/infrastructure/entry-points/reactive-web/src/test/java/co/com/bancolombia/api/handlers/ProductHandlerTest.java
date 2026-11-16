package co.com.bancolombia.api.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import co.com.bancolombia.api.RouterRest;
import co.com.bancolombia.api.dto.request.PatchNameRequest;
import co.com.bancolombia.api.dto.request.AddProductRequest;
import co.com.bancolombia.api.dto.request.PatchProductStockRequest;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.model.exception.DataConflictException;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
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
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = { RouterRest.class, FranchiseHandler.class, BranchHandler.class,
        ProductHandler.class, ErrorMapper.class })
@WebFluxTest
class ProductHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

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

    // Required only so RouterRest can build the other routes.
    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

    @MockitoBean
    private AddBranchUseCase addBranchUseCase;

    @Test
    void shouldReturnCreatedWhenProductIsValid() {
        Product saved = Product.builder().id(10L).name("Arepa").stock(5).branchId(1L).build();
        when(addProductUseCase.execute(1L, "Arepa", 5)).thenReturn(Mono.just(saved));

        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddProductRequest("Arepa", 5))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("Location",
                        location -> assertThat(location).endsWith("/api/branches/1/products/10"))
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.name").isEqualTo("Arepa")
                .jsonPath("$.stock").isEqualTo(5);
    }

    @Test
    void shouldReturnBadRequestWhenProductIsInvalid() {
        when(addProductUseCase.execute(1L, "Arepa", -1))
                .thenReturn(Mono.error(new ValidationException("Product stock must be greater than or equal to 0")));

        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddProductRequest("Arepa", -1))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product stock must be greater than or equal to 0");
    }

    @Test
    void shouldReturnBadRequestWhenStockIsNotNumeric() {
        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Arepa\",\"stock\":\"abc\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnBadRequestWhenBranchIdIsNotNumeric() {
        webTestClient.post()
                .uri("/api/branches/abc/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddProductRequest("Arepa", 5))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch id must be a number");
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMissing() {
        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Request body is required");
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformed() {
        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("not json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnNotFoundWhenBranchDoesNotExist() {
        when(addProductUseCase.execute(99L, "Arepa", 5))
                .thenReturn(Mono.error(new RecordNotFoundException("Branch not found")));

        webTestClient.post()
                .uri("/api/branches/99/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddProductRequest("Arepa", 5))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch not found");
    }

    @Test
    void shouldReturnConflictWhenBranchIsRemovedDuringTheWrite() {
        when(addProductUseCase.execute(1L, "Arepa", 5))
                .thenReturn(Mono.error(new DataConflictException("conflict", new IllegalStateException("fk"))));

        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddProductRequest("Arepa", 5))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.message").isEqualTo("conflict")
                .jsonPath("$.errorCode").isEqualTo("DATA_CONFLICT");
    }

    @Test
    void shouldReturnNoContentWhenProductIsDeleted() {
        when(removeProductUseCase.execute(1L, 5L)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/branches/1/products/5")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenProductToDeleteDoesNotExist() {
        when(removeProductUseCase.execute(1L, 99L))
                .thenReturn(Mono.error(new RecordNotFoundException("Product not found")));

        webTestClient.delete()
                .uri("/api/branches/1/products/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product not found");
    }

    @Test
    void shouldReturnNotFoundWhenBranchOfProductToDeleteDoesNotExist() {
        when(removeProductUseCase.execute(99L, 5L))
                .thenReturn(Mono.error(new RecordNotFoundException("Branch not found")));

        webTestClient.delete()
                .uri("/api/branches/99/products/5")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch not found");
    }

    @Test
    void shouldReturnBadRequestWhenDeleteBranchIdIsNotNumeric() {
        webTestClient.delete()
                .uri("/api/branches/abc/products/5")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch id must be a number");
    }

    @Test
    void shouldReturnBadRequestWhenDeleteProductIdIsNotNumeric() {
        webTestClient.delete()
                .uri("/api/branches/1/products/abc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product id must be a number");
    }

    @Test
    void shouldReturnOkWhenStockIsPatched() {
        Product updated = Product.builder().id(5L).name("Arepa").stock(20).branchId(1L).build();
        when(patchProductStockUseCase.execute(5L, 20)).thenReturn(Mono.just(updated));

        webTestClient.patch()
                .uri("/api/products/5/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchProductStockRequest(20))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.name").isEqualTo("Arepa")
                .jsonPath("$.stock").isEqualTo(20);
    }

    @Test
    void shouldReturnBadRequestWhenPatchedStockIsInvalid() {
        when(patchProductStockUseCase.execute(5L, -1))
                .thenReturn(Mono.error(new ValidationException("Product stock must be greater than or equal to 0")));

        webTestClient.patch()
                .uri("/api/products/5/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchProductStockRequest(-1))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product stock must be greater than or equal to 0");
    }

    @Test
    void shouldReturnBadRequestWhenPatchedStockIsNotNumeric() {
        webTestClient.patch()
                .uri("/api/products/5/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"stock\":\"abc\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnBadRequestWhenPatchProductIdIsNotNumeric() {
        webTestClient.patch()
                .uri("/api/products/abc/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchProductStockRequest(5))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product id must be a number");
    }

    @Test
    void shouldReturnBadRequestWhenPatchBodyIsMissing() {
        webTestClient.patch()
                .uri("/api/products/5/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Request body is required");
    }

    @Test
    void shouldReturnNotFoundWhenProductToPatchDoesNotExist() {
        when(patchProductStockUseCase.execute(99L, 5))
                .thenReturn(Mono.error(new RecordNotFoundException("Product not found")));

        webTestClient.patch()
                .uri("/api/products/99/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchProductStockRequest(5))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product not found");
    }

    @Test
    void shouldReturnOkWhenNameIsPatched() {
        Product updated = Product.builder().id(5L).name("Renamed").stock(3).branchId(1L).build();
        when(patchProductNameUseCase.execute(5L, "Renamed")).thenReturn(Mono.just(updated));

        webTestClient.patch()
                .uri("/api/products/5/name")
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
        when(patchProductNameUseCase.execute(5L, "  "))
                .thenReturn(Mono.error(new ValidationException("Product name must not be blank")));

        webTestClient.patch()
                .uri("/api/products/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("  "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product name must not be blank");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameIdIsNotNumeric() {
        webTestClient.patch()
                .uri("/api/products/abc/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("Renamed"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product id must be a number");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameBodyIsMissing() {
        webTestClient.patch()
                .uri("/api/products/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Request body is required");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameBodyIsMalformed() {
        webTestClient.patch()
                .uri("/api/products/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("not json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnNotFoundWhenEntityToRenameDoesNotExist() {
        when(patchProductNameUseCase.execute(99L, "Renamed"))
                .thenReturn(Mono.error(new RecordNotFoundException("Product not found")));

        webTestClient.patch()
                .uri("/api/products/99/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("Renamed"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product not found");
    }

    @Test
    void shouldRejectAddingAProductWithoutName() {
        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"stock\":5}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'name' is required");

        verifyNoInteractions(addProductUseCase);
    }

    @Test
    void shouldRejectAddingAProductWithoutStock() {
        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Arepa\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'stock' is required");

        verifyNoInteractions(addProductUseCase);
    }

    @Test
    void shouldReportEveryMissingFieldWhenAddingAProduct() {
        webTestClient.post()
                .uri("/api/branches/1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'name' is required, Field 'stock' is required");

        verifyNoInteractions(addProductUseCase);
    }

    @Test
    void shouldRejectPatchingStockWithoutStock() {
        webTestClient.patch()
                .uri("/api/products/5/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'stock' is required");

        verifyNoInteractions(patchProductStockUseCase);
    }

    @Test
    void shouldRejectRenamingAProductWithoutName() {
        webTestClient.patch()
                .uri("/api/products/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'name' is required");

        verifyNoInteractions(patchProductNameUseCase);
    }
}
