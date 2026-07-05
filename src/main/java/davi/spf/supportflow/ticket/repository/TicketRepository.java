package davi.spf.supportflow.ticket.repository;

import davi.spf.supportflow.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
