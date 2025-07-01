package com.projectArka.user_service.UseCaseTest;

import com.projectArka.user_service.application.usecase.UserUseCase;
import com.projectArka.user_service.domain.exception.UserAlreadyExistsException;
import com.projectArka.user_service.domain.exception.UserNotFoundException;
import com.projectArka.user_service.domain.model.Role;
import com.projectArka.user_service.domain.model.User;
import com.projectArka.user_service.domain.port.out.RoleRepositoryPort;
import com.projectArka.user_service.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private RoleRepositoryPort roleRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserUseCase userUseCase;

    private User testUser;
    private Role testRole;
    private final String USER_ID = UUID.randomUUID().toString();
    private final String ROLE_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        testRole = new Role(ROLE_ID, "ROLE_USER");
        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .name("Test User")
                .email("test@example.com")
                .password("raw_password")
                .phone("1234567890")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(new HashSet<>(Collections.singletonList(testRole.getName())))
                .build();
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() {
        when(userRepositoryPort.findByUsername(anyString())).thenReturn(Mono.empty());
        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Mono.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(roleRepositoryPort.findByName("ROLE_USER")).thenReturn(Mono.just(testRole));
        when(userRepositoryPort.save(any(User.class))).thenReturn(Mono.just(testUser.toBuilder().password("encoded_password").build()));

        StepVerifier.create(userUseCase.createUser(testUser))
                .expectNextMatches(user ->
                        user.getUsername().equals(testUser.getUsername()) &&
                                user.getPassword().equals("encoded_password") &&
                                user.getRoles().contains("ROLE_USER")
                )
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findByUsername(testUser.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(testUser.getEmail());
        verify(passwordEncoder, times(1)).encode(testUser.getPassword());
        verify(roleRepositoryPort, times(1)).findByName("ROLE_USER");
        verify(userRepositoryPort, times(1)).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowUserAlreadyExistsException_whenUsernameExists() {

        when(userRepositoryPort.findByUsername(testUser.getUsername())).thenReturn(Mono.just(testUser));
        when(userRepositoryPort.findByEmail(testUser.getEmail())).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.createUser(testUser))
                .expectErrorMatches(e -> e instanceof UserAlreadyExistsException &&
                        e.getMessage().contains("username " + testUser.getUsername()))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(testUser.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(testUser.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(roleRepositoryPort, never()).findByName(anyString());
        verify(userRepositoryPort, never()).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowUserAlreadyExistsException_whenEmailExists() {
        when(userRepositoryPort.findByUsername(anyString())).thenReturn(Mono.empty());
        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Mono.just(testUser));

        StepVerifier.create(userUseCase.createUser(testUser))
                .expectErrorMatches(e -> e instanceof UserAlreadyExistsException &&
                        e.getMessage().contains("email " + testUser.getEmail()))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(testUser.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(testUser.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(roleRepositoryPort, never()).findByName(anyString());
        verify(userRepositoryPort, never()).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowIllegalStateException_whenDefaultRoleNotFound() {
        when(userRepositoryPort.findByUsername(anyString())).thenReturn(Mono.empty());
        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Mono.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(roleRepositoryPort.findByName("ROLE_USER")).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.createUser(testUser))
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("Default role 'ROLE_USER' not found"))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(testUser.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(testUser.getEmail());
        verify(passwordEncoder, times(1)).encode(testUser.getPassword());
        verify(roleRepositoryPort, times(1)).findByName("ROLE_USER");
        verify(userRepositoryPort, never()).save(any(User.class));
    }

    @Test
    void getUserById_shouldReturnUser_whenFound() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.just(testUser));

        StepVerifier.create(userUseCase.getUserById(USER_ID))
                .expectNext(testUser)
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findById(USER_ID);
    }

    @Test
    void getUserById_shouldThrowUserNotFoundException_whenNotFound() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.getUserById(USER_ID))
                .expectErrorMatches(e -> e instanceof UserNotFoundException &&
                        e.getMessage().contains("User with ID " + USER_ID + " not found"))
                .verify();

        verify(userRepositoryPort, times(1)).findById(USER_ID);
    }

    @Test
    void getUserByUsername_shouldReturnUser_whenFound() {
        when(userRepositoryPort.findByUsername(testUser.getUsername())).thenReturn(Mono.just(testUser));

        StepVerifier.create(userUseCase.getUserByUsername(testUser.getUsername()))
                .expectNext(testUser)
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findByUsername(testUser.getUsername());
    }

    @Test
    void getUserByUsername_shouldThrowUserNotFoundException_whenNotFound() {
        when(userRepositoryPort.findByUsername(testUser.getUsername())).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.getUserByUsername(testUser.getUsername()))
                .expectErrorMatches(e -> e instanceof UserNotFoundException &&
                        e.getMessage().contains("User with username " + testUser.getUsername() + " not found"))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(testUser.getUsername());
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        User user1 = User.builder().id("1").username("user1").email("u1@e.com").build();
        User user2 = User.builder().id("2").username("user2").email("u2@e.com").build();

        when(userRepositoryPort.findAll()).thenReturn(Flux.just(user1, user2));

        StepVerifier.create(userUseCase.getAllUsers())
                .expectNext(user1, user2)
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findAll();
    }

    @Test
    void getAllUsers_shouldReturnEmptyFlux_whenNoUsers() {
        when(userRepositoryPort.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(userUseCase.getAllUsers())
                .expectNextCount(0)
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findAll();
    }

    @Test
    void updateUser_shouldUpdateUserSuccessfully_withNewPassword() {
        User existingUser = testUser.toBuilder().password("old_encoded_password").build();
        User updatedUser = testUser.toBuilder().password("new_encoded_password").email("updated@example.com").build();
        User userToUpdate = testUser.toBuilder().password("new_raw_password").email("updated@example.com").build();

        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.just(existingUser));
        when(passwordEncoder.encode("new_raw_password")).thenReturn("new_encoded_password");
        when(userRepositoryPort.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        StepVerifier.create(userUseCase.updateUser(userToUpdate))
                .expectNextMatches(user ->
                        user.getEmail().equals("updated@example.com") &&
                                user.getPassword().equals("new_encoded_password")
                )
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).encode("new_raw_password");
        verify(userRepositoryPort, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_shouldUpdateUserSuccessfully_withoutNewPassword() {
        User existingUser = testUser.toBuilder().password("existing_encoded_password").build();
        User updatedUser = testUser.toBuilder().email("updated@example.com").password("existing_encoded_password").build();
        User userToUpdate = testUser.toBuilder().password(null).email("updated@example.com").build();

        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.just(existingUser));
        when(userRepositoryPort.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        StepVerifier.create(userUseCase.updateUser(userToUpdate))
                .expectNextMatches(user ->
                        user.getEmail().equals("updated@example.com") &&
                                user.getPassword().equals("existing_encoded_password")
                )
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findById(USER_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepositoryPort, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        User userToUpdate = testUser.toBuilder().id("nonexistent").build();

        when(userRepositoryPort.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.updateUser(userToUpdate))
                .expectErrorMatches(e -> e instanceof UserNotFoundException &&
                        e.getMessage().contains("User with ID nonexistent not found for update"))
                .verify();

        verify(userRepositoryPort, times(1)).findById("nonexistent");
        verify(userRepositoryPort, never()).save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentException_whenIdIsNull() {
        User userWithNullId = testUser.toBuilder().id(null).build();

        StepVerifier.create(userUseCase.updateUser(userWithNullId))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("User ID cannot be null for update operation"))
                .verify();

        verify(userRepositoryPort, never()).findById(anyString());
        verify(userRepositoryPort, never()).save(any(User.class));
    }

    @Test
    void deleteUserById_shouldDeleteUserSuccessfully() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.just(testUser));
        when(userRepositoryPort.deleteById(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.deleteUserById(USER_ID))
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findById(USER_ID);
        verify(userRepositoryPort, times(1)).deleteById(USER_ID);
    }

    @Test
    void deleteUserById_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.deleteUserById(USER_ID))
                .expectErrorMatches(e -> e instanceof UserNotFoundException &&
                        e.getMessage().contains("User with ID " + USER_ID + " not found for deletion"))
                .verify();

        verify(userRepositoryPort, times(1)).findById(USER_ID);
        verify(userRepositoryPort, never()).deleteById(anyString());
    }

    @Test
    void deleteUserById_shouldWrapRepositoryError() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.just(testUser));
        when(userRepositoryPort.deleteById(USER_ID)).thenReturn(Mono.error(new RuntimeException("DB error")));

        StepVerifier.create(userUseCase.deleteUserById(USER_ID))
                .expectErrorMatches(e -> e instanceof RuntimeException &&
                        e.getMessage().contains("Failed to delete user"))
                .verify();

        verify(userRepositoryPort, times(1)).findById(USER_ID);
        verify(userRepositoryPort, times(1)).deleteById(USER_ID);
    }
}