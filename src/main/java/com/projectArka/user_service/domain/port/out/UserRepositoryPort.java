package com.projectArka.user_service.domain.port.out;

import com.projectArka.user_service.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {
    Mono<User> save(User user);
    Mono<User> findById(String id);
    Mono<User> findByUsername(String username);
    Mono<User> findByEmail(String email);
    Flux<User> findAll();
    Mono<Void> deleteById(String id);
}