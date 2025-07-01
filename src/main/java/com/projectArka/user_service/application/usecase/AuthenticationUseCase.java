package com.projectArka.user_service.application.usecase;

import com.projectArka.user_service.application.dto.AuthResponseDTO;
import com.projectArka.user_service.application.dto.LoginRequestDTO;
import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.application.mapper.IUserMapper;
import com.projectArka.user_service.application.port.out.JwtServicePort;
import com.projectArka.user_service.domain.exception.InvalidCredentialsException;
import com.projectArka.user_service.domain.exception.UserAlreadyExistsException;
import com.projectArka.user_service.domain.model.User;
import com.projectArka.user_service.domain.port.in.IAuthenticateUserPort;
import com.projectArka.user_service.domain.port.out.RoleRepositoryPort;
import com.projectArka.user_service.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthenticationUseCase implements IAuthenticateUserPort {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepositoryPort roleRepositoryPort;
    private final JwtServicePort jwtServicePort;
    private final IUserMapper userMapper;

    public Mono<AuthResponseDTO> registerUser(UserRegisterRequestDTO requestDTO) {
        Mono<Void> checkUsernameMono = userRepositoryPort.findByUsername(requestDTO.getUsername())
                .flatMap(existingUser -> Mono.<User>error(new UserAlreadyExistsException("User with username " + requestDTO.getUsername() + " already exists")))
                .then();

        Mono<Void> checkEmailMono = userRepositoryPort.findByEmail(requestDTO.getEmail())
                .flatMap(existingUser -> Mono.<User>error(new UserAlreadyExistsException("User with email " + requestDTO.getEmail() + " already exists")))
                .then();

        return Mono.when(checkUsernameMono, checkEmailMono)
                .then(Mono.defer(() -> {
                    return Mono.fromCallable(() -> passwordEncoder.encode(requestDTO.getPassword()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(encodedPassword -> {
                                final User baseUser = userMapper.toDomain(requestDTO).toBuilder()
                                        .password(encodedPassword)
                                        .active(true)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                                return roleRepositoryPort.findByName("ROLE_USER")
                                        .switchIfEmpty(Mono.error(new IllegalStateException("Default role 'ROLE_USER' not found. Please ensure it's configured.")))
                                        .flatMap(defaultRole -> {
                                            User userWithRole = baseUser.toBuilder()
                                                    .roles(Collections.singleton(defaultRole.getName()))
                                                    .build();
                                            return userRepositoryPort.save(userWithRole);
                                        });
                            });
                }))
                .flatMap(savedUser -> jwtServicePort.generateToken(savedUser)
                        .map(token -> AuthResponseDTO.builder()
                                .token(token)
                                .userId(savedUser.getId())
                                .username(savedUser.getUsername())
                                .email(savedUser.getEmail())
                                .build()));
    }

    @Override
    public Mono<User> authenticate(String username, String rawPassword) {
        return userRepositoryPort.findByUsername(username)
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid username or password")))
                .flatMap(user ->
                        Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, user.getPassword()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(matches -> {
                                    if (Boolean.TRUE.equals(matches)) {
                                        return Mono.just(user);
                                    } else {
                                        return Mono.error(new InvalidCredentialsException("Invalid username or password"));
                                    }
                                })
                );
    }

    public Mono<AuthResponseDTO> authenticateAndGenerateToken(LoginRequestDTO loginRequest) {
        return authenticate(loginRequest.getUsername(), loginRequest.getPassword())
                .flatMap(user -> jwtServicePort.generateToken(user)
                        .map(token -> AuthResponseDTO.builder()
                                .token(token)
                                .userId(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .build()));
    }
}