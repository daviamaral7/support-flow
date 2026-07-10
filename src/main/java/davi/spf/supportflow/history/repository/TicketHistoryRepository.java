package davi.spf.supportflow.history.repository;

import davi.spf.supportflow.history.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
}
