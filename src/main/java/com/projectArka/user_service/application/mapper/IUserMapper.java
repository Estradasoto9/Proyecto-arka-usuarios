// src/main/java/com/projectArka/user_service/application/mapper/IUserMapper.java
package com.projectArka.user_service.application.mapper;

import com.projectArka.user_service.application.dto.UserRegisterRequestDTO;
import com.projectArka.user_service.application.dto.UserResponseDTO;
import com.projectArka.user_service.application.dto.UserUpdateRequestDTO;
import com.projectArka.user_service.application.dto.RoleDTO;
import com.projectArka.user_service.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface IUserMapper {

    User toDomain(UserRegisterRequestDTO dto);

    UserResponseDTO toDTO(User domain);

    default RoleDTO map(String roleName) {
        if (roleName == null) {
            return null;
        }
        return RoleDTO.builder().name(roleName).build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateDomainFromDTO(UserUpdateRequestDTO dto, @MappingTarget User user);
}