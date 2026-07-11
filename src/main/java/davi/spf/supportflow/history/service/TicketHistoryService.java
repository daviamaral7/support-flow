package davi.spf.supportflow.history.service;

import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.history.dto.TicketHistoryResponseDTO;
import davi.spf.supportflow.history.entity.TicketHistory;
import davi.spf.supportflow.history.enums.TicketActionHistory;
import davi.spf.supportflow.history.mapper.TicketHistoryMapper;
import davi.spf.supportflow.history.repository.TicketHistoryRepository;
import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserRole;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryMapper mapper;

    public void registerCreated(Ticket ticket, User performedBy) {
        TicketHistory ticketHistory = TicketHistory.create(
                ticket,
                TicketActionHistory.CREATED,
                "Ticket created",
                performedBy
        );

        ticketHistoryRepository.save(ticketHistory);
    }

    public void registerAssigned(Ticket ticket, User admin, User assignedTo) {
        TicketHistory ticketHistory = TicketHistory.create(
                ticket,
                TicketActionHistory.ASSIGNED,
                "Ticket assigned to " + assignedTo.getName(),
                admin
        );

        ticketHistoryRepository.save(ticketHistory);
    }

    public void registerClaimed(Ticket ticket, User technician) {
        TicketHistory ticketHistory = TicketHistory.create(
                ticket,
                TicketActionHistory.CLAIMED,
                "Ticket claimed by " + technician.getName(),
                technician
        );

        ticketHistoryRepository.save(ticketHistory);
    }

    public void registerResolved(Ticket ticket, User performedBy) {
        TicketHistory ticketHistory = TicketHistory.create(
                ticket,
                TicketActionHistory.RESOLVED,
                "Ticket resolved",
                performedBy
        );

        ticketHistoryRepository.save(ticketHistory);
    }

    public void registerClosed(Ticket ticket, User performedBy) {
        TicketHistory ticketHistory = TicketHistory.create(
                ticket,
                TicketActionHistory.CLOSED,
                "Ticket closed",
                performedBy
        );

        ticketHistoryRepository.save(ticketHistory);
    }

    public void registerCancelled(Ticket ticket, User performedBy) {
        TicketHistory ticketHistory = TicketHistory.create(
                ticket,
                TicketActionHistory.CANCELLED,
                "Ticket cancelled",
                performedBy
        );

        ticketHistoryRepository.save(ticketHistory);
    }

    @Transactional(readOnly = true)
    public Page<TicketHistoryResponseDTO> listHistory(Long ticketId, Authentication authentication, Pageable pageable) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        validateTicketHistoryAccess(ticket, authenticatedUser);

        return ticketHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticketId, pageable)
                .map(mapper::toResponse);
    }

    private void validateTicketHistoryAccess(Ticket ticket, User user) {
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.TECHNICIAN) {
            return;
        }

        if (!ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to access this ticket history");
        }
    }
}
