package com.projectArka.user_service.infrastructure.adapter.out.r2dbc.adapter;

import com.projectArka.user_service.infrastructure.entity.UserEntity;
import com.projectArka.user_service.infrastructure.entity.UserRoleEntity;
import com.projectArka.user_service.application.mapper.IPersistenceRoleMapper;
import com.projectArka.user_service.application.mapper.IPersistenceUserMapper;
import com.projectArka.user_service.domain.model.Role;
import com.projectArka.user_service.domain.model.User;
import com.projectArka.user_service.domain.port.out.UserRepositoryPort;
import com.projectArka.user_service.infrastructure.adapter.out.r2dbc.repository.SpringDataRoleRepository;
import com.projectArka.user_service.infrastructure.adapter.out.r2dbc.repository.SpringDataUserRepository;
import com.projectArka.user_service.infrastructure.adapter.out.r2dbc.repository.SpringDataUserRoleRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataRoleRepository springDataRoleRepository;
    private final SpringDataUserRoleRepository springDataUserRoleRepository;
    private final IPersistenceUserMapper userMapper;
    private final IPersistenceRoleMapper roleMapper;

    @Override
    public Mono<User> save(User user) {
        UserEntity userEntityToSave = userMapper.toEntity(user);
        userEntityToSave = userEntityToSave.toBuilder()
                .updatedAt(LocalDateTime.now())
                .build();

        return springDataUserRepository.save(userEntityToSave)
                .flatMap(savedUserEntity ->
                        springDataUserRoleRepository.deleteByUserId(savedUserEntity.getId())
                                .then(Mono.defer(() -> {
                                    Set<String> rolesToSave = user.getRoles();
                                    if (rolesToSave == null || rolesToSave.isEmpty()) {
                                        return Mono.just(savedUserEntity);
                                    }

                                    return Flux.fromIterable(rolesToSave)
                                            .flatMap(roleName ->
                                                    springDataRoleRepository.findByName(roleName)
                                                            .switchIfEmpty(Mono.error(new IllegalStateException("Role '" + roleName + "' not found. Ensure it's configured.")))
                                                            .flatMap(roleEntity -> {
                                                                UserRoleEntity userRole = UserRoleEntity.builder()
                                                                        .userId(savedUserEntity.getId())
                                                                        .roleId(roleEntity.getId())
                                                                        .build();
                                                                return springDataUserRoleRepository.save(userRole);
                                                            })
                                            )
                                            .collectList()
                                            .then(Mono.just(savedUserEntity));
                                }))
                )
                .flatMap(this::loadUserRoles);
    }

    @Override
    public Mono<User> findById(String id) {
        return springDataUserRepository.findById(UUID.fromString(id))
                .flatMap(this::loadUserRoles);
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username)
                .flatMap(this::loadUserRoles);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email)
                .flatMap(this::loadUserRoles);
    }

    @Override
    public Flux<User> findAll() {
        return springDataUserRepository.findAll()
                .flatMap(this::loadUserRoles);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return springDataUserRoleRepository.deleteByUserId(UUID.fromString(id))
                .then(springDataUserRepository.deleteById(UUID.fromString(id)));
    }

    private Mono<User> loadUserRoles(UserEntity userEntity) {
        if (userEntity.getId() == null) {
            return Mono.just(userMapper.toDomain(userEntity).toBuilder().roles(Collections.emptySet()).build());
        }
        return springDataUserRoleRepository.findByUserId(userEntity.getId())
                .flatMap(userRoleEntity -> springDataRoleRepository.findById(userRoleEntity.getRoleId()))
                .map(roleMapper::toDomain)
                .map(Role::getName)
                .collect(Collectors.toSet())
                .map(roles -> userMapper.toDomain(userEntity).toBuilder().roles(roles).build())
                .switchIfEmpty(Mono.just(userMapper.toDomain(userEntity).toBuilder().roles(Collections.emptySet()).build()));
    }
}