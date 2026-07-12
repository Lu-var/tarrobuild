CREATE TABLE favorites (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    build_id    BIGINT       NOT NULL,
    created_at  DATETIME     NOT NULL,
    UNIQUE KEY uk_user_build (user_id, build_id),
    CONSTRAINT fk_favorites_build FOREIGN KEY (build_id) REFERENCES builds(id)
);

CREATE TABLE build_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    build_id    BIGINT       NOT NULL,
    data        TEXT,
    changed_at  DATETIME     NOT NULL,
    CONSTRAINT fk_history_build FOREIGN KEY (build_id) REFERENCES builds(id)
);
