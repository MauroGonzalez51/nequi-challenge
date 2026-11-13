package co.com.bancolombia.api.handlers;

import org.springframework.web.reactive.function.server.ServerRequest;

import co.com.bancolombia.api.exception.InvalidRequestException;
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
                .switchIfEmpty(Mono.error(() -> new InvalidRequestException(BODY_REQUIRED)));
    }

    private static Mono<Long> id(ServerRequest request, String name, String label) {
        return Mono.fromCallable(() -> Long.valueOf(request.pathVariable(name)))
                .onErrorMap(NumberFormatException.class,
                        ignored -> new InvalidRequestException(label + " must be a number"));
    }
}
