package com.projectArka.user_service.infrastructure.adapter.in.webflux;

import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.application.dto.UserResponseDTO;
import com.projectArka.user_service.application.dto.UserUpdateRequestDTO;
import com.projectArka.user_service.application.mapper.IUserMapper;
import com.projectArka.user_service.application.usecase.UserUseCase;
import com.projectArka.user_service.domain.exception.UserNotFoundException;
import com.projectArka.user_service.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API for managing user accounts")
public class UserController {

    private final UserUseCase userUseCase;
    private final IUserMapper userMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new user", description = "Registers a new user in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid user data supplied",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "409", description = "User with username or email already exists",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Mono<UserResponseDTO> createUser(
            @RequestBody(description = "Details of the user to be created", required = true,
                    content = @Content(schema = @Schema(implementation = UserRegisterRequestDTO.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UserRegisterRequestDTO userRegisterRequestDTO) {
        User userDomain = userMapper.toDomain(userRegisterRequestDTO);
        return userUseCase.createUser(userDomain)
                .map(userMapper::toDTO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user's details by their unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Mono<ResponseEntity<UserResponseDTO>> getUserById(
            @Parameter(description = "ID of the user to retrieve", required = true, schema = @Schema(type = "string", format = "uuid"))
            @PathVariable String id) {
        return userUseCase.getUserById(id)
                .map(userMapper::toDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/exists")
    @Operation(summary = "Check if a user exists", description = "Checks if a user with the given ID exists in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User existence checked successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Mono<Boolean> checkUserExists(
            @Parameter(description = "ID of the user to check existence for", required = true, schema = @Schema(type = "string", format = "uuid"))
            @PathVariable String userId) {
        return userUseCase.userExists(userId)
                .doOnNext(exists -> {
                    if (!exists) {
                        throw new UserNotFoundException("User with ID " + userId + " not found.");
                    }
                })
                .onErrorResume(UserNotFoundException.class, e -> Mono.just(false))
                .defaultIfEmpty(false);
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a list of all registered users.")
    @ApiResponse(responseCode = "200", description = "List of users retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class)))
    public Flux<UserResponseDTO> getAllUsers() {
        return userUseCase.getAllUsers()
                .map(userMapper::toDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user", description = "Updates details of an existing user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid user data supplied",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Mono<ResponseEntity<UserResponseDTO>> updateUser(
            @Parameter(description = "ID of the user to update", required = true, schema = @Schema(type = "string", format = "uuid"))
            @PathVariable String id,
            @RequestBody(description = "Updated user details", required = true,
                    content = @Content(schema = @Schema(implementation = UserUpdateRequestDTO.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UserUpdateRequestDTO userUpdateRequestDTO) {

        return userUseCase.getUserById(id)
                .flatMap(existingUser -> {
                    userMapper.updateDomainFromDTO(userUpdateRequestDTO, existingUser);
                    return userUseCase.updateUser(existingUser);
                })
                .map(userMapper::toDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user by ID", description = "Deletes a user from the system by their unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Mono<Void> deleteUser(
            @Parameter(description = "ID of the user to delete", required = true, schema = @Schema(type = "string", format = "uuid"))
            @PathVariable String id) {
        return userUseCase.deleteUserById(id)
                .onErrorResume(UserNotFoundException.class, Mono::error);
    }
}