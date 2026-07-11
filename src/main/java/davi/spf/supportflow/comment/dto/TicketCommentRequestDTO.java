package davi.spf.supportflow.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketCommentRequestDTO(
        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must have at most 2000 characters")
        String comment
) {
}
