package com.projectArka.user_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Role {
    String id;
    String name;
}