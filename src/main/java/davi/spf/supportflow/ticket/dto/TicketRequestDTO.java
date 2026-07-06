package davi.spf.supportflow.ticket.dto;

import davi.spf.supportflow.ticket.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequestDTO(
        @NotBlank(message = "Title is required")
        @Size(min = 4)
        String title,
        @NotBlank(message = "Description is required")
        String description,
        @NotNull(message = "Priority is required")
        TicketPriority priority,
        @NotNull(message = "Category id is required")
        Long categoryId
) {
}
