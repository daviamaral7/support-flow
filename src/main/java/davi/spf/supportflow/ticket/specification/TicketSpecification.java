package davi.spf.supportflow.ticket.specification;

import davi.spf.supportflow.ticket.dto.TicketFilterDTO;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

public class TicketSpecification {

    private TicketSpecification() {
    }

    public static Specification<Ticket> withFilters(TicketFilterDTO filter) {
        return Specification
                .where(hasStatus(filter.status()))
                .and(hasPriority(filter.priority()))
                .and(hasCategory(filter.categoryId()))
                .and(hasAssignedTo(filter.assignedToId()))
                .and(hasCreatedBy(filter.createdById()));
    }

    private static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }

            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    private static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, criteriaBuilder) -> {
            if (priority == null) {
                return null;
            }

            return criteriaBuilder.equal(root.get("priority"), priority);
        };
    }

    private static Specification<Ticket> hasCategory(Long categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return null;
            }

            return criteriaBuilder.equal(root.get("category").get("id"), categoryId);
        };
    }

    private static Specification<Ticket> hasAssignedTo(Long assignedToId) {
        return (root, query, criteriaBuilder) -> {
            if (assignedToId == null) {
                return null;
            }

            return criteriaBuilder.equal(root.get("assignedTo").get("id"), assignedToId);
        };
    }

    private static Specification<Ticket> hasCreatedBy(Long createdById) {
        return (root, query, criteriaBuilder) -> {
            if (createdById == null) {
                return null;
            }

            return criteriaBuilder.equal(root.get("createdBy").get("id"), createdById);
        };
    }
}