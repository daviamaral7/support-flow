package davi.spf.supportflow.ticket.dto;

import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.enums.TicketStatus;

public record TicketFilterDTO(
        TicketStatus status,
        TicketPriority priority,
        Long categoryId,
        Long assignedToId,
        Long createdById
) {
}
