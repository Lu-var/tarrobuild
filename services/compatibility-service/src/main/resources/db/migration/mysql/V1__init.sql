CREATE TABLE compatibility_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_category VARCHAR(255) NOT NULL,
    source_attribute_name VARCHAR(255) NOT NULL,
    operator VARCHAR(255) NOT NULL,
    target_category VARCHAR(255) NOT NULL,
    target_attribute_name VARCHAR(255) NOT NULL,
    incompatibility_reason VARCHAR(500) NOT NULL
);

CREATE TABLE compatibility_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    build_id BIGINT NOT NULL,
    product_ids VARCHAR(2000) NOT NULL,
    result BOOLEAN NOT NULL,
    details TEXT,
    created_at DATETIME NOT NULL
);
