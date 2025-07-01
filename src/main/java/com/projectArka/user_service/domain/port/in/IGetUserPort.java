package com.projectArka.user_service.domain.port.in;

import com.projectArka.user_service.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IGetUserPort {
    Mono<User> getUserById(String id);
    Mono<User> getUserByUsername(String username);
    Flux<User> getAllUsers();
}