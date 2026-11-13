package co.com.bancolombia.api.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import co.com.bancolombia.api.RouterRest;
import co.com.bancolombia.api.dto.request.AddProductRequest;
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.model.product.Product;
import co.com.bancolombia.usecase.addbranch.AddBranchUseCase;
import co.com.bancolombia.usecase.addproduct.AddProductUseCase;
import co.com.bancolombia.usecase.removeproduct.RemoveProductUseCase;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
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
}
