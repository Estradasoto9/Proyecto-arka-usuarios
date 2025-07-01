package com.projectArka.user_service.domain.port.in;

import com.projectArka.user_service.application.dto.AuthResponseDTO;
import com.projectArka.user_service.application.dto.LoginRequestDTO;
import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.domain.model.User;

import reactor.core.publisher.Mono;

public interface IAuthenticateUserPort {
    Mono<User> authenticate(String username, String rawPassword);
    Mono<AuthResponseDTO> registerUser(UserRegisterRequestDTO requestDTO);
    Mono<AuthResponseDTO> authenticateAndGenerateToken(LoginRequestDTO loginRequest);
}