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
import co.com.bancolombia.api.dto.request.AddBranchRequest;
import co.com.bancolombia.model.branch.Branch;
import co.com.bancolombia.model.exception.RecordNotFoundException;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.usecase.addbranch.AddBranchUseCase;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = { RouterRest.class, FranchiseHandler.class, BranchHandler.class })
@WebFluxTest
class BranchHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AddBranchUseCase addBranchUseCase;

    // Required only so RouterRest can build its franchise routes.
    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

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
                        location -> assertThat(location).endsWith("/api/franchises/1/branches/10"))
                .expectBody()
                .jsonPath("$.id").isEqualTo(10)
                .jsonPath("$.name").isEqualTo("Cartagena")
                .jsonPath("$.products").isEmpty();
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
}
