package davi.spf.supportflow.user.dto;

public record UserSummaryDTO(
        Long id,
        String name,
        String email,
        String role
) {
}
