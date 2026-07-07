SET search_path TO notification;

CREATE TABLE notification_logs (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id   BIGINT       NOT NULL,
    type      VARCHAR(255) NOT NULL,
    content   VARCHAR(255) NOT NULL,
    status    VARCHAR(32)  NOT NULL DEFAULT 'INFO',
    timestamp TIMESTAMP    NOT NULL
);
