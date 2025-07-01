package com.projectArka.user_service.infrastructure.adapter.out.r2dbc.repository;

import com.projectArka.user_service.infrastructure.entity.UserRoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataUserRoleRepository extends ReactiveCrudRepository<UserRoleEntity, UUID> {
    Flux<UserRoleEntity> findByUserId(UUID userId);
    Mono<Void> deleteByUserId(UUID userId);
}