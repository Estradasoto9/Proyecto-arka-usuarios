package com.projectArka.user_service.infrastructure.adapter.out.r2dbc.adapter;

import com.projectArka.user_service.infrastructure.entity.RoleEntity;
import com.projectArka.user_service.application.mapper.IPersistenceRoleMapper;
import com.projectArka.user_service.domain.model.Role;
import com.projectArka.user_service.domain.port.out.RoleRepositoryPort;

import com.projectArka.user_service.infrastructure.adapter.out.r2dbc.repository.SpringDataRoleRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepositoryPort {

    private final SpringDataRoleRepository springDataRoleRepository;
    private final IPersistenceRoleMapper roleMapper;

    @Override
    public Mono<Role> findByName(String name) {
        return springDataRoleRepository.findByName(name)
                .map(roleMapper::toDomain);
    }

    @Override
    public Mono<Role> save(Role role) {
        RoleEntity roleEntityToSave = roleMapper.toEntity(role);
        return springDataRoleRepository.save(roleEntityToSave)
                .map(roleMapper::toDomain);
    }

    @Override
    public Mono<Role> findById(String id) {
        return springDataRoleRepository.findById(UUID.fromString(id))
                .map(roleMapper::toDomain);
    }

    @Override
    public Flux<Role> findAll() {
        return springDataRoleRepository.findAll()
                .map(roleMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return springDataRoleRepository.deleteById(UUID.fromString(id));
    }
}