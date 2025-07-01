package com.projectArka.user_service.ControllerTest;

import com.projectArka.user_service.application.dto.RoleDTO;
import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.application.dto.UserResponseDTO;
import com.projectArka.user_service.application.dto.UserUpdateRequestDTO;
import com.projectArka.user_service.application.mapper.IUserMapper;
import com.projectArka.user_service.application.usecase.UserUseCase;
import com.projectArka.user_service.domain.exception.UserAlreadyExistsException;
import com.projectArka.user_service.domain.exception.UserNotFoundException;
import com.projectArka.user_service.domain.model.User;
import com.projectArka.user_service.infrastructure.adapter.in.webflux.UserController;
import com.projectArka.user_service.infrastructure.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private UserUseCase userUseCase;

    @Mock
    private IUserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();


    private final String USER_ID = UUID.randomUUID().toString();
    private User testUser;
    private UserResponseDTO testUserResponseDTO;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(userController)
                .controllerAdvice(globalExceptionHandler)
                .build();

        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .name("Test User")
                .email("test@example.com")
                .password("encoded_password")
                .phone("1234567890")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(new HashSet<>(Collections.singletonList("ROLE_USER")))
                .build();

        testUserResponseDTO = UserResponseDTO.builder()
                .id(USER_ID)
                .username("testuser")
                .name("Test User")
                .email("test@example.com")
                .phone("1234567890")
                .active(true)
                .createdAt(testUser.getCreatedAt())
                .updatedAt(testUser.getUpdatedAt())
                .roles(new HashSet<>(Collections.singletonList(RoleDTO.builder().id(UUID.randomUUID().toString()).name("ROLE_USER").build())))
                .build();
    }

    @Test
    void createUser_shouldReturnCreatedUser() {
        UserRegisterRequestDTO requestDTO = new UserRegisterRequestDTO(
                "newuser", "New User", "new@example.com", "rawpassword", "1122334455");

        User userDomainToCreate = testUser.toBuilder()
                .id(null)
                .username("newuser")
                .email("new@example.com")
                .password("rawpassword")
                .build();

        User createdUser = userDomainToCreate.toBuilder()
                .id(UUID.randomUUID().toString())
                .password("encoded_password_from_usecase")
                .build();

        UserResponseDTO createdUserResponseDTO = testUserResponseDTO.toBuilder()
                .id(createdUser.getId())
                .username("newuser")
                .email("new@example.com")
                .build();

        when(userMapper.toDomain(any(UserRegisterRequestDTO.class))).thenReturn(userDomainToCreate);
        when(userUseCase.createUser(any(User.class))).thenReturn(Mono.just(createdUser));
        when(userMapper.toDTO(any(User.class))).thenReturn(createdUserResponseDTO);

        webTestClient.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserResponseDTO.class)
                .consumeWith(response -> {
                    UserResponseDTO responseBody = response.getResponseBody();
                    assert responseBody != null;
                    assert responseBody.getId() != null;
                    assert responseBody.getUsername().equals("newuser");
                    assert responseBody.getEmail().equals("new@example.com");
                });

        verify(userMapper, times(1)).toDomain(requestDTO);
        verify(userUseCase, times(1)).createUser(any(User.class));
        verify(userMapper, times(1)).toDTO(createdUser);
    }

    @Test
    void createUser_shouldReturnConflict_whenUserAlreadyExists() {
        UserRegisterRequestDTO requestDTO = new UserRegisterRequestDTO(
                "existinguser", "Existing User", "existing@example.com", "password", "1122334455");

        User userDomainFromRequest = testUser.toBuilder().id(null).username("existinguser").build();

        when(userMapper.toDomain(any(UserRegisterRequestDTO.class))).thenReturn(userDomainFromRequest);
        when(userUseCase.createUser(any(User.class)))
                .thenReturn(Mono.error(new UserAlreadyExistsException("User with username existinguser already exists")));

        webTestClient.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        verify(userMapper, times(1)).toDomain(requestDTO);
        verify(userUseCase, times(1)).createUser(any(User.class));
        verify(userMapper, never()).toDTO(any(User.class));
    }

    @Test
    void getUserById_shouldReturnUser() {
        when(userUseCase.getUserById(USER_ID)).thenReturn(Mono.just(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserResponseDTO);

        webTestClient.get().uri("/api/users/{id}", USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponseDTO.class)
                .isEqualTo(testUserResponseDTO);

        verify(userUseCase, times(1)).getUserById(USER_ID);
        verify(userMapper, times(1)).toDTO(testUser);
    }

    @Test
    void getUserById_shouldReturnNotFound() {
        when(userUseCase.getUserById(USER_ID)).thenReturn(Mono.error(new UserNotFoundException("User not found")));

        webTestClient.get().uri("/api/users/{id}", USER_ID)
                .exchange()
                .expectStatus().isNotFound();

        verify(userUseCase, times(1)).getUserById(USER_ID);
        verify(userMapper, never()).toDTO(any(User.class));
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        User user1 = testUser.toBuilder().id("id1").username("user1").email("u1@e.com").build();
        User user2 = testUser.toBuilder().id("id2").username("user2").email("u2@e.com").build();

        UserResponseDTO userResponseDTO1 = testUserResponseDTO.toBuilder().id("id1").username("user1").email("u1@e.com").build();
        UserResponseDTO userResponseDTO2 = testUserResponseDTO.toBuilder().id("id2").username("user2").email("u2@e.com").build();

        when(userUseCase.getAllUsers()).thenReturn(Flux.just(user1, user2));
        when(userMapper.toDTO(user1)).thenReturn(userResponseDTO1);
        when(userMapper.toDTO(user2)).thenReturn(userResponseDTO2);

        webTestClient.get().uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserResponseDTO.class)
                .contains(userResponseDTO1, userResponseDTO2);

        verify(userUseCase, times(1)).getAllUsers();
        verify(userMapper, times(2)).toDTO(any(User.class));
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenNoUsers() {
        when(userUseCase.getAllUsers()).thenReturn(Flux.empty());

        webTestClient.get().uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserResponseDTO.class)
                .hasSize(0);

        verify(userUseCase, times(1)).getAllUsers();
        verify(userMapper, never()).toDTO(any(User.class));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() {
        UserUpdateRequestDTO updateRequestDTO = UserUpdateRequestDTO.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phone("0987654321")
                .active(false)
                .build();

        User existingUserInDomain = testUser.toBuilder().build();

        User updatedUserDomain = existingUserInDomain.toBuilder()
                .name(updateRequestDTO.getName())
                .email(updateRequestDTO.getEmail())
                .phone(updateRequestDTO.getPhone())
                .active(updateRequestDTO.getActive())
                .updatedAt(LocalDateTime.now())
                .build();

        UserResponseDTO updatedUserResponseDTO = testUserResponseDTO.toBuilder()
                .name(updateRequestDTO.getName())
                .email(updateRequestDTO.getEmail())
                .phone(updateRequestDTO.getPhone())
                .active(updateRequestDTO.getActive())
                .build();

        when(userUseCase.getUserById(USER_ID)).thenReturn(Mono.just(existingUserInDomain));
        when(userUseCase.updateUser(any(User.class))).thenReturn(Mono.just(updatedUserDomain));
        when(userMapper.toDTO(updatedUserDomain)).thenReturn(updatedUserResponseDTO);

        webTestClient.put().uri("/api/users/{id}", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponseDTO.class)
                .consumeWith(response -> {
                    UserResponseDTO responseBody = response.getResponseBody();
                    assert responseBody != null;
                    assert responseBody.getId().equals(USER_ID);
                    assert responseBody.getName().equals(updateRequestDTO.getName());
                    assert responseBody.getEmail().equals(updateRequestDTO.getEmail());
                    assert responseBody.getPhone().equals(updateRequestDTO.getPhone());
                    assert responseBody.getActive().equals(updateRequestDTO.getActive());
                });

        verify(userUseCase, times(1)).getUserById(USER_ID);
        verify(userMapper, times(1)).updateDomainFromDTO(eq(updateRequestDTO), any(User.class));
        verify(userUseCase, times(1)).updateUser(any(User.class));
        verify(userMapper, times(1)).toDTO(updatedUserDomain);
    }

    @Test
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() {
        UserUpdateRequestDTO updateRequestDTO = UserUpdateRequestDTO.builder()
                .name("NonExistent Update")
                .build();

        when(userUseCase.getUserById(USER_ID)).thenReturn(Mono.empty());

        webTestClient.put().uri("/api/users/{id}", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequestDTO)
                .exchange()
                .expectStatus().isNotFound();

        verify(userUseCase, times(1)).getUserById(USER_ID);
        verify(userMapper, never()).updateDomainFromDTO(any(UserUpdateRequestDTO.class), any(User.class));
        verify(userUseCase, never()).updateUser(any(User.class));
        verify(userMapper, never()).toDTO(any(User.class));
    }

    @Test
    void deleteUser_shouldReturnNoContent() {
        when(userUseCase.deleteUserById(USER_ID)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/users/{id}", USER_ID)
                .exchange()
                .expectStatus().isNoContent();

        verify(userUseCase, times(1)).deleteUserById(USER_ID);
    }

    @Test
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() {
        when(userUseCase.deleteUserById(USER_ID)).thenReturn(Mono.error(new UserNotFoundException("User not found for deletion")));

        webTestClient.delete().uri("/api/users/{id}", USER_ID)
                .exchange()
                .expectStatus().isNotFound();

        verify(userUseCase, times(1)).deleteUserById(USER_ID);
    }
}