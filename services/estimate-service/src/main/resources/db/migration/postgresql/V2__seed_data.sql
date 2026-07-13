SET search_path TO estimate;

INSERT INTO estimates (build_id, total_cost, currency, created_at) VALUES
(1, 2840, 'USD', NOW()),
(2, 635000,  'CLP', NOW()),
(3, 2630, 'USD', NOW());
