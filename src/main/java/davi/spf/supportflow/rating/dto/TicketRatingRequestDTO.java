package davi.spf.supportflow.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRatingRequestDTO(
        @NotNull(message = "Score is required")
        @Min(value = 1, message = "Score must be at least 1")
        @Max(value = 5, message = "Score must be at most 5")
        Integer score,

        @Size(max = 500, message = "Comment must have at most 500 characters")
        String comment
) {
}
