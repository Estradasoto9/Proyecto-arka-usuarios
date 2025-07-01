package com.projectArka.user_service.application.usecase;

import com.projectArka.user_service.domain.port.in.ICreateUserPort;
import com.projectArka.user_service.domain.port.in.IDeleteUserPort;
import com.projectArka.user_service.domain.port.in.IGetUserPort;
import com.projectArka.user_service.domain.port.in.IUpdateUserPort;
import com.projectArka.user_service.domain.exception.UserAlreadyExistsException;
import com.projectArka.user_service.domain.exception.UserNotFoundException;
import com.projectArka.user_service.domain.model.User;
import com.projectArka.user_service.domain.port.out.RoleRepositoryPort;
import com.projectArka.user_service.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserUseCase implements ICreateUserPort, IGetUserPort, IUpdateUserPort, IDeleteUserPort {

    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<User> createUser(User user) {
        Mono<Void> checkUsernameMono = userRepositoryPort.findByUsername(user.getUsername())
                .flatMap(existingUser -> Mono.<User>error(new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists")))
                .then();

        Mono<Void> checkEmailMono = userRepositoryPort.findByEmail(user.getEmail())
                .flatMap(existingUser -> Mono.<User>error(new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists")))
                .then();

        return Mono.when(checkUsernameMono, checkEmailMono)
                .then(Mono.defer(() -> {
                    return Mono.fromCallable(() -> passwordEncoder.encode(user.getPassword()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(encodedPassword -> {
                                return roleRepositoryPort.findByName("ROLE_USER")
                                        .switchIfEmpty(
                                                Mono.error(new IllegalStateException("Default role 'ROLE_USER' not found. Please ensure it's configured."))
                                        )
                                        .flatMap(defaultRole -> {
                                            LocalDateTime now = LocalDateTime.now();
                                            User newUser = user.toBuilder()
                                                    .password(encodedPassword)
                                                    .active(true)
                                                    .createdAt(now)
                                                    .updatedAt(now)
                                                    .roles(Collections.singleton(defaultRole.getName()))
                                                    .build();
                                            return userRepositoryPort.save(newUser);
                                        });
                            });
                }));
    }

    @Override
    public Mono<User> getUserById(String id) {
        return userRepositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User with ID " + id + " not found")));
    }

    @Override
    public Mono<User> getUserByUsername(String username) {
        return userRepositoryPort.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User with username " + username + " not found")));
    }

    @Override
    public Flux<User> getAllUsers() {
        return userRepositoryPort.findAll();
    }

    @Override
    public Mono<User> updateUser(User user) {
        if (user.getId() == null) {
            return Mono.error(new IllegalArgumentException("User ID cannot be null for update operation"));
        }

        return userRepositoryPort.findById(user.getId())
                .switchIfEmpty(Mono.error(new UserNotFoundException("User with ID " + user.getId() + " not found for update")))
                .flatMap(existingUser -> {
                    Mono<String> encodedPasswordMono;
                    if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                        encodedPasswordMono = Mono.fromCallable(() -> passwordEncoder.encode(user.getPassword()))
                                .subscribeOn(Schedulers.boundedElastic());
                    } else {
                        encodedPasswordMono = Mono.just(existingUser.getPassword());
                    }

                    return encodedPasswordMono.flatMap(finalEncodedPassword -> {
                        User.UserBuilder updatedUserBuilder = existingUser.toBuilder();

                        if (user.getName() != null) updatedUserBuilder.name(user.getName());
                        if (user.getEmail() != null) updatedUserBuilder.email(user.getEmail());
                        updatedUserBuilder.password(finalEncodedPassword);
                        if (user.getPhone() != null) updatedUserBuilder.phone(user.getPhone());
                        if (user.getActive() != null) updatedUserBuilder.active(user.getActive());

                        if (user.getRoles() != null) {
                            updatedUserBuilder.roles(user.getRoles());
                        } else {
                            updatedUserBuilder.roles(existingUser.getRoles());
                        }

                        updatedUserBuilder.updatedAt(LocalDateTime.now());

                        return userRepositoryPort.save(updatedUserBuilder.build());
                    });
                });
    }

    @Override
    public Mono<Void> deleteUserById(String id) {
        return userRepositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User with ID " + id + " not found for deletion")))
                .flatMap(user -> userRepositoryPort.deleteById(id))
                .onErrorMap(ex -> {
                    if (ex instanceof UserNotFoundException) {
                        return ex;
                    }
                    return new RuntimeException("Failed to delete user", ex);
                });
    }

    public Mono<Boolean> userExists(String userId) {
        return userRepositoryPort.findById(userId)
                .hasElement();
    }
}