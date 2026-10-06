CREATE OR REPLACE PACKAGE pkg_product AS
  PROCEDURE listar_categorias (p_cursor OUT SYS_REFCURSOR);
  PROCEDURE listar_todo (p_cursor OUT SYS_REFCURSOR);
  PROCEDURE buscar_uno  (p_id IN product.id%TYPE, p_cursor OUT SYS_REFCURSOR);
  PROCEDURE agregar     (p_name        IN product.name%TYPE,
                         p_description IN product.description%TYPE,
                         p_price       IN product.price%TYPE,
                         p_category_id IN product.category_id%TYPE,
                         p_brand       IN product.brand%TYPE,
                         p_model       IN product.model%TYPE,
                         p_id          OUT product.id%TYPE);
  PROCEDURE editar      (p_id          IN product.id%TYPE,
                         p_name        IN product.name%TYPE,
                         p_description IN product.description%TYPE,
                         p_price       IN product.price%TYPE,
                         p_category_id IN product.category_id%TYPE,
                         p_brand       IN product.brand%TYPE,
                         p_model       IN product.model%TYPE,
                         p_is_active   IN product.is_active%TYPE);
  PROCEDURE desactivar  (p_id IN product.id%TYPE);
  PROCEDURE eliminar    (p_id IN product.id%TYPE);
END pkg_product;
/

CREATE OR REPLACE PACKAGE BODY pkg_product AS
  e_en_uso EXCEPTION;
  PRAGMA EXCEPTION_INIT(e_en_uso, -2292);   -- ORA-02292: registro hijo encontrado

  -- Validaciones privadas
  PROCEDURE validar_existe(p_id product.id%TYPE) IS
    v NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v FROM product WHERE id = p_id;
    IF v = 0 THEN
      RAISE_APPLICATION_ERROR(-20001, 'Producto ' || p_id || ' no existe');
    END IF;
  END;

PROCEDURE validar_datos(p_name VARCHAR2, p_price NUMBER, p_category_id NUMBER) IS
  v NUMBER;
BEGIN
  IF TRIM(p_name) IS NULL THEN
    RAISE_APPLICATION_ERROR(-20005, 'El nombre es obligatorio');
  END IF;
  IF p_price IS NULL OR p_price <= 0 THEN
    RAISE_APPLICATION_ERROR(-20002, 'El precio debe ser mayor que 0');
  END IF;
  SELECT COUNT(*) INTO v FROM category WHERE id = p_category_id;
  IF v = 0 THEN
    RAISE_APPLICATION_ERROR(-20003, 'La categoría ' || p_category_id || ' no existe');
  END IF;
END;

  -- LISTAR CATEGORÍAS (apoyo)
  PROCEDURE listar_categorias(p_cursor OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_cursor FOR SELECT id, name FROM category WHERE is_active = 1 ORDER BY id;
  END;

  -- LISTAR TODO
  PROCEDURE listar_todo(p_cursor OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_cursor FOR
      SELECT p.id, p.name, c.name AS categoria, p.brand, p.model, p.price, p.is_active
      FROM product p JOIN category c ON c.id = p.category_id
      ORDER BY p.id;
  END;

  -- BUSCAR UNO
  PROCEDURE buscar_uno(p_id IN product.id%TYPE, p_cursor OUT SYS_REFCURSOR) IS
  BEGIN
    validar_existe(p_id);
    OPEN p_cursor FOR
      SELECT p.id, p.name, p.description, p.category_id, c.name AS categoria,
             p.brand, p.model, p.price, p.is_active
      FROM product p JOIN category c ON c.id = p.category_id
      WHERE p.id = p_id;
  END;

  -- AGREGAR
  PROCEDURE agregar(p_name IN product.name%TYPE, p_description IN product.description%TYPE,
                    p_price IN product.price%TYPE, p_category_id IN product.category_id%TYPE,
                    p_brand IN product.brand%TYPE, p_model IN product.model%TYPE,
                    p_id OUT product.id%TYPE) IS
  BEGIN
    validar_datos(p_name, p_price, p_category_id);
    INSERT INTO product (name, description, price, category_id, brand, model)
    VALUES (p_name, p_description, p_price, p_category_id, p_brand, p_model)
    RETURNING id INTO p_id;
    COMMIT;
  END;

  -- EDITAR
  PROCEDURE editar(p_id IN product.id%TYPE, p_name IN product.name%TYPE,
                   p_description IN product.description%TYPE, p_price IN product.price%TYPE,
                   p_category_id IN product.category_id%TYPE, p_brand IN product.brand%TYPE,
                   p_model IN product.model%TYPE, p_is_active IN product.is_active%TYPE) IS
  BEGIN
    validar_existe(p_id);
    validar_datos(p_name, p_price, p_category_id);

    IF p_is_active IS NULL OR p_is_active NOT IN (0, 1) THEN
      RAISE_APPLICATION_ERROR(-20006, 'is_active debe ser 0 o 1');
    END IF;
    
    UPDATE product
       SET name = p_name, description = p_description, price = p_price,
           category_id = p_category_id, brand = p_brand, model = p_model,
           is_active = p_is_active
     WHERE id = p_id;
    COMMIT;
  END;

  -- DESACTIVAR
  PROCEDURE desactivar(p_id IN product.id%TYPE) IS
  BEGIN
    validar_existe(p_id);
    UPDATE product SET is_active = 0 WHERE id = p_id;
    COMMIT;
  END;

  -- ELIMINAR
  PROCEDURE eliminar(p_id IN product.id%TYPE) IS
  BEGIN
    validar_existe(p_id);
    DELETE FROM product WHERE id = p_id;
    COMMIT;
  EXCEPTION
    WHEN e_en_uso THEN
      ROLLBACK;
      RAISE_APPLICATION_ERROR(-20004,
        'No se puede eliminar: el producto está referenciado (build o recomendación). Use desactivar.');
  END;
END pkg_product;
/
SHOW ERRORS PACKAGE pkg_product
SHOW ERRORS PACKAGE BODY pkg_product
