package davi.spf.supportflow.dashboard.service;

import davi.spf.supportflow.dashboard.dto.DashboardSummaryDTO;
import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DashboardService {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary() {
        long totalTickets = ticketRepository.count();

        long openTickets = ticketRepository.countByStatus(TicketStatus.OPEN);
        long inProgressTickets = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long resolvedTickets = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closedTickets = ticketRepository.countByStatus(TicketStatus.CLOSED);
        long cancelledTickets = ticketRepository.countByStatus(TicketStatus.CANCELLED);

        long criticalTickets = ticketRepository.countByPriority(TicketPriority.CRITICAL);
        long unassignedTickets = ticketRepository.countByAssignedToIsNull();

        return new DashboardSummaryDTO(
                totalTickets,
                openTickets,
                inProgressTickets,
                resolvedTickets,
                closedTickets,
                cancelledTickets,
                criticalTickets,
                unassignedTickets
        );
    }
}
