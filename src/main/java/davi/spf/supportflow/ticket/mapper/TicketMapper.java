package davi.spf.supportflow.ticket.mapper;

import davi.spf.supportflow.category.dto.CategorySummaryDTO;
import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.ticket.dto.TicketRequestDTO;
import davi.spf.supportflow.ticket.dto.TicketResponseDTO;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.user.dto.UserSummaryDTO;
import davi.spf.supportflow.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public Ticket toEntity(TicketRequestDTO dto) {
        Ticket ticket = new Ticket();
        ticket.setTitle(dto.title().trim());
        ticket.setDescription(dto.description().trim());
        ticket.setPriority(dto.priority());
        return ticket;
    }

    public TicketResponseDTO toResponse(Ticket ticket) {
        return new TicketResponseDTO(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                toUserSummary(ticket.getCreatedBy()),
                toUserSummary(ticket.getAssignedTo()),
                toCategorySummary(ticket.getCategory()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getClosedAt()
        );
    }

    private UserSummaryDTO toUserSummary(User user) {
        if (user == null) {
            return null;
        }

        return new UserSummaryDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private CategorySummaryDTO toCategorySummary(Category category) {
        if (category == null) {
            return null;
        }

        return new CategorySummaryDTO(
                category.getId(),
                category.getName()
        );
    }
}
