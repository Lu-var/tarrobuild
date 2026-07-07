SET search_path TO provider;

CREATE TABLE providers (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    contact    VARCHAR(255),
    website    VARCHAR(255),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE provider_products (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_id        BIGINT NOT NULL,
    product_id         BIGINT NOT NULL,
    external_reference VARCHAR(255)
);
