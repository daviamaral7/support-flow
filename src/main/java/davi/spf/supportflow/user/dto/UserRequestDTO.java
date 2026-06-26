package davi.spf.supportflow.user.dto;

import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.enums.UserStatus;

public record UserRequestDTO(
        String name,
        String email,
        String password,
        UserRole role,
        UserStatus status
) {
}
