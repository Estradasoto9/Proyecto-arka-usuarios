package com.projectArka.user_service.domain.port.out;

import com.projectArka.user_service.domain.model.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoleRepositoryPort {
    Mono<Role> findByName(String name);
    Mono<Role> save(Role role);
    Mono<Role> findById(String id);
    Flux<Role> findAll();
    Mono<Void> deleteById(String id);
}