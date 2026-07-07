SET search_path TO hardware_advisor;

CREATE TABLE recommendations (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    build_id             BIGINT       NOT NULL,
    rule_applied         VARCHAR(255) NOT NULL,
    suggested_product_id BIGINT       NOT NULL,
    reason               TEXT,
    created_at           TIMESTAMP    NOT NULL
);
