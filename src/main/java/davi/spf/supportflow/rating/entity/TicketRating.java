package davi.spf.supportflow.rating.entity;

import davi.spf.supportflow.ticket.entity.Ticket;
import davi.spf.supportflow.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "ticket_ratings")
public class TicketRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rated_by_id", nullable = false)
    private User ratedBy;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static TicketRating create(Ticket ticket, User ratedBy, Integer score, String comment) {
        TicketRating rating = new TicketRating();
        rating.setTicket(ticket);
        rating.setRatedBy(ratedBy);
        rating.setScore(score);
        rating.setComment(comment);
        return rating;
    }
}
