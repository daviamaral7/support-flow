package davi.spf.supportflow.user.dto;

import java.time.LocalDateTime;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
