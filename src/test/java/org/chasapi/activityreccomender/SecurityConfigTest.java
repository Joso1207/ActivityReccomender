package org.chasapi.activityreccomender;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;

import org.chasapi.activityreccomender.security.LocalhostAuthorizationManager;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SecurityConfigTest {

    private final LocalhostAuthorizationManager authorizationManager =
            new LocalhostAuthorizationManager();


    @Test
    void localhostRequestShouldBeAllowed() {

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .get("/api/private")
                        .remoteAddress(
                                new InetSocketAddress("127.0.0.1", 8080)
                        )
                        .build()
        );

        StepVerifier.create(
                        authorizationManager.check(Mono.empty(), exchange)
                )
                .assertNext(decision ->
                        assertThat(decision.isGranted())
                                .isTrue()
                )
                .verifyComplete();
    }


    @Test
    void remoteRequestShouldBeDenied() {

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .get("/api/private")
                        .remoteAddress(
                                new InetSocketAddress("192.168.1.50", 8080)
                        )
                        .build()
        );

        StepVerifier.create(
                        authorizationManager.check(Mono.empty(), exchange)
                )
                .assertNext(decision ->
                        assertThat(decision.isGranted())
                                .isFalse()
                )
                .verifyComplete();
    }
}