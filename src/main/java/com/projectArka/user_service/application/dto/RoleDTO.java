package com.projectArka.user_service.application.dto;

import lombok.*;

@Value
@Builder
@AllArgsConstructor
public class RoleDTO {
    String id;
    String name;
}