SET search_path TO category;

INSERT INTO categories (name, slug, description, is_active) VALUES
('CPU', 'cpu', 'Processors for desktop systems', true),
('GPU', 'gpu', 'Graphics cards for gaming and rendering', true),
('RAM', 'ram', 'Memory modules', true),
('Motherboard', 'motherboard', 'Main system board', true),
('Storage', 'storage', 'SSDs and HDDs', true),
('Power Supply', 'psu', 'Power supply units', true),
('Case', 'case', 'PC cases and chassis', true),
('Cooling', 'cooling', 'Air and liquid cooling solutions', true);

INSERT INTO attribute_definitions (attribute_name, value_type, is_required, category_id) VALUES
('Socket', 'STRING', true, 1),
('Cores', 'NUMBER', true, 1),
('Threads', 'NUMBER', true, 1),
('Base Clock', 'NUMBER', false, 1),
('TDP', 'NUMBER', false, 1),
('VRAM', 'NUMBER', true, 2),
('Core Clock', 'NUMBER', false, 2),
('Power Draw', 'NUMBER', false, 2),
('Capacity', 'NUMBER', true, 3),
('Speed', 'NUMBER', true, 3),
('Type', 'STRING', true, 3),
('Socket', 'STRING', true, 4),
('Chipset', 'STRING', true, 4),
('Form Factor', 'STRING', true, 4),
('Capacity', 'NUMBER', true, 5),
('Type', 'STRING', true, 5),
('Read Speed', 'NUMBER', false, 5),
('Wattage', 'NUMBER', true, 6),
('Efficiency Rating', 'STRING', false, 6),
('Modular', 'BOOLEAN', false, 6),
('Form Factor Support', 'STRING', true, 7),
('GPU Clearance', 'NUMBER', false, 7),
('Cooling Support', 'STRING', false, 7),
('Type', 'STRING', true, 8),
('TDP Rating', 'NUMBER', false, 8),
('Fan Size', 'NUMBER', false, 8);
