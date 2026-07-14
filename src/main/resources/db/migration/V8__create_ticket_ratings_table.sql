CREATE TABLE ticket_ratings
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id   BIGINT   NOT NULL,
    rated_by_id BIGINT   NOT NULL,
    score       INT      NOT NULL,
    comment     VARCHAR(500),
    created_at  DATETIME NOT NULL,

    CONSTRAINT uk_ticket_ratings_ticket UNIQUE (ticket_id),

    CONSTRAINT fk_ticket_ratings_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id),

    CONSTRAINT fk_ticket_ratings_rated_by
        FOREIGN KEY (rated_by_id) REFERENCES users (id),

    CONSTRAINT chk_ticket_ratings_score
        CHECK (score BETWEEN 1 AND 5)
);