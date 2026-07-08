package davi.spf.supportflow.ticket.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTicketRequestDTO(
        @NotNull(message = "Technician id is required")
        Long technicianId
) {
}
