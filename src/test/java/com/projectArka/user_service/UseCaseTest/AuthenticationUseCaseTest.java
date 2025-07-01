package com.projectArka.user_service.UseCaseTest;

import com.projectArka.user_service.application.dto.LoginRequestDTO;
import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.application.mapper.IUserMapper;
import com.projectArka.user_service.application.port.out.JwtServicePort;
import com.projectArka.user_service.application.usecase.AuthenticationUseCase;
import com.projectArka.user_service.domain.exception.InvalidCredentialsException;
import com.projectArka.user_service.domain.exception.UserAlreadyExistsException;
import com.projectArka.user_service.domain.model.Role;
import com.projectArka.user_service.domain.model.User;
import com.projectArka.user_service.domain.port.out.RoleRepositoryPort;
import com.projectArka.user_service.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AuthenticationUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepositoryPort roleRepositoryPort;

    @Mock
    private JwtServicePort jwtServicePort;

    @Mock
    private IUserMapper userMapper;
    @InjectMocks
    private AuthenticationUseCase authenticationUseCase;

    private User testUser;
    private Role testRole;
    private UserRegisterRequestDTO registerRequestDTO;
    private LoginRequestDTO loginRequestDTO;
    private final String USER_ID = UUID.randomUUID().toString();
    private final String ROLE_ID = UUID.randomUUID().toString();
    private final String MOCKED_TOKEN = "mocked.jwt.token";

    @BeforeEach
    void setUp() {
        testRole = new Role(ROLE_ID, "ROLE_USER");
        testUser = User.builder()
                .id(USER_ID)
                .username("tester")
                .name("Test User")
                .email("test@example.com")
                .password("encoded_password")
                .phone("1234567890")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(new HashSet<>(Collections.singletonList(testRole.getName())))
                .build();

        registerRequestDTO = new UserRegisterRequestDTO(
                "new user", "New User", "new@example.com", "password", "1122334455");

        loginRequestDTO = new LoginRequestDTO(
                "tester", "raw_password");
    }

    @Test
    void registerUser_shouldCreateUserAndReturnAuthResponseSuccessfully() {
        User userMappedFromDto = User.builder()
                .username(registerRequestDTO.getUsername())
                .name(registerRequestDTO.getName())
                .email(registerRequestDTO.getEmail())
                .password(registerRequestDTO.getPassword())
                .phone(registerRequestDTO.getPhone())
                .active(true)
                .build();

        when(userRepositoryPort.findByUsername(anyString())).thenReturn(Mono.empty());
        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Mono.empty());
        when(passwordEncoder.encode(registerRequestDTO.getPassword())).thenReturn("encoded_rawpassword");

        when(userMapper.toDomain(any(UserRegisterRequestDTO.class))).thenReturn(userMappedFromDto);

        when(roleRepositoryPort.findByName("ROLE_USER")).thenReturn(Mono.just(testRole));

        ArgumentCaptor<User> userToSaveCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepositoryPort.save(userToSaveCaptor.capture()))
                .thenReturn(Mono.defer(() -> {
                    User userSavedByUseCase = userToSaveCaptor.getValue();
                    return Mono.just(userSavedByUseCase.toBuilder().id(USER_ID).build());
                }));

        when(jwtServicePort.generateToken(any(User.class))).thenReturn(Mono.just(MOCKED_TOKEN));

        StepVerifier.create(authenticationUseCase.registerUser(registerRequestDTO))
                .expectNextMatches(response ->
                        response.getToken().equals(MOCKED_TOKEN) &&
                                response.getUserId().equals(USER_ID) &&
                                response.getUsername().equals(registerRequestDTO.getUsername()) &&
                                response.getEmail().equals(registerRequestDTO.getEmail())
                )
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findByUsername(registerRequestDTO.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(registerRequestDTO.getEmail());
        verify(passwordEncoder, times(1)).encode(registerRequestDTO.getPassword());
        verify(userMapper, times(1)).toDomain(registerRequestDTO);
        verify(roleRepositoryPort, times(1)).findByName("ROLE_USER");

        User actualUserSaved = userToSaveCaptor.getValue();
        verify(userRepositoryPort, times(1)).save(actualUserSaved);

        verify(jwtServicePort, times(1)).generateToken(actualUserSaved.toBuilder().id(USER_ID).build());

        assert actualUserSaved.getPassword().equals("encoded_rawpassword");
        assert actualUserSaved.getRoles().contains("ROLE_USER");
        assert actualUserSaved.getCreatedAt() != null;
        assert actualUserSaved.getUpdatedAt() != null;
    }

    @Test
    void registerUser_shouldThrowUserAlreadyExistsException_whenUsernameExists() {
        when(userRepositoryPort.findByUsername(registerRequestDTO.getUsername())).thenReturn(Mono.just(testUser));

        when(userRepositoryPort.findByEmail(registerRequestDTO.getEmail())).thenReturn(Mono.empty());

        StepVerifier.create(authenticationUseCase.registerUser(registerRequestDTO))
                .expectErrorMatches(e -> e instanceof UserAlreadyExistsException &&
                        e.getMessage().contains("username " + registerRequestDTO.getUsername()))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(registerRequestDTO.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(registerRequestDTO.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).toDomain(any(UserRegisterRequestDTO.class));
        verify(roleRepositoryPort, never()).findByName(anyString());
        verify(userRepositoryPort, never()).save(any(User.class));
        verify(jwtServicePort, never()).generateToken(any(User.class));
    }

    @Test
    void registerUser_shouldThrowUserAlreadyExistsException_whenEmailExists() {
        when(userRepositoryPort.findByUsername(anyString())).thenReturn(Mono.empty());
        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Mono.just(testUser));

        StepVerifier.create(authenticationUseCase.registerUser(registerRequestDTO))
                .expectErrorMatches(e -> e instanceof UserAlreadyExistsException &&
                        e.getMessage().contains("email " + registerRequestDTO.getEmail()))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(registerRequestDTO.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(registerRequestDTO.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).toDomain(any(UserRegisterRequestDTO.class));
        verify(roleRepositoryPort, never()).findByName(anyString());
        verify(userRepositoryPort, never()).save(any(User.class));
        verify(jwtServicePort, never()).generateToken(any(User.class));
    }

    @Test
    void registerUser_shouldThrowIllegalStateException_whenDefaultRoleNotFound() {
        User userMappedFromDto = User.builder()
                .username(registerRequestDTO.getUsername())
                .name(registerRequestDTO.getName())
                .email(registerRequestDTO.getEmail())
                .password(registerRequestDTO.getPassword())
                .phone(registerRequestDTO.getPhone())
                .active(true)
                .build();

        when(userRepositoryPort.findByUsername(anyString())).thenReturn(Mono.empty());
        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Mono.empty());
        when(passwordEncoder.encode(registerRequestDTO.getPassword())).thenReturn("encoded_rawpassword");
        when(userMapper.toDomain(any(UserRegisterRequestDTO.class))).thenReturn(userMappedFromDto);
        when(roleRepositoryPort.findByName("ROLE_USER")).thenReturn(Mono.empty());

        StepVerifier.create(authenticationUseCase.registerUser(registerRequestDTO))
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("Default role 'ROLE_USER' not found"))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(registerRequestDTO.getUsername());
        verify(userRepositoryPort, times(1)).findByEmail(registerRequestDTO.getEmail());
        verify(passwordEncoder, times(1)).encode(registerRequestDTO.getPassword());
        verify(userMapper, times(1)).toDomain(registerRequestDTO);
        verify(roleRepositoryPort, times(1)).findByName("ROLE_USER");
        verify(userRepositoryPort, never()).save(any(User.class));
        verify(jwtServicePort, never()).generateToken(any(User.class));
    }


    @Test
    void authenticate_shouldReturnUser_whenCredentialsAreValid() {
        when(userRepositoryPort.findByUsername(loginRequestDTO.getUsername())).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword())).thenReturn(true);

        StepVerifier.create(authenticationUseCase.authenticate(loginRequestDTO.getUsername(), loginRequestDTO.getPassword()))
                .expectNext(testUser)
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findByUsername(loginRequestDTO.getUsername());
        verify(passwordEncoder, times(1)).matches(loginRequestDTO.getPassword(), testUser.getPassword());
    }

    @Test
    void authenticate_shouldThrowInvalidCredentialsException_whenUserNotFound() {
        when(userRepositoryPort.findByUsername(loginRequestDTO.getUsername())).thenReturn(Mono.empty());

        StepVerifier.create(authenticationUseCase.authenticate(loginRequestDTO.getUsername(), loginRequestDTO.getPassword()))
                .expectErrorMatches(e -> e instanceof InvalidCredentialsException &&
                        e.getMessage().contains("Invalid username or password"))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(loginRequestDTO.getUsername());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticate_shouldThrowInvalidCredentialsException_whenPasswordMismatch() {
        when(userRepositoryPort.findByUsername(loginRequestDTO.getUsername())).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword())).thenReturn(false);

        StepVerifier.create(authenticationUseCase.authenticate(loginRequestDTO.getUsername(), loginRequestDTO.getPassword()))
                .expectErrorMatches(e -> e instanceof InvalidCredentialsException &&
                        e.getMessage().contains("Invalid username or password"))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(loginRequestDTO.getUsername());
        verify(passwordEncoder, times(1)).matches(loginRequestDTO.getPassword(), testUser.getPassword());
    }


    @Test
    void authenticateAndGenerateToken_shouldReturnAuthResponseSuccessfully() {
        when(userRepositoryPort.findByUsername(loginRequestDTO.getUsername())).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtServicePort.generateToken(testUser)).thenReturn(Mono.just(MOCKED_TOKEN));

        StepVerifier.create(authenticationUseCase.authenticateAndGenerateToken(loginRequestDTO))
                .expectNextMatches(response ->
                        response.getToken().equals(MOCKED_TOKEN) &&
                                response.getUserId().equals(USER_ID) &&
                                response.getUsername().equals(testUser.getUsername()) &&
                                response.getEmail().equals(testUser.getEmail())
                )
                .verifyComplete();

        verify(userRepositoryPort, times(1)).findByUsername(loginRequestDTO.getUsername());
        verify(passwordEncoder, times(1)).matches(loginRequestDTO.getPassword(), testUser.getPassword());
        verify(jwtServicePort, times(1)).generateToken(testUser);
    }

    @Test
    void authenticateAndGenerateToken_shouldPropagateInvalidCredentialsException() {
        when(userRepositoryPort.findByUsername(loginRequestDTO.getUsername())).thenReturn(Mono.empty());

        StepVerifier.create(authenticationUseCase.authenticateAndGenerateToken(loginRequestDTO))
                .expectErrorMatches(e -> e instanceof InvalidCredentialsException &&
                        e.getMessage().contains("Invalid username or password"))
                .verify();

        verify(userRepositoryPort, times(1)).findByUsername(loginRequestDTO.getUsername());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtServicePort, never()).generateToken(any(User.class));
    }
}