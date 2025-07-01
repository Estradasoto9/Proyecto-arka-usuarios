package com.projectArka.user_service.infrastructure.config.security;

import com.projectArka.user_service.application.port.out.JwtServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BearerTokenServerSecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtServicePort jwtServicePort;
    private final ReactiveUserDetailsService userDetailsService;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .flatMap(authHeader -> {
                    String authToken = authHeader.substring(7);
                    return jwtServicePort.extractUsername(authToken)
                            .flatMap(username -> jwtServicePort.validateToken(authToken)
                                    .filter(isValid -> isValid)
                                    .flatMap(isValid -> userDetailsService.findByUsername(username))
                                    .map(userDetails -> {
                                        UsernamePasswordAuthenticationToken authentication =
                                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                        return (SecurityContext) new SecurityContextImpl(authentication);
                                    }));
                })
                .switchIfEmpty(Mono.empty());
    }
}