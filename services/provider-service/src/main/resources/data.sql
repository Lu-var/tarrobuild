-- =========================
-- PROVIDERS
-- =========================

INSERT INTO providers (name, contact, website, is_active) VALUES
    ('PcFactory', 'ventas@pcfactory.cl', 'https://www.pcfactory.cl', true),
    ('SoloTodo', 'contacto@solotodo.cl', 'https://www.solotodo.cl', true),
    ('SpDigital', 'info@spdigital.cl', 'https://www.spdigital.cl', true),
    ('Winpy', 'soporte@winpy.cl', 'https://www.winpy.cl', true);

-- =========================
-- PROVIDER PRODUCTS
-- =========================

-- PcFactory (id=1)
INSERT INTO provider_products (provider_id, product_id, external_reference) VALUES
    (1, 1, 'INTEL-CORE-I7-14700K'),
    (1, 5, 'NVIDIA-RTX-4080-SUPER'),
    (1, 9, 'CORSAIR-VENGEANCE-32GB-DDR5'),
    (1, 13, 'ASUS-ROG-STRIX-Z790-E'),
    (1, 21, 'CORSAIR-RM1000X');

-- SoloTodo (id=2)
INSERT INTO provider_products (provider_id, product_id, external_reference) VALUES
    (2, 2, 'AMD-RYZEN-7-7800X3D'),
    (2, 7, 'AMD-RX-7900-XTX'),
    (2, 10, 'GSKILL-TRIDENT-Z5-64GB'),
    (2, 15, 'GIGABYTE-X670E-AORUS'),
    (2, 22, 'EVGA-SUPERNOVA-850-G7');

-- SpDigital (id=3)
INSERT INTO provider_products (provider_id, product_id, external_reference) VALUES
    (3, 3, 'INTEL-CORE-I5-14600K'),
    (3, 6, 'NVIDIA-RTX-4070-TI-SUPER'),
    (3, 11, 'KINGSTON-FURY-16GB-DDR5'),
    (3, 17, 'SAMSUNG-990-PRO-2TB'),
    (3, 23, 'SEASONIC-FOCUS-GX-750');

-- Winpy (id=4)
INSERT INTO provider_products (provider_id, product_id, external_reference) VALUES
    (4, 4, 'AMD-RYZEN-5-7600X'),
    (4, 8, 'NVIDIA-RTX-4060-TI'),
    (4, 12, 'CORSAIR-DOMINATOR-32GB-DDR5'),
    (4, 16, 'ASUS-TUF-B650-PLUS'),
    (4, 24, 'CORSAIR-CV650');
