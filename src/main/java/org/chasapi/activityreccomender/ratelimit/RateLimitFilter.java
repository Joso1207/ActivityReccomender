package org.chasapi.activityreccomender.ratelimit;


import io.github.bucket4j.ConsumptionProbe;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Component
public class RateLimitFilter implements WebFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;

    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             WebFilterChain chain) {

        String key = resolveKey(exchange);

        ConsumptionProbe probe = rateLimitService.consume(key);

        if (probe.isConsumed()) {
            exchange.getResponse()
                    .getHeaders()
                    .add("X-RateLimit-Remaining",
                            String.valueOf(probe.getRemainingTokens()));

            return chain.filter(exchange);
        }

        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

        response.getHeaders().add(
                "Retry-After",
                String.valueOf(
                        Duration.ofNanos(probe.getNanosToWaitForRefill())
                                .toSeconds()));

        response.getHeaders().add(
                "X-RateLimit-Remaining",
                "0");

        return response.setComplete();
    }

    private String resolveKey(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
    }
}