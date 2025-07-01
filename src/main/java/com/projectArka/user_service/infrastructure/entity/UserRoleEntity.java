package com.projectArka.user_service.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("user_role")
public class UserRoleEntity {

    @Id
    @Column("id")
    private UUID id;
    @Column("user_id")
    private UUID userId;
    @Column("role_id")
    private UUID roleId;
}