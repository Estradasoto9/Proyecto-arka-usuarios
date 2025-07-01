package com.projectArka.user_service.application.mapper;

import com.projectArka.user_service.application.dto.RoleDTO;
import com.projectArka.user_service.domain.model.Role;
import com.projectArka.user_service.infrastructure.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;


@Mapper(componentModel = "spring")
public interface IRoleMapper {

    RoleDTO toDTO(Role role);

    Role toDomain(RoleDTO roleDTO);

    @Named("stringToRoleDTO")
    default RoleDTO map(String roleName) {
        if (roleName == null) {
            return null;
        }
        return RoleDTO.builder().name(roleName).build();
    }

    @Named("roleEntityToString")
    default String map(RoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }
        return roleEntity.getName();
    }

    Role toDomain(RoleEntity roleEntity);

}