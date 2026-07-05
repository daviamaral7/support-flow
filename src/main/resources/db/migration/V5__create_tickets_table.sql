CREATE TABLE tickets
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(150) NOT NULL,
    description    TEXT         NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    priority       VARCHAR(30)  NOT NULL,
    created_by_id  BIGINT       NOT NULL,
    assigned_to_id BIGINT,
    category_id    BIGINT       NOT NULL,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    closed_at      DATETIME,

    CONSTRAINT fk_tickets_created_by
        FOREIGN KEY (created_by_id) REFERENCES users (id),

    CONSTRAINT fk_tickets_assigned_to
        FOREIGN KEY (assigned_to_id) REFERENCES users (id),

    CONSTRAINT fk_tickets_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
);