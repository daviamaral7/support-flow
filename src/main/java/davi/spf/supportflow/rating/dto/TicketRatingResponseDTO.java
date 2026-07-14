package davi.spf.supportflow.rating.dto;

import java.time.LocalDateTime;

public record TicketRatingResponseDTO(
        Long id,
        Long ticketId,
        Long ratedById,
        String ratedByName,
        Integer score,
        String comment,
        LocalDateTime createdAt
) {
}
