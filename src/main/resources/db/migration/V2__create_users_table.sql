CREATE TABLE users
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(30)  NOT NULL,
    status     VARCHAR(30)  NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL
);