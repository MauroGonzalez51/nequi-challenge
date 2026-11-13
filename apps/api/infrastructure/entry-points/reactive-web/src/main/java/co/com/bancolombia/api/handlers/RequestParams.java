package co.com.bancolombia.api.handlers;

import org.springframework.web.reactive.function.server.ServerRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import co.com.bancolombia.api.exception.InvalidRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Single place that reads and validates what handlers take from the request:
 * path ids and body. Keeps names and error messages out of the handlers.
 */
final class RequestParams {

    private static final String FRANCHISE_ID = "franchiseId";
    private static final String BRANCH_ID = "branchId";
    private static final String PRODUCT_ID = "productId";
    private static final String BODY_REQUIRED = "Request body is required";
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private RequestParams() {
    }

    static Mono<Long> franchiseId(ServerRequest request) {
        return id(request, FRANCHISE_ID, "Franchise id");
    }

    static Mono<Long> branchId(ServerRequest request) {
        return id(request, BRANCH_ID, "Branch id");
    }

    static Mono<Long> productId(ServerRequest request) {
        return id(request, PRODUCT_ID, "Product id");
    }

    static <T> Mono<T> body(ServerRequest request, Class<T> type) {
        return request.bodyToMono(type)
                .switchIfEmpty(Mono.error(() -> new InvalidRequestException(BODY_REQUIRED)))
                .flatMap(RequestParams::validated);
    }

    // Bean Validation on the request shape. Emits the body when it is valid, or fails with
    // every violation joined (sorted, since the violations come in no fixed order).
    private static <T> Mono<T> validated(T body) {
        return Flux.fromIterable(VALIDATOR.validate(body))
                .map(violation -> "Field '" + violation.getPropertyPath() + "' " + violation.getMessage())
                .sort()
                .collectList()
                .filter(messages -> !messages.isEmpty())
                .flatMap(messages -> Mono.<T>error(new InvalidRequestException(String.join(", ", messages))))
                .defaultIfEmpty(body);
    }

    private static Mono<Long> id(ServerRequest request, String name, String label) {
        return Mono.fromCallable(() -> Long.valueOf(request.pathVariable(name)))
                .onErrorMap(NumberFormatException.class,
                        ignored -> new InvalidRequestException(label + " must be a number"));
    }
}
