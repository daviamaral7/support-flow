package davi.spf.supportflow.ticket.dto;

import davi.spf.supportflow.category.dto.CategorySummaryDTO;
import davi.spf.supportflow.user.dto.UserSummaryDTO;

import java.time.LocalDateTime;

public record TicketResponseDTO(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        UserSummaryDTO createdBy,
        UserSummaryDTO assignedTo,
        CategorySummaryDTO category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt
) {
}
