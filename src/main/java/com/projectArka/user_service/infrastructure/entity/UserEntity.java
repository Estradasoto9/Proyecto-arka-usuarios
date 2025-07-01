package com.projectArka.user_service.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("users")
public class UserEntity {

    @Id
    @Column("id")
    private UUID id;
    @Column("username")
    private String username;
    @Column("password")
    private String password;
    @Column("name")
    private String name;
    @Column("email")
    private String email;
    @Column("phone")
    private String phone;
    @Column("active")
    private Boolean active;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Transient
    private Set<RoleEntity> roles;
}