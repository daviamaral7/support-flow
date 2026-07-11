package davi.spf.supportflow.comment.dto;

import java.time.LocalDateTime;

public record TicketCommentResponseDTO(
        Long id,
        Long ticketId,
        Long authorId,
        String authorName,
        String authorRole,
        String message,
        LocalDateTime createdAt
) {
}
