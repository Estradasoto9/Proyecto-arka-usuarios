package com.projectArka.user_service.infrastructure.adapter.out.r2dbc.repository;

import com.projectArka.user_service.infrastructure.entity.UserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataUserRepository extends ReactiveCrudRepository<UserEntity, UUID> {
    Mono<UserEntity> findByUsername(String username);
    Mono<UserEntity> findByEmail(String email);
}