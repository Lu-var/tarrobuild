-- =========================
-- BUILDS
-- =========================

INSERT INTO builds (user_id, name, status, created_at) VALUES
                                                          (1, 'Gaming Beast RTX', 'VALIDATED', '2026-04-30T10:15:00'),
                                                          (2, 'Budget Office PC', 'DRAFT', '2026-04-30T10:20:00'),
                                                          (1, 'Streaming Ryzen Setup', 'INCOMPATIBLE', '2026-04-30T10:25:00'),
                                                          (3, 'Midrange Editing Build', 'VALIDATED', '2026-04-30T10:30:00'),
                                                          (2, 'Test Bench Intel', 'DRAFT', '2026-04-30T10:35:00');

-- =========================
-- BUILD ITEMS
-- =========================

-- Build 1 : Gaming Beast RTX (VALIDATED - AMD AM5 build)
INSERT INTO build_items (build_id, product_id, quantity) VALUES
                                                             (1, 2, 1),  -- AMD Ryzen 7 7800X3D (CPU, AM5)
                                                             (1, 5, 1),  -- NVIDIA RTX 4080 Super (GPU)
                                                             (1, 9, 2),  -- Corsair Vengeance 32GB DDR5 (RAM)
                                                             (1, 15, 1), -- Gigabyte X670E AORUS Master (Motherboard, AM5)
                                                             (1, 21, 1), -- Corsair RM1000x (PSU)
                                                             (1, 17, 1); -- Samsung 990 Pro 2TB NVMe (Storage)

-- Build 2 : Budget Office PC (DRAFT - Intel LGA1700)
INSERT INTO build_items (build_id, product_id, quantity) VALUES
                                                             (2, 3, 1),  -- Intel Core i5-14600K (CPU, LGA1700)
                                                             (2, 13, 1), -- ASUS ROG Strix Z790-E (Motherboard, LGA1700)
                                                             (2, 11, 1), -- Kingston Fury Beast 16GB DDR5 (RAM)
                                                             (2, 18, 1); -- WD Black SN850X 1TB NVMe (Storage)

-- Build 3 : Streaming Ryzen Setup (INCOMPATIBLE - AM5 CPU + LGA1700 motherboard)
INSERT INTO build_items (build_id, product_id, quantity) VALUES
                                                             (3, 2, 1),  -- AMD Ryzen 7 7800X3D (CPU, AM5)
                                                             (3, 13, 1), -- ASUS ROG Strix Z790-E (MOTHERBOARD, LGA1700) -- MISMATCH!
                                                             (3, 9, 2),  -- Corsair Vengeance 32GB DDR5 (RAM)
                                                             (3, 6, 1),  -- NVIDIA RTX 4070 Ti Super (GPU)
                                                             (3, 22, 1); -- EVGA SuperNOVA 850 G7 (PSU)

-- Build 4 : Midrange Editing Build (VALIDATED - Intel LGA1700)
INSERT INTO build_items (build_id, product_id, quantity) VALUES
                                                             (4, 1, 1),  -- Intel Core i7-14700K (CPU, LGA1700)
                                                             (4, 13, 1), -- ASUS ROG Strix Z790-E (Motherboard, LGA1700)
                                                             (4, 12, 2), -- Corsair Dominator Platinum 32GB DDR5 (RAM)
                                                             (4, 19, 1), -- Crucial P3 Plus 2TB NVMe (Storage)
                                                             (4, 7, 1),  -- AMD Radeon RX 7900 XTX (GPU)
                                                             (4, 23, 1); -- Seasonic Focus GX-750 (PSU)

-- Build 5 : Test Bench Intel (DRAFT - Intel LGA1700)
INSERT INTO build_items (build_id, product_id, quantity) VALUES
                                                             (5, 3, 1),  -- Intel Core i5-14600K (CPU, LGA1700)
                                                             (5, 14, 1), -- MSI MEG Z790 ACE (Motherboard, LGA1700)
                                                             (5, 20, 1); -- Seagate Barracuda 4TB HDD (Storage)