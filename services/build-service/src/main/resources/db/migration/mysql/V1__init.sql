CREATE TABLE builds (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(255),
    status      VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    created_at  DATETIME     NOT NULL
);

CREATE TABLE build_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT  NOT NULL,
    quantity    INT     NOT NULL,
    build_id    BIGINT,
    CONSTRAINT fk_build_items_build FOREIGN KEY (build_id) REFERENCES builds(id)
);
