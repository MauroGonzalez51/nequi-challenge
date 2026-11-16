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
import co.com.bancolombia.api.exception.ErrorMapper;
import co.com.bancolombia.api.dto.request.AddBranchRequest;
import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.exception.DataConflictException;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
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

@ContextConfiguration(classes = { RouterRest.class, FranchiseHandler.class, BranchHandler.class, ProductHandler.class,
        ErrorMapper.class })
@WebFluxTest
class BranchHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AddBranchUseCase addBranchUseCase;

    // Required only so RouterRest can build its franchise routes.
    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

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
    void shouldReturnCreatedWhenBranchIsValid() {
        Branch saved = Branch.builder().id(10L).name("Cartagena").franchiseId(1L).build();
        when(addBranchUseCase.execute(1L, "Cartagena")).thenReturn(Mono.just(saved));

        webTestClient.post()
                .uri("/api/franchises/1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddBranchRequest("Cartagena"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("Location",
                        location -> assertThat(location)
                                .endsWith("/api/franchises/1/branches/10"))
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.name").isEqualTo("Cartagena");
    }

    @Test
    void shouldReturnBadRequestWhenBranchNameIsInvalid() {
        when(addBranchUseCase.execute(1L, "  "))
                .thenReturn(Mono.error(new ValidationException("Branch name must not be blank")));

        webTestClient.post()
                .uri("/api/franchises/1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddBranchRequest("  "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch name must not be blank");
    }

    @Test
    void shouldReturnBadRequestWhenFranchiseIdIsNotNumeric() {
        webTestClient.post()
                .uri("/api/franchises/abc/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddBranchRequest("Cartagena"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise id must be a number");
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMissing() {
        webTestClient.post()
                .uri("/api/franchises/1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Request body is required");
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformed() {
        webTestClient.post()
                .uri("/api/franchises/1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("not json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnNotFoundWhenFranchiseDoesNotExist() {
        when(addBranchUseCase.execute(99L, "Cartagena"))
                .thenReturn(Mono.error(new RecordNotFoundException("Franchise not found")));

        webTestClient.post()
                .uri("/api/franchises/99/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddBranchRequest("Cartagena"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Franchise not found");
    }

    @Test
    void shouldReturnConflictWhenFranchiseIsRemovedDuringTheWrite() {
        when(addBranchUseCase.execute(1L, "Cartagena"))
                .thenReturn(Mono.error(new DataConflictException("conflict", new IllegalStateException("fk"))));

        webTestClient.post()
                .uri("/api/franchises/1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AddBranchRequest("Cartagena"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.message").isEqualTo("conflict")
                .jsonPath("$.errorCode").isEqualTo("DATA_CONFLICT");
    }

    @Test
    void shouldReturnOkWhenNameIsPatched() {
        Branch updated = Branch.builder().id(5L).name("Renamed").franchiseId(1L).build();
        when(patchBranchNameUseCase.execute(5L, "Renamed")).thenReturn(Mono.just(updated));

        webTestClient.patch()
                .uri("/api/branches/5/name")
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
        when(patchBranchNameUseCase.execute(5L, "  "))
                .thenReturn(Mono.error(new ValidationException("Branch name must not be blank")));

        webTestClient.patch()
                .uri("/api/branches/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("  "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch name must not be blank");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameIdIsNotNumeric() {
        webTestClient.patch()
                .uri("/api/branches/abc/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("Renamed"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch id must be a number");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameBodyIsMissing() {
        webTestClient.patch()
                .uri("/api/branches/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Request body is required");
    }

    @Test
    void shouldReturnBadRequestWhenPatchNameBodyIsMalformed() {
        webTestClient.patch()
                .uri("/api/branches/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("not json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid Request Body");
    }

    @Test
    void shouldReturnNotFoundWhenEntityToRenameDoesNotExist() {
        when(patchBranchNameUseCase.execute(99L, "Renamed"))
                .thenReturn(Mono.error(new RecordNotFoundException("Branch not found")));

        webTestClient.patch()
                .uri("/api/branches/99/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PatchNameRequest("Renamed"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch not found");
    }

    @Test
    void shouldRejectAddingABranchWithoutName() {
        webTestClient.post()
                .uri("/api/franchises/1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'name' is required");

        verifyNoInteractions(addBranchUseCase);
    }

    @Test
    void shouldRejectRenamingABranchWithoutName() {
        webTestClient.patch()
                .uri("/api/branches/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'name' is required");

        verifyNoInteractions(patchBranchNameUseCase);
    }
}
