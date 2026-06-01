INSERT INTO builds (user_id, name, status, created_at) VALUES
(1, 'Gaming Beast RTX', 'VALIDATED',   '2026-04-30T10:15:00'),
(2, 'Budget Office PC', 'DRAFT',       '2026-04-30T10:20:00'),
(1, 'Streaming Ryzen Setup', 'INCOMPATIBLE', '2026-04-30T10:25:00'),
(3, 'Midrange Editing Build', 'VALIDATED',   '2026-04-30T10:30:00'),
(2, 'Test Bench Intel', 'DRAFT',       '2026-04-30T10:35:00');

INSERT INTO build_items (build_id, product_id, quantity) VALUES
-- Build 1 : Gaming Beast RTX (VALIDATED - AMD AM5 build)
(1, 2,  1),
(1, 5,  1),
(1, 9,  2),
(1, 15, 1),
(1, 21, 1),
(1, 17, 1),

-- Build 2 : Budget Office PC (DRAFT - Intel LGA1700)
(2, 3,  1),
(2, 13, 1),
(2, 11, 1),
(2, 18, 1),

-- Build 3 : Streaming Ryzen Setup (INCOMPATIBLE - AM5 CPU + LGA1700 motherboard)
(3, 2,  1),
(3, 13, 1),
(3, 9,  2),
(3, 6,  1),
(3, 22, 1),

-- Build 4 : Midrange Editing Build (VALIDATED - Intel LGA1700)
(4, 1,  1),
(4, 13, 1),
(4, 12, 2),
(4, 19, 1),
(4, 7,  1),
(4, 23, 1),

-- Build 5 : Test Bench Intel (DRAFT - Intel LGA1700)
(5, 3,  1),
(5, 14, 1),
(5, 20, 1);
