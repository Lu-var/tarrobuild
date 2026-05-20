CREATE TABLE recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    build_id BIGINT NOT NULL,
    rule_applied VARCHAR(255) NOT NULL,
    suggested_product_id BIGINT NOT NULL,
    reason TEXT,
    created_at DATETIME NOT NULL
);
