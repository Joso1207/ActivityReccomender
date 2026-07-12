package org.chasapi.activityreccomender.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
public class LocalhostAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    public Mono<AuthorizationDecision> check(
            Mono<Authentication> authentication,
            ServerWebExchange exchange) {

        InetSocketAddress remote =
                exchange.getRequest().getRemoteAddress();

        boolean localhost =
                remote != null &&
                        remote.getAddress().isLoopbackAddress();

        return Mono.just(new AuthorizationDecision(localhost));
    }

    @Override
    public Mono<Void> verify(Mono<Authentication> authentication, AuthorizationContext object) {
        return ReactiveAuthorizationManager.super.verify(authentication, object);
    }

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext object) {
        return null;
    }
}