package com.projectArka.user_service.application.dto;

import jdk.jshell.Snippet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor

public class UserResponseDTO {
    String id;
    String username;
    String name;
    String email;
    String phone;
    Boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Set<RoleDTO> roles;


}