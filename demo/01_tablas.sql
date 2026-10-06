BEGIN
  FOR t IN (SELECT table_name FROM user_tables WHERE table_name IN
    ('NOTIFICATION_LOG','RECOMMENDATION','ESTIMATE','BUILD_ITEM','BUILD',
     'CREDENTIAL','USUARIO','PRODUCT_ATTRIBUTE','PRODUCT','CATEGORY')) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
  END LOOP;
END;
/

CREATE TABLE category (
  id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        VARCHAR2(60)  NOT NULL UNIQUE,
  description VARCHAR2(200),
  is_active   NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1))
);

CREATE TABLE product (
  id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        VARCHAR2(120) NOT NULL,
  description VARCHAR2(300),
  price       NUMBER(10,2)  NOT NULL CHECK (price > 0),
  category_id NUMBER        NOT NULL REFERENCES category(id),
  brand       VARCHAR2(60),
  model       VARCHAR2(60),
  is_active   NUMBER(1) DEFAULT 1 NOT NULL CHECK (is_active IN (0,1)),
  created_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE product_attribute (
  id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  product_id      NUMBER NOT NULL REFERENCES product(id) ON DELETE CASCADE,
  attribute_name  VARCHAR2(60)  NOT NULL,
  attribute_value VARCHAR2(120) NOT NULL,
  CONSTRAINT uq_prod_attr UNIQUE (product_id, attribute_name)
);

CREATE TABLE usuario (
  id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name       VARCHAR2(60)  NOT NULL,
  last_name  VARCHAR2(60)  NOT NULL,
  email      VARCHAR2(120) NOT NULL UNIQUE,
  phone      VARCHAR2(20),
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE credential (
  id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id    NUMBER NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
  password_hash VARCHAR2(100) NOT NULL,
  role          VARCHAR2(10) DEFAULT 'USER' NOT NULL CHECK (role IN ('ADMIN','USER'))
);

CREATE TABLE build (
  id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id NUMBER NOT NULL REFERENCES usuario(id),
  name       VARCHAR2(100) NOT NULL,
  status     VARCHAR2(12) DEFAULT 'BORRADOR' NOT NULL
             CHECK (status IN ('BORRADOR','VALIDADA','COTIZADA')),
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE build_item (
  id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  build_id   NUMBER NOT NULL REFERENCES build(id) ON DELETE CASCADE,
  product_id NUMBER NOT NULL REFERENCES product(id),   -- sin cascade: protege el catálogo
  quantity   NUMBER(3) DEFAULT 1 NOT NULL CHECK (quantity > 0),
  CONSTRAINT uq_build_prod UNIQUE (build_id, product_id)
);

CREATE TABLE estimate (
  id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  build_id    NUMBER NOT NULL REFERENCES build(id) ON DELETE CASCADE,
  total_price NUMBER(12,2) NOT NULL,
  currency    VARCHAR2(3) DEFAULT 'CLP' NOT NULL,
  created_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE recommendation (
  id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  build_id             NUMBER NOT NULL REFERENCES build(id) ON DELETE CASCADE,
  rule_applied         VARCHAR2(100),
  suggested_product_id NUMBER REFERENCES product(id),
  reason               VARCHAR2(300),
  created_at           TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE notification_log (
  id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id NUMBER NOT NULL REFERENCES usuario(id),
  type       VARCHAR2(30)  NOT NULL,
  content    VARCHAR2(300) NOT NULL,
  status     VARCHAR2(12) DEFAULT 'ENVIADA' NOT NULL,
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE OR REPLACE VIEW v_compatibilidad_build AS
SELECT s.build_id, s.cpu_socket, s.board_socket, s.board_ram, s.ram_type,
       CASE WHEN s.cpu_socket IS NULL OR s.board_socket IS NULL THEN 'SIN DATOS'
            WHEN s.cpu_socket = s.board_socket THEN 'OK' ELSE 'INCOMPATIBLE' END AS socket_check,
       CASE WHEN s.board_ram IS NULL OR s.ram_type IS NULL THEN 'SIN DATOS'
            WHEN s.board_ram = s.ram_type THEN 'OK' ELSE 'INCOMPATIBLE' END AS ram_check
FROM (
  SELECT bi.build_id,
    MAX(CASE WHEN c.name='CPU'         AND pa.attribute_name='socket'   THEN pa.attribute_value END) AS cpu_socket,
    MAX(CASE WHEN c.name='Placa madre' AND pa.attribute_name='socket'   THEN pa.attribute_value END) AS board_socket,
    MAX(CASE WHEN c.name='Placa madre' AND pa.attribute_name='ram_type' THEN pa.attribute_value END) AS board_ram,
    MAX(CASE WHEN c.name='RAM'         AND pa.attribute_name='ram_type' THEN pa.attribute_value END) AS ram_type
  FROM build_item bi
  JOIN product p            ON p.id = bi.product_id
  JOIN category c           ON c.id = p.category_id
  JOIN product_attribute pa ON pa.product_id = p.id
  GROUP BY bi.build_id
) s;
