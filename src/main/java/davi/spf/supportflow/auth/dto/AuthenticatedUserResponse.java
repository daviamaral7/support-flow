package davi.spf.supportflow.auth.dto;

public record AuthenticatedUserResponse(
        Long id,
        String name,
        String email,
        String role,
        String status
) {
}
