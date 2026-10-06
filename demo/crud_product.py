import getpass
import os
from decimal import Decimal, InvalidOperation

import oracledb


def conectar():
    user = os.getenv("ORA_USER") or input("Usuario Oracle: ")
    password = os.getenv("ORA_PASSWORD") or getpass.getpass("Contraseña: ")
    dsn = os.getenv("ORA_DSN") or (
        input("DSN [localhost:1521/FREEPDB1]: ").strip()
        or "localhost:1521/FREEPDB1"
    )
    return oracledb.connect(user=user, password=password, dsn=dsn)


def llamar_cursor(conn, procedimiento, *args):
    cur = conn.cursor()
    salida = conn.cursor()
    cur.callproc(f"pkg_product.{procedimiento}", [*args, salida])
    columnas = [d[0] for d in salida.description]
    return columnas, salida.fetchall()


def mostrar(columnas, filas):
    if not filas:
        print("(sin resultados)")
        return
    tabla = [[("" if v is None else str(v)) for v in f] for f in filas]
    anchos = [max(len(c), *(len(f[i]) for f in tabla)) for i, c in enumerate(columnas)]
    print(" | ".join(c.ljust(anchos[i]) for i, c in enumerate(columnas)))
    print("-+-".join("-" * a for a in anchos))
    for f in tabla:
        print(" | ".join(v.ljust(anchos[i]) for i, v in enumerate(f)))


def pedir_numero(texto, tipo=int, actual=None):
    while True:
        valor = input(
            texto + (f" [{actual}]" if actual is not None else "") + ": "
        ).strip()
        if valor == "" and actual is not None:
            return actual
        try:
            return tipo(valor)
        except (ValueError, InvalidOperation):
            print("  Valor inválido, intenta de nuevo.")


# ---------------------------- operaciones CRUD ----------------------------
def listar_todo(conn):
    mostrar(*llamar_cursor(conn, "listar_todo"))


def buscar_uno(conn):
    id_ = pedir_numero("ID del producto")
    mostrar(*llamar_cursor(conn, "buscar_uno", id_))


def agregar(conn):
    print("Categorías disponibles:")
    mostrar(*llamar_cursor(conn, "listar_categorias"))
    categoria = pedir_numero("ID de categoría")
    nombre = input("Nombre: ").strip()
    descripcion = input("Descripción (opcional): ").strip() or None
    precio = pedir_numero("Precio", Decimal)
    marca = input("Marca: ").strip() or None
    modelo = input("Modelo: ").strip() or None
    cur = conn.cursor()
    nuevo_id = cur.var(int)
    cur.callproc(
        "pkg_product.agregar",
        [nombre, descripcion, precio, categoria, marca, modelo, nuevo_id],
    )
    print(f"Producto agregado con ID {nuevo_id.getvalue()}")


def editar(conn):
    id_ = pedir_numero("ID del producto a editar")
    columnas, filas = llamar_cursor(conn, "buscar_uno", id_)
    actual = dict(zip(columnas, filas[0]))
    print("Enter conserva el valor actual.")
    nombre = input(f"Nombre [{actual['NAME']}]: ").strip() or actual["NAME"]
    descripcion = (
        input(f"Descripción [{actual['DESCRIPTION'] or ''}]: ").strip()
        or actual["DESCRIPTION"]
    )
    precio = pedir_numero("Precio", Decimal, Decimal(str(actual["PRICE"])))
    print("Categorías disponibles:")
    mostrar(*llamar_cursor(conn, "listar_categorias"))
    categoria = pedir_numero("ID de categoría", int, actual["CATEGORY_ID"])
    marca = input(f"Marca [{actual['BRAND'] or ''}]: ").strip() or actual["BRAND"]
    modelo = input(f"Modelo [{actual['MODEL'] or ''}]: ").strip() or actual["MODEL"]
    activo = pedir_numero("Activo (1 = sí, 0 = no)", int, actual["IS_ACTIVE"])
    conn.cursor().callproc(
        "pkg_product.editar",
        [id_, nombre, descripcion, precio, categoria, marca, modelo, activo],
    )
    print("Producto actualizado.")


def eliminar(conn):
    id_ = pedir_numero("ID del producto a eliminar")
    if input(f"¿Eliminar el producto {id_}? (s/n): ").strip().lower() == "s":
        conn.cursor().callproc("pkg_product.eliminar", [id_])
        print("Producto eliminado.")
    else:
        print("Operación cancelada.")

def desactivar(conn):
    id_ = pedir_numero("ID del producto a desactivar")
    conn.cursor().callproc("pkg_product.desactivar", [id_])
    print("Producto desactivado.")


MENU = {
    "1": ("Listar todo", listar_todo),
    "2": ("Buscar uno", buscar_uno),
    "3": ("Agregar", agregar),
    "4": ("Editar", editar),
    "5": ("Eliminar", eliminar),
    "6": ("Desactivar", desactivar),   # nuevo
}


def main():
    conn = conectar()
    print("Conectado a Oracle", conn.version)
    try:
        while True:
            print("\n=== CRUD PRODUCT (pkg_product) ===")
            for k, (nombre, _) in MENU.items():
                print(f"{k}. {nombre}")
            print("0. Salir")
            opcion = input("Opción: ").strip()
            if opcion == "0":
                break
            if opcion not in MENU:
                print("Opción inválida.")
                continue
            try:
                MENU[opcion][1](conn)
            except oracledb.DatabaseError as e:
                # Muestra el mensaje del package (ORA-20001 ... ORA-20004)
                print("Error:", e.args[0].message.splitlines()[0])
    finally:
        conn.close()


if __name__ == "__main__":
    main()
