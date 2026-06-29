SET search_path TO category;

CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active   BOOLEAN DEFAULT TRUE
);

CREATE TABLE attribute_definitions (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attribute_name VARCHAR(255) NOT NULL,
    value_type     VARCHAR(32)  NOT NULL,
    is_required    BOOLEAN      NOT NULL,
    category_id    BIGINT       NOT NULL,
    CONSTRAINT fk_attr_def_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
