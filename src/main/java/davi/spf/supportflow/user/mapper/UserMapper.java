package davi.spf.supportflow.user.mapper;

import davi.spf.supportflow.user.dto.UserRequestDTO;
import davi.spf.supportflow.user.dto.UserResponseDTO;
import davi.spf.supportflow.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserResponseDTO toResponse(User user);

    User toEntity(UserRequestDTO dto);
}
