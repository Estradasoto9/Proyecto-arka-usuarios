package com.projectArka.user_service.infrastructure.adapter.out.r2dbc.repository;

import com.projectArka.user_service.infrastructure.entity.RoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataRoleRepository extends ReactiveCrudRepository<RoleEntity, UUID> {
    Mono<RoleEntity> findByName(String name);
}