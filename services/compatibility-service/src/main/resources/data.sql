-- =========================
-- COMPATIBILITY RULES
-- =========================

-- CPU socket must match Motherboard socket
INSERT INTO compatibility_rules (source_category, source_attribute_name, operator, target_category, target_attribute_name, incompatibility_reason)
VALUES ('CPU', 'Socket', 'EQ', 'Motherboard', 'Socket', 'CPU socket does not match motherboard socket');

-- RAM type must be supported by Motherboard
INSERT INTO compatibility_rules (source_category, source_attribute_name, operator, target_category, target_attribute_name, incompatibility_reason)
VALUES ('RAM', 'Type', 'EQ', 'Motherboard', 'Chipset', 'RAM type is not supported by motherboard chipset');

-- PSU wattage must be >= GPU power draw
INSERT INTO compatibility_rules (source_category, source_attribute_name, operator, target_category, target_attribute_name, incompatibility_reason)
VALUES ('GPU', 'Power Draw', 'GTE', 'PSU', 'Wattage', 'Power supply wattage is insufficient for GPU power draw');

-- Cooler TDP rating must be >= CPU TDP
INSERT INTO compatibility_rules (source_category, source_attribute_name, operator, target_category, target_attribute_name, incompatibility_reason)
VALUES ('CPU', 'TDP', 'GTE', 'Cooling', 'TDP Rating', 'Cooler TDP rating is insufficient for CPU TDP');

-- Case form factor must support Motherboard form factor
INSERT INTO compatibility_rules (source_category, source_attribute_name, operator, target_category, target_attribute_name, incompatibility_reason)
VALUES ('Motherboard', 'Form Factor', 'CONTAINS', 'Case', 'Form Factor Support', 'Case does not support motherboard form factor');
