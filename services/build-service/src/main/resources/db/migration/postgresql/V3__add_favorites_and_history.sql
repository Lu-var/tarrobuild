CREATE TABLE favorites (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    build_id    BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    UNIQUE (user_id, build_id),
    CONSTRAINT fk_favorites_build FOREIGN KEY (build_id) REFERENCES builds(id)
);

CREATE TABLE build_history (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    build_id    BIGINT       NOT NULL,
    data        TEXT,
    changed_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_history_build FOREIGN KEY (build_id) REFERENCES builds(id)
);
