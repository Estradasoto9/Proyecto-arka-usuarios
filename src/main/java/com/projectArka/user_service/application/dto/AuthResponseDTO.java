package com.projectArka.user_service.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponseDTO {
    String token;
    String userId;
    String username;
    String email;
}