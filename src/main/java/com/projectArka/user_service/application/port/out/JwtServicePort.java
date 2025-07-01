package com.projectArka.user_service.application.port.out;

import com.projectArka.user_service.domain.model.User;
import reactor.core.publisher.Mono;

public interface JwtServicePort {

    Mono<String> generateToken(User user);
    Mono<Boolean> validateToken(String token);
    Mono<String> extractUsername(String token);
}