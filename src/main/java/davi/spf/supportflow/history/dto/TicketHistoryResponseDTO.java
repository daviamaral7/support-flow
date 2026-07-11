package davi.spf.supportflow.history.dto;

import java.time.LocalDateTime;

public record TicketHistoryResponseDTO(
        Long id,
        String action,
        String description,
        Long performedById,
        String performedByName,
        LocalDateTime createdAt
) {
}
