CREATE TABLE ticket_history
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT       NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    description     VARCHAR(255) NOT NULL,
    performed_by_id BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL,

    CONSTRAINT fk_ticket_history_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id),

    CONSTRAINT fk_ticket_history_performed_by
        FOREIGN KEY (performed_by_id) REFERENCES users (id)
);