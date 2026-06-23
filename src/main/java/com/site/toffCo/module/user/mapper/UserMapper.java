package com.site.toffCo.module.user.mapper;

import com.site.toffCo.module.user.dto.UserRequestDTO;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.dto.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Mapping(target = "password", expression = "java(passwordEncoder.encode(requestDTO.password()))")
    @Mapping(target = "id", ignore = true) // Ignora o ID (o banco que gera)
    @Mapping(target = "createTime", ignore = true) // Ignora campos de auditoria
    @Mapping(target = "updateTime", ignore = true) // Ignora campos de auditoria
    @Mapping(target = "token", ignore = true)
    public abstract User toEntity(UserRequestDTO requestDTO);

    public abstract UserResponseDTO toDto(User entity);
    public abstract List<UserResponseDTO> toDtoList(List<User> entityList);
}
