SET search_path TO estimate;

CREATE TABLE estimates (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    build_id   BIGINT      NOT NULL,
    total_cost INT         NOT NULL,
    currency   VARCHAR(255) NOT NULL DEFAULT 'CLP',
    created_at TIMESTAMP   NOT NULL
);
