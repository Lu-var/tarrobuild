CREATE TABLE estimates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    build_id BIGINT NOT NULL,
    total_price INT NOT NULL,
    currency VARCHAR(255) NOT NULL DEFAULT 'CLP',
    created_at DATETIME NOT NULL
);
