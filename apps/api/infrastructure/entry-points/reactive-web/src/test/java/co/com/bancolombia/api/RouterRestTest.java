package co.com.bancolombia.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import co.com.bancolombia.api.dto.request.CreateFranchiseRequest;
import co.com.bancolombia.api.handlers.FranchiseHandler;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.model.exception.ValidationException;
import co.com.bancolombia.usecase.createfranchise.CreateFranchiseUseCase;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = { RouterRest.class, FranchiseHandler.class })
@WebFluxTest
class RouterRestTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

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
                .expectHeader().value("Location", location -> org.assertj.core.api.Assertions
                        .assertThat(location).endsWith("/api/franchises/1"))
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
        webTestClient.post()
                .uri("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Field 'name' is required");
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
}
