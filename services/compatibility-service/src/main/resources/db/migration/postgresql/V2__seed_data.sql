SET search_path TO compatibility;

INSERT INTO compatibility_rules (source_category, source_attribute_name, operator, target_category, target_attribute_name, incompatibility_reason)
VALUES
('CPU', 'Socket', 'EQ', 'Motherboard', 'Socket', 'CPU socket does not match motherboard socket'),
('RAM', 'Type', 'EQ', 'Motherboard', 'Chipset', 'RAM type is not supported by motherboard chipset'),
('GPU', 'Power Draw', 'GTE', 'PSU', 'Wattage', 'Power supply wattage is insufficient for GPU power draw'),
('CPU', 'TDP', 'GTE', 'Cooling', 'TDP Rating', 'Cooler TDP rating is insufficient for CPU TDP'),
('Motherboard', 'Form Factor', 'CONTAINS', 'Case', 'Form Factor Support', 'Case does not support motherboard form factor');
