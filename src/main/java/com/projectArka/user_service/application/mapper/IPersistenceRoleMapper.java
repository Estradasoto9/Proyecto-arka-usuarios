package com.projectArka.user_service.application.mapper;

import com.projectArka.user_service.domain.model.Role;
import com.projectArka.user_service.infrastructure.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface IPersistenceRoleMapper {

    @Mapping(target = "id", expression = "java(role.getId() != null && !role.getId().isEmpty() ? java.util.UUID.fromString(role.getId()) : null)")
    RoleEntity toEntity(Role role);

    @Mapping(target = "id", expression = "java(roleEntity.getId() != null ? roleEntity.getId().toString() : null)")
    Role toDomain(RoleEntity roleEntity);

    @Named("stringToRoleEntity")
    default RoleEntity mapString(String roleName) {
        if (roleName == null) {
            return null;
        }
        return RoleEntity.builder().name(roleName).build();
    }

    @Named("roleEntityToString")
    default String mapRoleEntity(RoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }
        return roleEntity.getName();
    }
}