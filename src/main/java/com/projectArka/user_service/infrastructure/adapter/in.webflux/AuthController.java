package com.projectArka.user_service.infrastructure.adapter.in.webflux;

import com.projectArka.user_service.application.dto.LoginRequestDTO;
import com.projectArka.user_service.application.dto.AuthResponseDTO;
import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.application.usecase.AuthenticationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

// Importaciones de Swagger
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Usamos este RequestBody para Swagger, no el de Spring si hay conflicto
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Management", description = "API for user registration and login")
public class AuthController {

    private final AuthenticationUseCase authUseCase;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", description = "Registers a new user and returns an authentication token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid registration data supplied (e.g., missing fields, invalid format)",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "409", description = "User with provided email or username already exists",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Mono<AuthResponseDTO> registerUser(
            @RequestBody(description = "User registration details", required = true,
                    content = @Content(schema = @Schema(implementation = UserRegisterRequestDTO.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UserRegisterRequestDTO userRegisterRequestDTO) {
        return authUseCase.registerUser(userRegisterRequestDTO);
    }

    @PostMapping("/login")
    @Operation(summary = "Login an existing user", description = "Authenticates a user and returns an authentication token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged in successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid login credentials (e.g., missing fields, invalid format)",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Invalid username or password)",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Mono<AuthResponseDTO> loginUser(
            @RequestBody(description = "User login credentials", required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDTO.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequestDTO loginRequestDTO) {
        return authUseCase.authenticateAndGenerateToken(loginRequestDTO);
    }
}