package com.projectArka.user_service.ControllerTest;

import com.projectArka.user_service.application.dto.AuthResponseDTO;
import com.projectArka.user_service.application.dto.LoginRequestDTO;
import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.application.usecase.AuthenticationUseCase;
import com.projectArka.user_service.infrastructure.adapter.in.webflux.AuthController;
import com.projectArka.user_service.domain.exception.UserAlreadyExistsException;
import com.projectArka.user_service.domain.exception.InvalidCredentialsException;
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
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @InjectMocks
    private AuthController authController;

    private WebTestClient webTestClient;

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();


    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToController(authController)
                .controllerAdvice(globalExceptionHandler)
                .build();
    }

    @Test
    void registerUser_shouldReturnCreatedStatusAndAuthResponseDTO() {
        UserRegisterRequestDTO requestDTO = new UserRegisterRequestDTO(
                "tester",
                "Test Name",
                "test@example.com",
                "password123",
                "1234567890"
        );

        AuthResponseDTO expectedResponse = AuthResponseDTO.builder()
                .token("mockedAccessToken")
                .userId(UUID.randomUUID().toString())
                .username(requestDTO.getUsername())
                .email(requestDTO.getEmail())
                .build();

        when(authenticationUseCase.registerUser(any(UserRegisterRequestDTO.class)))
                .thenReturn(Mono.just(expectedResponse));

        webTestClient.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponseDTO.class)
                .value(response -> {
                    assertThat(response.getToken()).isEqualTo("mockedAccessToken");
                    assertThat(response.getUsername()).isEqualTo("tester");
                    assertThat(response.getEmail()).isEqualTo("test@example.com");
                    assertThat(response.getUserId()).isNotNull();
                });
    }

    @Test
    void loginUser_shouldReturnOkStatusAndAuthResponseDTO() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("login user", "secure password");
        AuthResponseDTO expectedResponse = AuthResponseDTO.builder()
                .token("mockedLoginAccessToken")
                .userId(UUID.randomUUID().toString())
                .username("login user")
                .email("login@example.com")
                .build();

        when(authenticationUseCase.authenticateAndGenerateToken(any(LoginRequestDTO.class)))
                .thenReturn(Mono.just(expectedResponse));

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequestDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDTO.class)
                .value(response -> {
                    assertThat(response.getToken()).isEqualTo("mockedLoginAccessToken");
                    assertThat(response.getUsername()).isEqualTo("login user");
                    assertThat(response.getEmail()).isEqualTo("login@example.com");
                    assertThat(response.getUserId()).isNotNull();
                });
    }

    @Test
    void registerUser_shouldReturnConflictForUserAlreadyExists() {
        UserRegisterRequestDTO requestDTO = new UserRegisterRequestDTO(
                "existing user", "Existing Name", "existing@example.com", "password", "0987654321");

        when(authenticationUseCase.registerUser(any(UserRegisterRequestDTO.class)))
                .thenReturn(Mono.error(new UserAlreadyExistsException("User with username existing user already exists")));

        webTestClient.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.message").isEqualTo("User with username existing user already exists");
    }

    @Test
    void loginUser_shouldReturnUnauthorizedForInvalidCredentials() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("nonexistent", "wrong password");

        when(authenticationUseCase.authenticateAndGenerateToken(any(LoginRequestDTO.class)))
                .thenReturn(Mono.error(new InvalidCredentialsException("Invalid username or password")));

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequestDTO)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid username or password");
    }
}