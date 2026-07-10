package davi.spf.supportflow.ticket.entity;

import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.ticket.enums.TicketPriority;
import davi.spf.supportflow.ticket.enums.TicketStatus;
import davi.spf.supportflow.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void assignTo(User technician) {
        this.assignedTo = technician;
        this.status = TicketStatus.IN_PROGRESS;
    }

    public void resolve() {
        this.status = TicketStatus.RESOLVED;
    }

    public void close() {
        this.status = TicketStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TicketStatus.CANCELLED;
        this.closedAt = LocalDateTime.now();
    }
}
