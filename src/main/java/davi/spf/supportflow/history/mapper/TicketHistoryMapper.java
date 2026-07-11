package davi.spf.supportflow.history.mapper;

import davi.spf.supportflow.history.dto.TicketHistoryResponseDTO;
import davi.spf.supportflow.history.entity.TicketHistory;
import org.springframework.stereotype.Component;

@Component
public class TicketHistoryMapper {

    public TicketHistoryResponseDTO toResponse(TicketHistory history) {
        return new TicketHistoryResponseDTO(
                history.getId(),
                history.getAction().name(),
                history.getDescription(),
                history.getPerformedBy().getId(),
                history.getPerformedBy().getName(),
                history.getCreatedAt()
        );
    }
}
