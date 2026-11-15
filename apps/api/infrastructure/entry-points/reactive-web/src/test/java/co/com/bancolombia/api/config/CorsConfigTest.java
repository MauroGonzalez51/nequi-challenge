package co.com.bancolombia.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

// Built by hand (no Spring context) so the filter under test is the one just compiled.
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";
    private static final String OTHER_ORIGIN = "http://evil.example.com";

    private final WebTestClient client = WebTestClient
            .bindToRouterFunction(RouterFunctions.route()
                    .GET("/ping", request -> ServerResponse.ok().build())
                    .build())
            .webFilter(new CorsConfig().corsWebFilter(List.of(ALLOWED_ORIGIN)))
            .build();

    @Test
    void shouldAllowTheConfiguredOriginWithCredentials() {
        client.get()
                .uri("http://localhost/ping")
                .header("Origin", ALLOWED_ORIGIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", ALLOWED_ORIGIN)
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void shouldRejectAnOriginThatIsNotConfigured() {
        client.get()
                .uri("http://localhost/ping")
                .header("Origin", OTHER_ORIGIN)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }

    @Test
    void shouldAnswerThePreflightWithAllowedMethodsAndHeaders() {
        client.options()
                .uri("http://localhost/ping")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", HttpMethod.PATCH.name())
                .header("Access-Control-Request-Headers", "x-custom")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value("Access-Control-Allow-Methods",
                        methods -> assertThat(methods).contains("POST", "GET", "DELETE", "PUT", "PATCH"))
                .expectHeader().value("Access-Control-Allow-Headers",
                        headers -> assertThat(headers).containsIgnoringCase("x-custom"));
    }
}
