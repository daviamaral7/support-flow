package davi.spf.supportflow.rating.repository;

import davi.spf.supportflow.rating.entity.TicketRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRatingRepository extends JpaRepository<TicketRating, Long> {

    boolean existsByTicketId(Long ticketId);

    Optional<TicketRating> findByTicketId(Long ticketId);
}
