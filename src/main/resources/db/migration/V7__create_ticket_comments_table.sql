CREATE TABLE ticket_comments
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT   NOT NULL,
    author_id  BIGINT   NOT NULL,
    message    TEXT     NOT NULL,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_ticket_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id),

    CONSTRAINT fk_ticket_comments_author
        FOREIGN KEY (author_id) REFERENCES users (id)
);