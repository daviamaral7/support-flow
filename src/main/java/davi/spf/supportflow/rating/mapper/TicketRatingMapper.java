package davi.spf.supportflow.rating.mapper;

import davi.spf.supportflow.rating.dto.TicketRatingResponseDTO;
import davi.spf.supportflow.rating.entity.TicketRating;
import org.springframework.stereotype.Component;

@Component
public class TicketRatingMapper {

    public TicketRatingResponseDTO toResponse(TicketRating rating) {
        return new TicketRatingResponseDTO(
                rating.getId(),
                rating.getTicket().getId(),
                rating.getRatedBy().getId(),
                rating.getRatedBy().getName(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt()
        );
    }
}
