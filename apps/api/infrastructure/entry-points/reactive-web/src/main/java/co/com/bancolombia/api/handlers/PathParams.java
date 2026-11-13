package co.com.bancolombia.api.handlers;

import org.springframework.web.reactive.function.server.ServerRequest;

import co.com.bancolombia.api.exception.InvalidRequestException;
import reactor.core.publisher.Mono;

final class PathParams {

    private PathParams() {
    }

    static Mono<Long> id(ServerRequest request, String name, String label) {
        return Mono.fromCallable(() -> Long.valueOf(request.pathVariable(name)))
                .onErrorMap(NumberFormatException.class,
                        ignored -> new InvalidRequestException(label + " must be a number"));
    }
}
