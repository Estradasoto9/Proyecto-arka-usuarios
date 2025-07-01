package com.projectArka.user_service.application.mapper;

import com.projectArka.user_service.domain.model.User;
import com.projectArka.user_service.infrastructure.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = IPersistenceRoleMapper.class)
public interface IPersistenceUserMapper {

    @Mapping(target = "id", expression = "java(user.getId() != null && !user.getId().isEmpty() ? java.util.UUID.fromString(user.getId()) : null)")

    @Mapping(target = "roles", ignore = true)
    UserEntity toEntity(User user);

    @Mapping(target = "id", expression = "java(userEntity.getId() != null ? userEntity.getId().toString() : null)")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "roleEntityToString")
    User toDomain(UserEntity userEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "username", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "email", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "phone", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "active", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDomain(User user, @MappingTarget UserEntity userEntity);
}