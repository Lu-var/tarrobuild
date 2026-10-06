INSERT INTO category (name, description) VALUES ('CPU','Procesadores');
INSERT INTO category (name, description) VALUES ('Placa madre','Placas madre');
INSERT INTO category (name, description) VALUES ('RAM','Memorias RAM');
INSERT INTO category (name, description) VALUES ('GPU','Tarjetas de video');
INSERT INTO category (name, description) VALUES ('Fuente de poder','Fuentes de poder');

INSERT INTO usuario (name, last_name, email) VALUES ('Admin','TarroBuild','admin@tarrobuild.cl');
INSERT INTO usuario (name, last_name, email) VALUES ('Usuario','Prueba','user@test.com');
INSERT INTO credential (usuario_id, password_hash, role)
  VALUES ((SELECT id FROM usuario WHERE email='admin@tarrobuild.cl'),'hash_demo_admin','ADMIN');
INSERT INTO credential (usuario_id, password_hash, role)
  VALUES ((SELECT id FROM usuario WHERE email='user@test.com'),'hash_demo_user','USER');

DECLARE
  PROCEDURE prod(p_name VARCHAR2, p_cat VARCHAR2, p_price NUMBER, p_brand VARCHAR2, p_model VARCHAR2) IS
  BEGIN
    INSERT INTO product (name, price, category_id, brand, model)
    VALUES (p_name, p_price, (SELECT id FROM category WHERE name = p_cat), p_brand, p_model);
  END;
  PROCEDURE attr(p_model VARCHAR2, p_n VARCHAR2, p_v VARCHAR2) IS
  BEGIN
    INSERT INTO product_attribute (product_id, attribute_name, attribute_value)
    VALUES ((SELECT id FROM product WHERE model = p_model), p_n, p_v);
  END;
  PROCEDURE bld(p_name VARCHAR2) IS
  BEGIN
    INSERT INTO build (usuario_id, name)
    VALUES ((SELECT id FROM usuario WHERE email='user@test.com'), p_name);
  END;
  PROCEDURE item(p_build VARCHAR2, p_model VARCHAR2) IS
  BEGIN
    INSERT INTO build_item (build_id, product_id)
    VALUES ((SELECT id FROM build WHERE name = p_build), (SELECT id FROM product WHERE model = p_model));
  END;
BEGIN
  prod('AMD Ryzen 5 5600',        'CPU',             109990,'AMD','5600');
  prod('AMD Ryzen 7 7700',        'CPU',             239990,'AMD','7700');
  prod('MSI B550M PRO',           'Placa madre',      89990,'MSI','B550M PRO');
  prod('ASUS PRIME B650M',        'Placa madre',     139990,'ASUS','PRIME B650M');
  prod('Kingston Fury 16GB DDR4', 'RAM',              39990,'Kingston','FURY16D4');
  prod('Corsair Vengeance 16GB DDR5','RAM',           69990,'Corsair','VENG16D5');
  prod('NVIDIA GeForce RTX 4060', 'GPU',             329990,'NVIDIA','RTX 4060');
  prod('Corsair CV650 650W',      'Fuente de poder',  59990,'Corsair','CV650');

  attr('5600','socket','AM4');       attr('5600','tdp','65');
  attr('7700','socket','AM5');       attr('7700','tdp','65');
  attr('B550M PRO','socket','AM4');  attr('B550M PRO','ram_type','DDR4');
  attr('PRIME B650M','socket','AM5');attr('PRIME B650M','ram_type','DDR5');
  attr('FURY16D4','ram_type','DDR4');attr('VENG16D5','ram_type','DDR5');
  attr('RTX 4060','tdp','115');      attr('CV650','potencia','650');

  bld('PC Oficina AM4');        -- build compatible
  item('PC Oficina AM4','5600'); item('PC Oficina AM4','B550M PRO');
  item('PC Oficina AM4','FURY16D4'); item('PC Oficina AM4','CV650');

  bld('PC Prueba Incompatible'); -- Ryzen AM5 con placa AM4
  item('PC Prueba Incompatible','7700'); item('PC Prueba Incompatible','B550M PRO');
  item('PC Prueba Incompatible','FURY16D4');
END;
/
COMMIT;
