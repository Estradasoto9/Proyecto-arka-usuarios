package com.projectArka.user_service.domain.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder(toBuilder = true)
public class User {
    String id;
    String username;
    String name;
    String email;
    String password;
    String phone;
    Boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    @Transient
    Set<String> roles;
}