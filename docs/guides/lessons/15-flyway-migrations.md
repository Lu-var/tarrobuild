<!-- START OF FILE: docs_lessons_15-flyway-migrations_01_objetivo_y_alcance.md -->
# Documento: docs lessons 15-flyway-migrations 01 objetivo y alcance
---
# Lección 15 — Migraciones de Base de Datos con Flyway

## ¿De dónde venimos?

En la lección 11 configuraste múltiples bases de datos (H2, MySQL, Supabase) con perfiles de Spring Boot. En la lección 14 implementaste comunicación entre microservicios usando RestClient y FeignClient. Tu aplicación crea las tablas automáticamente gracias a `ddl-auto: update` de JPA/Hibernate, pero a medida que crece y conecta con otros servicios, necesitas más control sobre los cambios de esquema.

Esto funciona en desarrollo, pero tiene problemas en producción:

- **Sin control de versiones:** Si cambias el esquema, ¿cómo lo sincronizas con la BD de otros desarrolladores?
- **Cambios irreversibles:** `ddl-auto: update` nunca borra columnas; si cometes un error, queda para siempre
- **Múltiples BDs:** En Supabase necesitas sincronizar cambios de esquema sin código Java
- **Auditoría:** No hay registro de quién cambió qué en la BD

Esta lección introduce **Flyway**, una herramienta profesional de migraciones que soluciona todo esto.

---

## Los dos enfoques

| Enfoque | Tool | Cuándo | Ventajas | Desventajas |
|---------|------|--------|----------|------------|
| **JPA Auto** | Hibernate + `ddl-auto` | Desarrollo local, H2 | Simple, automático | Sin versiones, sin reversión |
| **Migraciones** | Flyway | Desarrollo persistente, producción | Versionado, reversible, profesional | Requiere escribir SQL |

---

## ¿Qué vas a construir?

Al terminar esta lección podrás:

1. Entender cuándo usar JPA automático vs Flyway
2. Configurar Flyway para MySQL y Supabase
3. Escribir migraciones SQL versionadas
4. Aplicar migraciones automáticamente al arrancar
5. Revertir migraciones si cometes un error
6. Mantener múltiples BDs sincronizadas

### Lo que vas a poder explicar

- ¿Por qué Flyway es importante en producción?
- ¿Cómo funciona el versionado de Flyway?
- ¿Qué hace la carpeta `db/migration/` y cómo nombrar archivos?
- ¿Por qué H2 no necesita Flyway si usan JPA?
- ¿Cómo revertir una migración si sale mal?
- ¿Cuál es la diferencia entre Flyway y Liquibase?

---

## Estructura de la Lección

1. **[Este documento](01_objetivo_y_alcance.md)** — Objetivo y alcance
2. **[Guión Paso a Paso](02_guion_paso_a_paso.md)** — Instrucciones prácticas
3. **[Configuración por Perfil](03_configuracion_por_perfil.md)** — YAML + propiedades
4. **[Ejemplos de Migraciones](04_ejemplos_migraciones.md)** — Scripts SQL listos
5. **[Comparación: JPA vs Flyway](05_jpa_vs_flyway.md)** — Cuándo usar cada uno
6. **[Troubleshooting](06_troubleshooting.md)** — Errores y soluciones
7. **[Checklist](07_checklist_rubrica_minima.md)** — Verificación
8. **[Actividad Individual](08_actividad_individual.md)** — Tu tarea





<!-- START OF FILE: docs_lessons_15-flyway-migrations_02_guion_paso_a_paso.md -->
# Documento: docs lessons 15-flyway-migrations 02 guion paso a paso
---
# Lección 15 — Flyway: Guión Paso a Paso

---

## Paso 1: Entender qué es Flyway

Flyway es una herramienta que **versionea cambios de base de datos** como si fuera Git, pero para SQL:

```
Versión 1: V1__create_tickets_table.sql
           → CREATE TABLE tickets (id INT, title VARCHAR...)

Versión 2: V2__add_priority_column.sql
           → ALTER TABLE tickets ADD COLUMN priority VARCHAR

Versión 3: V3__create_users_table.sql
           → CREATE TABLE users (id INT, name VARCHAR...)
```

Cada migración se ejecuta **una sola vez**, en orden. Si la BD no tiene el cambio, Flyway lo aplica automáticamente.

---

## Paso 2: Agregar Flyway al `pom.xml`

```xml
<!-- pom.xml -->

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.3</version>
</dependency>

<!-- Si usas Supabase o MySQL, también agrega el driver de PostgreSQL/MySQL -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <version>9.22.3</version>
</dependency>
```

Ejecuta:
```bash
./mvnw clean install
```

---

## Paso 3: Crear la Carpeta de Migraciones

En `src/main/resources/`, crea esta estructura:

```
Tickets/src/main/resources/
└── db/
    └── migration/
        ├── mysql/
        │   ├── V1__create_tickets_table.sql
        │   └── V2__add_status_column.sql
        └── supabase/
            ├── V1__create_tickets_table.sql
            └── V2__add_status_column.sql
```

> **¿Por qué dos carpetas?** Porque MySQL y PostgreSQL (Supabase) tienen pequeñas diferencias en SQL. H2 no necesita carpeta porque usa JPA automático.

---

## Paso 4: Configurar Flyway en YAML (Solo MySQL y Supabase)

**`application-mysql.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:tickets_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
  
  flyway:
    enabled: true
    locations: classpath:db/migration/mysql
    baseline-on-migrate: true
  
  jpa:
    hibernate:
      ddl-auto: validate  # ← CAMBIAR a 'validate' (no auto, Flyway controla el esquema)
    database-platform: org.hibernate.dialect.MySQL8Dialect
```

**`application-supabase.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:postgres}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  flyway:
    enabled: true
    locations: classpath:db/migration/supabase
    baseline-on-migrate: true
  
  jpa:
    hibernate:
      ddl-auto: validate  # ← CAMBIAR a 'validate' (no auto, Flyway controla el esquema)
    database-platform: org.hibernate.dialect.PostgreSQL10Dialect
```

**`application-h2.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:ticketsdb
    driver-class-name: org.h2.Driver
  
  flyway:
    enabled: false  # ← Flyway DESHABILITADO para H2
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # ← H2 sigue usando JPA automático
    database-platform: org.hibernate.dialect.H2Dialect
```

---

## Paso 5: Restringir DataInitializer al perfil H2

El proyecto base (`Tickets-14`) incluye un `DataInitializer` que inserta tickets y usuarios de prueba al arrancar. Como es un `@Component` sin condición de perfil, actualmente **se ejecuta en todos los perfiles** (H2, MySQL, Supabase).

En esta lección lo restringimos a H2 y delegamos el seed de MySQL/Supabase a una migración Flyway.

### 5.1 Agregar `@Profile("h2")` al DataInitializer

```java
// src/main/java/.../config/DataInitializer.java
@Component
@Profile("h2")          // ← solo corre cuando el perfil activo es h2
public class DataInitializer implements CommandLineRunner {
    // ... sin más cambios
}
```

### 5.2 Crear la migración de seed para MySQL/Supabase

Crea `V2__insert_initial_data.sql` (o el número que corresponda en tu secuencia):

**MySQL** — `src/main/resources/db/migration/mysql/V2__insert_initial_data.sql`
```sql
-- V2__insert_initial_data.sql
INSERT INTO users (name, email) VALUES
  ('Ana Garcia',    'ana.garcia@empresa.com'),
  ('Carlos Lopez',  'carlos.lopez@empresa.com');

INSERT INTO tickets (title, description, status, created_at, estimated_resolution_date, created_by_id) VALUES
  ('Error en login',      'No se puede iniciar sesion con Google',  'NEW',         NOW(), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 1),
  ('Mejora en dashboard', 'Agregar graficos de estadisticas',        'IN_PROGRESS', NOW(), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 2),
  ('Documentacion API',   'Falta documentacion de endpoints',        'NEW',         NOW(), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 1);
```

**Supabase** — `src/main/resources/db/migration/supabase/V2__insert_initial_data.sql`
```sql
-- V2__insert_initial_data.sql
INSERT INTO users (name, email) VALUES
  ('Ana Garcia',    'ana.garcia@empresa.com'),
  ('Carlos Lopez',  'carlos.lopez@empresa.com');

INSERT INTO tickets (title, description, status, created_at, estimated_resolution_date, created_by_id) VALUES
  ('Error en login',      'No se puede iniciar sesion con Google',  'NEW',         NOW(), CURRENT_DATE + INTERVAL '5 days', 1),
  ('Mejora en dashboard', 'Agregar graficos de estadisticas',        'IN_PROGRESS', NOW(), CURRENT_DATE + INTERVAL '5 days', 2),
  ('Documentacion API',   'Falta documentacion de endpoints',        'NEW',         NOW(), CURRENT_DATE + INTERVAL '5 days', 1);
```

> **Resultado:** H2 sigue usando `DataInitializer` en memoria. MySQL y Supabase usan la migración Flyway, que se ejecuta una sola vez y queda registrada en `flyway_schema_history`.

---

## Paso 6: Crear tu Primera Migración

**Archivo:** `src/main/resources/db/migration/mysql/V1__create_tickets_table.sql`

```sql
-- V1__create_tickets_table.sql
-- Crea la tabla inicial de tickets

CREATE TABLE tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estimated_resolution_date DATE NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_status ON tickets(status);
```

**Archivo:** `src/main/resources/db/migration/supabase/V1__create_tickets_table.sql`

```sql
-- V1__create_tickets_table.sql
-- Crea la tabla inicial de tickets (PostgreSQL)

CREATE TABLE tickets (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estimated_resolution_date DATE NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_status ON tickets(status);
```

---

## Paso 7: Ejecutar la App (Flyway Aplica Automáticamente)

```bash
cd Tickets

# Con MySQL
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=mysql"

# Con Supabase
export SPRING_PROFILES_ACTIVE=supabase
./mvnw spring-boot:run
```

**Verifica en los logs:**
```
Successfully validated 1 migration (execution time 12ms)
Successfully applied 1 migration to schema public (execution time 123ms)
```

¡Migración aplicada! ✅

---

## Paso 8: Agregar una Segunda Migración

Cuando necesites cambiar el esquema (ej: agregar columna), crea un nuevo archivo **sin editar el anterior**:

**Archivo:** `src/main/resources/db/migration/mysql/V2__add_priority_column.sql`

```sql
-- V2__add_priority_column.sql
-- Agrega columna de prioridad a tickets

ALTER TABLE tickets ADD COLUMN priority VARCHAR(20) DEFAULT 'MEDIUM' AFTER status;
```

**Archivo:** `src/main/resources/db/migration/supabase/V2__add_priority_column.sql`

```sql
-- V2__add_priority_column.sql
-- Agrega columna de prioridad a tickets (PostgreSQL)

ALTER TABLE tickets ADD COLUMN priority VARCHAR(20) DEFAULT 'MEDIUM';
```

Ejecuta de nuevo: `./mvnw spring-boot:run`

Flyway detecta V2 y la aplica automáticamente. 🚀

---

## Paso 9: Convención de Nombres

Flyway busca archivos con este patrón exacto:

```
V{versión}__{verbo}_{sujeto}.sql
│           │        │
│           │        └─ Tabla o columna afectada (snake_case, en inglés)
│           └─ Acción que realiza la migración (en inglés)
└─ Número secuencial sin ceros a la izquierda (V1, V2, V10…)
```

> **La descripción siempre va en inglés y snake_case.** Esto es estándar en entornos profesionales y facilita la colaboración en equipos multilenguaje.

### Verbos comunes

| Verbo | Uso |
|-------|-----|
| `create_` | Crear tabla o vista: `V1__create_tickets_table.sql` |
| `add_` | Agregar columna o constraint: `V2__add_priority_column.sql` |
| `alter_` | Modificar columna existente: `V3__alter_status_column_type.sql` |
| `drop_` | Eliminar tabla o columna: `V4__drop_legacy_table.sql` |
| `insert_` | Datos iniciales (seed): `V5__insert_initial_data.sql` |
| `rename_` | Renombrar tabla o columna: `V6__rename_users_table.sql` |
| `remove_` | Quitar columna o index: `V7__remove_old_column.sql` |
| `create_idx_` | Crear índice: `V8__create_idx_status.sql` |

### ✅ Correcto

```
V1__create_tickets_table.sql
V2__add_priority_column.sql
V3__create_users_table.sql
V10__create_user_index.sql
```

### ❌ Incorrecto

```
v1_crear_tabla.sql          → minúscula en V, descripción en español
V1_create_table.sql         → un solo guión bajo (se ignora)
V1 create table.sql         → espacios no permitidos
V01__create_table.sql       → cero a la izquierda
V1__CreateTable.sql         → CamelCase en lugar de snake_case
```

---

## Paso 10: Control de Versiones (Git)

Agrupa tus migraciones por versión del proyecto:

```
v1.0:
  - V1__create_tickets_table.sql
  - V2__create_users_table.sql

v1.1:
  - V3__add_priority_column.sql

v2.0:
  - V4__refactor_users_table.sql
```

**Nunca borres una migración** del repositorio. Si necesitas revertir:

```sql
-- V5__revert_incorrect_change.sql
-- Revierte el cambio de V4

DROP COLUMN priority FROM tickets;
```

---

## Paso 11: Tabla de Control (Flyway Schema History)

Flyway crea automáticamente una tabla que registra todas las migraciones:

```
flyway_schema_history
┌────┬────────────────────────────────────┬─────────┬────────────────┐
│ id │ version                            │ success │ execution_time │
├────┼────────────────────────────────────┼─────────┼────────────────┤
│ 1  │ V1__create_tickets_table            │ TRUE    │ 123ms          │
│ 2  │ V2__add_priority_column      │ TRUE    │ 45ms           │
│ 3  │ V3__create_users_table           │ TRUE    │ 89ms           │
└────┴────────────────────────────────────┴─────────┴────────────────┘
```

Flyway consulta esta tabla antes de ejecutar migraciones. Si la migración ya está ahí, **no la ejecuta de nuevo**.

---

## Resumen de Pasos

```
1. Agregar Flyway al pom.xml
         ↓
2. Configurar locations en application-*.yml
         ↓
3. Cambiar ddl-auto a 'validate' (para MySQL/Supabase)
         ↓
4. Crear carpeta db/migration/{mysql,supabase}
         ↓
5. Agregar @Profile("h2") a DataInitializer
         ↓
6. Crear V1__create_tickets_table.sql (esquema)
         ↓
7. Crear V2__insert_initial_data.sql (seed)
         ↓
8. Ejecutar app (Flyway aplica automáticamente)
         ↓
9. Cuando necesites cambios, crear V3__... (sin editar anteriores)
         ↓
✅ Flyway aplica solo las migraciones nuevas
```

---

*[← Volver a Lección 15](01_objetivo_y_alcance.md)*





<!-- START OF FILE: docs_lessons_15-flyway-migrations_03_configuracion_por_perfil.md -->
# Documento: docs lessons 15-flyway-migrations 03 configuracion por perfil
---
# Lección 15 — Configuración por Perfil

## `application.yml` (Base Común)

```yaml
spring:
  application:
    name: Tickets
  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  cloud:
    openfeign:
      client:
        config:
          audit-service:
            connect-timeout: 5000
            read-timeout: 10000
            logger-level: BASIC
          default:
            connect-timeout: 5000
            read-timeout: 10000

server:
  port: 8080
  servlet:
    context-path: "/ticket-app"

logging:
  level:
    root: INFO
    cl.duoc.fullstack: DEBUG
```

---

## `application-h2.yml` (H2 — Sin Flyway)

H2 sigue usando JPA automático. **Flyway deshabilitado**.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:tickets_db
    driverClassName: org.h2.Driver
    username: sa
    password: ''
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop  # ← H2 sigue con automático
  flyway:
    enabled: false  # ← H2 NO usa Flyway
```

> **Datos iniciales en H2:** el proyecto usa `DataInitializer.java` (`@Component CommandLineRunner`) que inserta tickets y usuarios de prueba al arrancar. En esta lección agrégale `@Profile("h2")` para que solo corra en H2. Para MySQL y Supabase usa una migración Flyway (`V2__insert_initial_data.sql`).

---

## `application-mysql.yml` (MySQL — Con Flyway)

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:tickets_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
  flyway:
    enabled: true                              # ← Flyway HABILITADO
    locations: classpath:db/migration/mysql    # ← Carpeta con migraciones
    baseline-on-migrate: true                  # ← Crea tabla de control si no existe
    out-of-order: false                        # ← Migraciones deben ser ordenadas
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: validate                       # ← VALIDAR SOLO (no auto)
    properties:
      hibernate:
        format_sql: true
```

---

## `application-supabase.yml` (Supabase/PostgreSQL — Con Flyway)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:postgres}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD}
  flyway:
    enabled: true                                # ← Flyway HABILITADO
    locations: classpath:db/migration/supabase   # ← Carpeta con migraciones
    baseline-on-migrate: true                    # ← Crea tabla de control si no existe
    out-of-order: false                          # ← Migraciones deben ser ordenadas
    schemas: public                              # ← Schema por defecto
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate                        # ← VALIDAR SOLO (no auto)
    properties:
      hibernate:
        format_sql: true
```

---

## Opciones Importantes de Flyway

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| `enabled` | `true`/`false` | Habilita/deshabilita Flyway |
| `locations` | `classpath:db/migration/mysql` | Dónde buscar las migraciones |
| `baseline-on-migrate` | `true` | Crea tabla `flyway_schema_history` si no existe |
| `out-of-order` | `false` | Las migraciones deben aplicarse en orden (recomendado) |
| `schemas` | `public` | Schema de BD donde aplicar (PostgreSQL) |
| `validate-on-migrate` | `true` | Valida migraciones antes de ejecutar |

---

## Cambio Importante en `ddl-auto`

### ❌ Antes (Lección 14 - Sin Flyway)
```yaml
jpa:
  hibernate:
    ddl-auto: update  # Crea/modifica automáticamente
```

### ✅ Después (Lección 15 - Con Flyway)
```yaml
jpa:
  hibernate:
    ddl-auto: validate  # Solo valida, no modifica
```

**¿Por qué?** Porque Flyway **controla** el esquema. Si JPA también modifica, entra en conflicto.

- `update` = JPA puede cambiar la BD (riesgo)
- `validate` = JPA solo valida que todo coincida (seguro)

---

## Estructura de Carpetas en el Proyecto

```
Tickets/
├── src/main/resources/
│   ├── application.yml
│   ├── application-h2.yml
│   ├── application-mysql.yml
│   ├── application-supabase.yml
│   └── db/
│       └── migration/
│           ├── mysql/
│           │   ├── V1__create_tickets_table.sql
│           │   ├── V2__add_priority_column.sql
│           │   └── V3__...
│           └── supabase/
│               ├── V1__create_tickets_table.sql
│               ├── V2__add_priority_column.sql
│               └── V3__...
│
├── pom.xml (con Flyway agregado)
└── .env (con variables de BD)
```

---

## `.env` (Variables de Entorno — Igual que antes)

```env
# Perfil activo
SPRING_PROFILES_ACTIVE=mysql

# MySQL
DB_HOST=localhost
DB_PORT=3306
DB_NAME=tickets_db
DB_USER=root
DB_PASSWORD=

# Supabase (reemplaza los valores anteriores)
# DB_HOST=db.xxxx.supabase.co
# DB_PORT=5432
# DB_NAME=postgres
# DB_USER=postgres
# DB_PASSWORD=your-password
```

**Flyway usa automáticamente las mismas variables de datasource.**

---

## Verificación en Logs

Cuando ejecutas la app, deberías ver:

```
2026-04-16 14:23:45.123 INFO  o.f.c.i.database.DatabaseFactory - Database: MySQL 5.7.43-0-log (detected version 5.7.43-0-log)
2026-04-16 14:23:45.234 INFO  o.f.c.i.s.JdbcTableSchemaHistory - Schema history table "tickets_db"."flyway_schema_history" does not exist yet
2026-04-16 14:23:45.250 INFO  o.f.core.internal.command.DbMigrate - Creating Schema History table "tickets_db"."flyway_schema_history"
2026-04-16 14:23:45.315 INFO  o.f.core.internal.command.DbMigrate - Successfully validated 3 migrations (execution time 15ms)
2026-04-16 14:23:45.401 INFO  o.f.core.internal.command.DbMigrate - Successfully applied 3 migrations to schema "tickets_db" (execution time 198ms)
```

---

*[← Volver a Lección 15](01_objetivo_y_alcance.md)*





<!-- START OF FILE: docs_lessons_15-flyway-migrations_04_ejemplos_migraciones.md -->
# Documento: docs lessons 15-flyway-migrations 04 ejemplos migraciones
---
# Lección 15 — Ejemplos de Migraciones SQL

## Patrón de Nombres

```
V{versión}__lesson_{lección}_{verbo}_{sujeto}.sql
```

| Parte | Regla | Ejemplo |
|-------|-------|---------|
| `V` | Mayúscula obligatoria | `V1`, `V2`, `V10` |
| `{versión}` | Número secuencial global, sin ceros a la izquierda | `1` ✅ — `01` ❌ |
| `__` | Dos guiones bajos (separador obligatorio) | `V1__` ✅ — `V1_` ❌ |
| `lesson_` | Prefijo fijo que identifica el bloque de lección | `lesson_` ✅ |
| `{lección}` | Número de lección donde se introdujo el cambio | `10`, `12`, `13` |
| `{verbo}` | Acción en inglés, snake_case | `create_`, `add_`, `insert_` |
| `{sujeto}` | Tabla o columna afectada, en inglés | `tickets_table`, `initial_tickets` |
| `.sql` | Extensión en minúsculas | `.sql` ✅ — `.SQL` ❌ |

> **Siempre en inglés.** Es el estándar de la industria. Los nombres en español generan problemas en entornos donde el equipo o las herramientas CI/CD son multilenguaje.

### Progresión por lección

Cada lección agrupa una o más migraciones DDL seguidas obligatoriamente de un **seed** (INSERT y/o UPDATE) antes de continuar con la siguiente lección:

```
V1__lesson_10_create_tickets_table.sql      ← DDL
V2__lesson_10_insert_initial_tickets.sql    ← seed ✅ cierra lección 10

V3__lesson_12_create_users_table.sql        ← DDL
V4__lesson_12_add_user_relations_to_tickets.sql  ← DDL
V5__lesson_12_insert_users_and_link_tickets.sql  ← seed ✅ cierra lección 12

V6__lesson_13_create_ticket_history_table.sql    ← DDL
V7__lesson_13_insert_initial_history.sql    ← seed ✅ cierra lección 13
```

### Verbos más usados

```
create_   →  V1__lesson_10_create_tickets_table.sql
add_      →  V4__lesson_12_add_user_relations_to_tickets.sql
alter_    →  V{n}__lesson_{x}_alter_status_column_type.sql
insert_   →  V2__lesson_10_insert_initial_tickets.sql
update_   →  V{n}__lesson_{x}_update_ticket_priority.sql
drop_     →  V{n}__lesson_{x}_drop_legacy_column.sql
rename_   →  V{n}__lesson_{x}_rename_users_table.sql
```

---

## Migración 1: Tabla Inicial (Tickets)

### MySQL

```sql
-- V1__create_tickets_table.sql
CREATE TABLE tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estimated_resolution_date DATE NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_title UNIQUE (title),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### PostgreSQL (Supabase)

```sql
-- V1__create_tickets_table.sql
CREATE TABLE tickets (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estimated_resolution_date DATE NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_valid_status CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX idx_status ON tickets(status);
CREATE INDEX idx_created_at ON tickets(created_at);
```

---

## Migración 2: Agregar Columnas

### MySQL

```sql
-- V2__add_priority_column.sql
ALTER TABLE tickets 
ADD COLUMN priority VARCHAR(20) DEFAULT 'MEDIUM' AFTER status,
ADD COLUMN assigned_to VARCHAR(255);

CREATE INDEX idx_assigned_to ON tickets(assigned_to);
```

### PostgreSQL (Supabase)

```sql
-- V2__add_priority_column.sql
ALTER TABLE tickets
ADD COLUMN priority VARCHAR(20) DEFAULT 'MEDIUM',
ADD COLUMN assigned_to VARCHAR(255);

CREATE INDEX idx_assigned_to ON tickets(assigned_to);
```

---

## Migración 3: Crear Tabla Usuarios

### MySQL

```sql
-- V3__create_users_table.sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email)
);
```

### PostgreSQL (Supabase)

```sql
-- V3__create_users_table.sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email ON users(email);
```

---

## Migración 4: Agregar Foreign Key

### MySQL

```sql
-- V4__add_tickets_users_relation.sql
-- Vincula tickets con usuarios

ALTER TABLE tickets
ADD COLUMN created_by_id INT NOT NULL DEFAULT 1,
ADD COLUMN assigned_to_id INT;

ALTER TABLE tickets
ADD CONSTRAINT fk_tickets_created_by 
FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE RESTRICT,
ADD CONSTRAINT fk_tickets_assigned_to 
FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE SET NULL;

-- Agregar índice para performance
CREATE INDEX idx_created_by_id ON tickets(created_by_id);
CREATE INDEX idx_assigned_to_id ON tickets(assigned_to_id);
```

### PostgreSQL (Supabase)

```sql
-- V4__add_tickets_users_relation.sql
ALTER TABLE tickets
ADD COLUMN created_by_id INTEGER NOT NULL DEFAULT 1,
ADD COLUMN assigned_to_id INTEGER;

ALTER TABLE tickets
ADD CONSTRAINT fk_tickets_created_by 
FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE RESTRICT,
ADD CONSTRAINT fk_tickets_assigned_to 
FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_created_by_id ON tickets(created_by_id);
CREATE INDEX idx_assigned_to_id ON tickets(assigned_to_id);
```

---

## Migración de Seed: Datos Iniciales

Flyway también gestiona la inserción de datos iniciales (seed). Es el mecanismo correcto para MySQL y Supabase.

> ⚠️ **El `DataInitializer` actual corre en todos los perfiles**
>
> `Tickets-14` incluye `DataInitializer.java` (`@Component` + `CommandLineRunner`) que inserta datos de prueba al arrancar. Sin la anotación `@Profile`, se ejecuta en **todos** los perfiles (H2, MySQL, Supabase).
>
> En esta lección debes:
> 1. Agregar `@Profile("h2")` a `DataInitializer` → solo corre en H2
> 2. Crear `V2__insert_initial_data.sql` con los mismos datos para MySQL y Supabase
>
> | Mecanismo | H2 | MySQL | Supabase |
> |-----------|:--:|:-----:|:--------:|
> | `DataInitializer` (`@Profile("h2")`) | ✅ | ❌ | ❌ |
> | `V2__insert_initial_data.sql` (Flyway) | ❌ | ✅ | ✅ |

### MySQL

```sql
-- V5__insert_initial_data.sql
-- Inserta usuarios y tickets de ejemplo

INSERT INTO users (email, name, password_hash) VALUES
('admin@example.com', 'Admin User', '$2a$10$...'),
('developer@example.com', 'Developer User', '$2a$10$...');

INSERT INTO tickets (title, description, status, priority, created_by_id) VALUES
('Bug: Login no funciona', 'El login falla con email/password incorrectos', 'NEW', 'HIGH', 1),
('Feature: Dark mode', 'Agregar tema oscuro a la aplicación', 'IN_PROGRESS', 'MEDIUM', 2);
```

### PostgreSQL (Supabase)

```sql
-- V5__insert_initial_data.sql
INSERT INTO users (email, name, password_hash) VALUES
('admin@example.com', 'Admin User', '$2a$10$...'),
('developer@example.com', 'Developer User', '$2a$10$...');

INSERT INTO tickets (title, description, status, priority, created_by_id) VALUES
('Bug: Login no funciona', 'El login falla con email/password incorrectos', 'NEW', 'HIGH', 1),
('Feature: Dark mode', 'Agregar tema oscuro a la aplicación', 'IN_PROGRESS', 'MEDIUM', 2);
```

---

## Migración 6: Cambiar Tipo de Dato

### MySQL

```sql
-- V6__change_description_column_type.sql
-- Amplía la columna description de TEXT a LONGTEXT

ALTER TABLE tickets MODIFY COLUMN description LONGTEXT;
```

### PostgreSQL (Supabase)

```sql
-- V6__change_description_column_type.sql
ALTER TABLE tickets ALTER COLUMN description TYPE TEXT;
```

---

## Migración 7: Crear Vista (View)

### MySQL

```sql
-- V7__create_tickets_by_user_view.sql
-- Vista que muestra tickets agrupados por usuario

CREATE VIEW tickets_by_user AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    COUNT(t.id) as total_tickets,
    SUM(CASE WHEN t.status = 'NEW' THEN 1 ELSE 0 END) as new_tickets
FROM users u
LEFT JOIN tickets t ON t.created_by_id = u.id
GROUP BY u.id, u.name;
```

### PostgreSQL (Supabase)

```sql
-- V7__create_tickets_by_user_view.sql
CREATE VIEW tickets_by_user AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    COUNT(t.id) as total_tickets,
    SUM(CASE WHEN t.status = 'NEW' THEN 1 ELSE 0 END) as new_tickets
FROM users u
LEFT JOIN tickets t ON t.created_by_id = u.id
GROUP BY u.id, u.name;
```

---

## Migración 8: Revertir Cambio (Rollback Manual)

Si cometiste un error en V4 y necesitas revertirlo:

### MySQL

```sql
-- V8__revert_foreign_keys_v4.sql
-- Revierte los cambios de V4

ALTER TABLE tickets DROP FOREIGN KEY fk_tickets_assigned_to;
ALTER TABLE tickets DROP FOREIGN KEY fk_tickets_created_by;
ALTER TABLE tickets DROP COLUMN assigned_to_id;
ALTER TABLE tickets DROP COLUMN created_by_id;
```

### PostgreSQL (Supabase)

```sql
-- V8__revert_foreign_keys_v4.sql
ALTER TABLE tickets DROP CONSTRAINT fk_tickets_assigned_to;
ALTER TABLE tickets DROP CONSTRAINT fk_tickets_created_by;
ALTER TABLE tickets DROP COLUMN assigned_to_id;
ALTER TABLE tickets DROP COLUMN created_by_id;
```

---

## Diferencias Clave: MySQL vs PostgreSQL

| Aspecto | MySQL | PostgreSQL |
|---------|-------|-----------|
| Auto-increment | `AUTO_INCREMENT` | `SERIAL` |
| Timestamp actual | `DEFAULT CURRENT_TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` |
| Alterar columna | `MODIFY COLUMN` | `ALTER COLUMN ... TYPE` |
| Foreign keys | `CONSTRAINT fk_... FOREIGN KEY` | `CONSTRAINT fk_... FOREIGN KEY` |
| Índices | `INDEX idx_name` | `CREATE INDEX idx_name` |
| Comentarios | `-- comentario` | `-- comentario` |
| Booleanos | `TINYINT(1)` | `BOOLEAN` |
| Strings ilimitados | `LONGTEXT` | `TEXT` |

---

## ✅ Checklist para Migraciones

Antes de crear una migración, verifica:

- ✅ El nombre sigue patrón `V[número]__[descripción].sql`
- ✅ Dos guiones bajos obligatorios `__`
- ✅ Número es secuencial (V1, V2, V3, no saltes)
- ✅ SQL sintácticamente correcto (prueba en tu BD)
- ✅ Existe para MySQL y PostgreSQL si ambos usan Flyway
- ✅ No editas migraciones viejas (solo creas nuevas)
- ✅ Commitas los archivos al repositorio
- ✅ Documentaste qué hace en un comentario al inicio

---

*[← Volver a Lección 15](01_objetivo_y_alcance.md)*





<!-- START OF FILE: docs_lessons_15-flyway-migrations_05_jpa_vs_flyway.md -->
# Documento: docs lessons 15-flyway-migrations 05 jpa vs flyway
---
# Lección 15 — JPA vs Flyway: Cuándo Usar Cada Uno

## Tabla Comparativa

| Aspecto | JPA (`ddl-auto`) | Flyway |
|---------|-----------------|--------|
| **Control de versión** | ❌ No | ✅ Sí (V1, V2, V3...) |
| **Reversión** | ❌ Complicada | ✅ Fácil (crear nueva migración) |
| **Ambiente** | Desarrollo | Producción |
| **Auditoría** | ❌ No hay registro | ✅ Tabla `flyway_schema_history` |
| **Equipo sincronización** | ❌ Problemático | ✅ Migraciones en Git |
| **SQL manual** | ❌ No | ✅ Sí |
| **Learning curve** | ✅ Fácil | ⚠️ Intermedio |
| **Seguridad** | ❌ Cambios automáticos | ✅ Cambios controlados |

---

## Decisión Rápida

```
¿Dónde está tu aplicación?
│
├─ Desarrollo local, solo tú
│  └─ Usa: JPA (ddl-auto: update)
│     Ventaja: Sin configuración extra
│
├─ Desarrollo local, múltiples desarrolladores
│  └─ Usa: Flyway
│     Ventaja: Sincronización en Git
│
├─ Staging / Producción
│  └─ Usa: Flyway
│     Ventaja: Auditoría y control
│
└─ Tests / H2
   └─ Usa: JPA (ddl-auto: create-drop)
      Ventaja: Limpieza automática
```

---

## Escenario 1: Solo Desarrollo Local (H2 o MySQL)

**Configuración:**
```yaml
jpa:
  hibernate:
    ddl-auto: update
```

**Flujo:**
1. Modificas tu entidad Java (`@Entity`)
2. Arrancar app
3. JPA **automáticamente** crea/modifica tablas
4. Listo, sin hacer nada más

✅ **Ventajas:**
- Desarrollo rápido
- No escribir SQL

❌ **Desventajas:**
- Sin versiones
- Cambios no reversibles
- Si hay error, queda en la BD

---

## Escenario 2: Múltiples Desarrolladores

**Problema sin Flyway:**
```
Dev A:          Modifica Ticket.java   (agrega columna)
Dev B:          Modifica Ticket.java   (agrega columna diferente)
                └─ CONFLICTO: ¿Qué cambios aplica JPA?
```

**Solución con Flyway:**
```
Dev A:          V1__add_priority_column.sql          (commitea)
                ├─ Los demás: git pull (reciben la migración)
Dev B:          V2__add_resolution_status_column.sql (commitea)
                ├─ Los demás: git pull (reciben V1 y V2)
                └─ Cuando arrancan: Flyway aplica ambas en orden
```

✅ **Ventajas:**
- Control de versiones (Git)
- Cada cambio es traceable
- Sincronización automática

❌ **Desventajas:**
- Deben escribir SQL

---

## Escenario 3: Producción

**SIN Flyway (PELIGROSO):**
```
Producción:  ddl-auto: update
             └─ ¿Quién cambió la BD? ¿Cuándo?
             └─ ¿Es reversible si falla?
             └─ RIESGO: Cambios no auditados
```

**CON Flyway (SEGURO):**
```
Producción:  V1, V2, V3 en DB
             └─ Tabla flyway_schema_history registra TODO
             └─ Cada cambio está en Git
             └─ Si falla: creas V4 para revertir
             └─ SEGURO: Auditado y reversible
```

---

## Comparación Técnica

### JPA automático

```java
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;           // JPA crea automáticamente
    private String status;          // JPA modifica automáticamente
}

// Cambias el código, arrancar app → JPA actualiza BD
```

### Flyway

```sql
-- V1__create_tickets_table.sql
CREATE TABLE tickets (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    status VARCHAR(50)
);

-- V2__add_priority_column.sql
ALTER TABLE tickets ADD COLUMN priority VARCHAR(20);
```

**Con Flyway, controlas exactamente qué SQL se ejecuta.**

---

## Recomendación por Etapa

| Etapa | Perfil | Herramienta | Configuración |
|-------|--------|-------------|---------------|
| **Aprendizaje (Semana 1-2)** | h2 | JPA | `ddl-auto: create-drop` |
| **Desarrollo Inicial** | h2 / mysql | JPA | `ddl-auto: update` |
| **Trabajo en Equipo** | mysql | Flyway | `locations: db/migration/mysql` |
| **Staging** | supabase | Flyway | `locations: db/migration/supabase` |
| **Producción** | supabase | Flyway | `ddl-auto: validate` |

---

## Migración: De JPA a Flyway

Si ya usabas JPA (`update`) y quieres pasar a Flyway:

### Paso 1: Exportar esquema actual

```bash
# MySQL
mysqldump -u root tickets_db --no-data > V1__current_schema.sql

# PostgreSQL (Supabase)
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME --schema-only > V1__current_schema.sql
```

### Paso 2: Crear V1 en `db/migration/`

Copia el esquema exportado a:
- `src/main/resources/db/migration/mysql/V1__current_schema.sql`
- `src/main/resources/db/migration/supabase/V1__current_schema.sql`

### Paso 3: Cambiar configuración

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration/mysql
  baseline-on-migrate: true

jpa:
  hibernate:
    ddl-auto: validate  # ← Cambiar a validate
```

### Paso 4: Ejecutar

```bash
./mvnw spring-boot:run
```

Flyway crea la tabla de historial y registra V1 como aplicada (sin re-ejecutar).

---

## ✅ Checklist de Decisión

Antes de elegir, pregúntate:

- ¿Trabajo en equipo? → Flyway
- ¿Múltiples BDs (dev, staging, prod)? → Flyway
- ¿Necesito auditoría de cambios? → Flyway
- ¿Solo desarrollo local de prueba? → JPA (update)
- ¿H2 para tests? → JPA (create-drop)
- ¿Es producción? → Flyway (SIEMPRE)

---

*[← Volver a Lección 15](01_objetivo_y_alcance.md)*





<!-- START OF FILE: docs_lessons_15-flyway-migrations_06_troubleshooting.md -->
# Documento: docs lessons 15-flyway-migrations 06 troubleshooting
---
# Lección 15 — Troubleshooting: Errores Comunes

## Error 1: "Migration checksum mismatch"

**Síntoma:**
```
org.flywaydb.core.api.FlywayException: Validate failed: 
Migration checksum mismatch for migration V1__create_tickets_table.sql
```

**Causa:** Modificaste un archivo de migración después de haberlo ejecutado.

**Solución:**
❌ **No hagas esto:**
```sql
-- V1__create_tickets_table.sql (ORIGINAL - ya ejecutado)
CREATE TABLE tickets (id INT, title VARCHAR);

-- Luego lo editaste (INCORRECTO)
CREATE TABLE tickets (id INT, title VARCHAR, description TEXT);
```

✅ **Haz esto:**
```sql
-- V1__create_tickets_table.sql (ORIGINAL - sin cambios)
CREATE TABLE tickets (id INT, title VARCHAR);

-- V2__add_description_column.sql (NUEVA migración)
ALTER TABLE tickets ADD COLUMN description TEXT;
```

**Pasos para recuperarse:**
1. Revert los cambios a V1
2. Crea V2 con los cambios
3. Ejecuta app de nuevo

---

## Error 2: "Failed to validate migration"

**Síntoma:**
```
org.flywaydb.core.api.FlywayException: Validate failed: 
Schema contains object 'flyway_schema_history' which is not found in migration
```

**Causa:** Conflicto entre Flyway y JPA.

**Solución:**
Asegúrate que `ddl-auto` sea `validate`, no `update`:

```yaml
jpa:
  hibernate:
    ddl-auto: validate  # ← DEBE ser validate, no update
```

Si ya cambió a `update`, cambia a `validate` y reinicia.

---

## Error 3: "No migrations found"

**Síntoma:**
```
org.flywaydb.core.api.FlywayException: 
No migrations found at location 'classpath:db/migration/mysql'
```

**Causa:** Carpeta de migraciones no existe o está mal nombrada.

**Solución:**
Verifica la estructura:

```
✅ Correcto:
src/main/resources/
└── db/
    └── migration/
        ├── mysql/
        │   └── V1__create_table.sql
        └── supabase/
            └── V1__create_table.sql

❌ Incorrecto:
src/main/resources/
└── migrations/         (mal nombre)
    └── V1__create_table.sql

❌ Incorrecto:
src/main/resources/
└── db/
    └── migration/
        └── V1__create_table.sql (sin subcarpeta mysql/supabase)
```

---

## Error 4: "Syntax Error in migration"

**Síntoma:**
```
org.flywaydb.core.api.FlywayException: 
Unable to parse statement in migration file 'V1__create_table.sql'
```

**Causa:** SQL sintácticamente incorrecto.

**Solución:**
1. Prueba el SQL directamente en tu BD (phpMyAdmin, DBeaver, etc)
2. Verifica que sea la sintaxis correcta para **MySQL** o **PostgreSQL**

**Errores comunes:**

MySQL vs PostgreSQL:
```sql
❌ MySQL: AUTO_INCREMENT (MySQL es específico)
✅ PostgreSQL: SERIAL

❌ PostgreSQL: CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP (MySQL específico)
✅ PostgreSQL: DEFAULT CURRENT_TIMESTAMP
```

---

## Error 5: "Foreign key constraint fails"

**Síntoma:**
```
org.flywaydb.core.api.FlywayException: 
Unable to execute migration: Syntax error or access violation: 
1064 Cannot delete or update a parent row: a foreign key constraint fails
```

**Causa:** Estás intentando crear o modificar un FK con datos incompatibles.

**Solución:**
Verifica el orden de las migraciones:

```sql
❌ Incorrecto (V1 intenta FK a tabla que no existe):
-- V1__add_users_fk.sql
ALTER TABLE tickets ADD CONSTRAINT fk_user
FOREIGN KEY (user_id) REFERENCES users(id);

✅ Correcto (crear tabla primero):
-- V1__create_users_table.sql
CREATE TABLE users (id INT PRIMARY KEY);

-- V2__create_tickets_table.sql
CREATE TABLE tickets (id INT, user_id INT);

-- V3__add_users_fk.sql
ALTER TABLE tickets ADD CONSTRAINT fk_user
FOREIGN KEY (user_id) REFERENCES users(id);
```

---

## Error 6: "Connection refused" durante migración

**Síntoma:**
```
java.sql.SQLException: Connection refused
```

**Causa:** Flyway intenta conectar a la BD pero no está disponible.

**Solución:**

Para MySQL:
```bash
# Verifica que XAMPP está corriendo
# Ve a: http://localhost/phpmyadmin
# Verifica que la BD "tickets_db" existe
```

Para Supabase:
```bash
# Verifica credenciales en .env
echo $DB_HOST
echo $DB_USER
echo $DB_PASSWORD

# Verifica que el IP está en IP whitelist (Supabase → Settings)
# Verifica conexión a internet
```

---

## Error 7: "Flyway schema history table is read-only"

**Síntoma:**
```
org.flywaydb.core.api.FlywayException: 
Schema history table is read-only
```

**Causa:** No tienes permisos para escribir en la tabla.

**Solución:**
Verifica permisos de usuario en la BD:

MySQL:
```sql
GRANT ALL PRIVILEGES ON tickets_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

PostgreSQL (Supabase):
- Asegúrate que usas el usuario correcto (`postgres`)
- Verifica en Supabase → Settings que el usuario tiene permisos

---

## Error 8: "Timeout waiting for migration"

**Síntoma:**
```
org.flywaydb.core.api.FlywayException: 
Timeout waiting for migration to complete
```

**Causa:** La migración tarda demasiado (probablemente por datos grandes).

**Solución:**
1. Aumenta el timeout en `application-*.yml`:
```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 60000  # 60 segundos
      maximum-pool-size: 10
```

2. O optimiza tu SQL (agrega índices, etc)

---

## Error 9: "Cannot drop table/column in production"

**Síntoma:** No error específico, pero Flyway rechaza el cambio.

**Causa:** Por seguridad, algunos hosts bloquean DROP.

**Solución:**
Si realmente quieres borrar (después de auditar):

```sql
-- V10__remove_old_column.sql
-- Después de auditar que nada la usa

ALTER TABLE tickets DROP COLUMN deprecated_column;
```

En producción, mejor crear una migración de "soft delete":

```sql
-- V10__mark_column_deprecated.sql
ALTER TABLE tickets ADD COLUMN is_deprecated BOOLEAN DEFAULT false;

-- Luego en V11:
-- ALTER TABLE tickets DROP COLUMN old_column;  (después de confirmar)
```

---

## Verificación Rápida

```bash
# Ver todas las migraciones aplicadas
# En MySQL:
SELECT * FROM flyway_schema_history;

# En Supabase:
SELECT * FROM flyway_schema_history;
```

---

*[← Volver a Lección 15](01_objetivo_y_alcance.md)*





<!-- START OF FILE: docs_lessons_15-flyway-migrations_07_checklist_rubrica_minima.md -->
# Documento: docs lessons 15-flyway-migrations 07 checklist rubrica minima
---
# Lección 15 — Checklist y Rúbrica Mínima

## ✅ Checklist de Completitud

### Configuración

- [ ] `pom.xml` contiene dependencia Flyway (versión ≥ 9.0)
- [ ] Carpeta `src/main/resources/db/migration/mysql/` existe
- [ ] Carpeta `src/main/resources/db/migration/supabase/` existe
- [ ] `application-mysql.yml` tiene `flyway.enabled: true`
- [ ] `application-supabase.yml` tiene `flyway.enabled: true`
- [ ] `application-h2.yml` tiene `flyway.enabled: false`
- [ ] Todos los YAML tienen `ddl-auto: validate` (MySQL/Supabase) o `create-drop` (H2)

### Migraciones

- [ ] `V1__create_tickets_table.sql` existe en mysql/ y supabase/
- [ ] `V2__create_users_table.sql` existe en mysql/ y supabase/
- [ ] `V3__add_tickets_users_relation.sql` existe en mysql/ y supabase/
- [ ] Todos los nombres siguen patrón `V[número]__[descripción].sql`
- [ ] SQL es syntácticamente correcto (probado en BD)
- [ ] Diferencias MySQL vs PostgreSQL son correctas

### Ejecución

- [ ] App arranca sin errores con `SPRING_PROFILES_ACTIVE=mysql`
- [ ] App arranca sin errores con `SPRING_PROFILES_ACTIVE=supabase`
- [ ] App arranca sin errores con `SPRING_PROFILES_ACTIVE=h2` (JPA automático)
- [ ] Logs muestran: "Successfully applied 3 migrations"
- [ ] Tabla `flyway_schema_history` tiene 3 filas
- [ ] API `/ticket-app/tickets` responde correctamente

### Documentación

- [ ] Leí lección 15 completa
- [ ] Entiendo diferencia JPA vs Flyway
- [ ] Puedo explicar por qué H2 no usa Flyway
- [ ] Documenté en comentarios SQL qué hace cada migración

### Git

- [ ] Migraciones están en repositorio
- [ ] `.env` NO está en repositorio (está en `.gitignore`)
- [ ] Commit message es descriptivo: `feat: agregar Flyway migrations`

---

## 🎓 Rúbrica de Evaluación

### 1. Configuración Flyway (25%)

| Criterio | Insuficiente | Satisfactorio | Excelente |
|----------|-------------|--------------|-----------|
| Dependencia en pom.xml | ❌ Falta | ✅ Presente | ✅ + versión correcta |
| Configuración YAML | ❌ Incompleta | ✅ Correcta | ✅ + comentarios explicativos |
| Carpetas de migraciones | ❌ Estructura mal | ✅ Correcta | ✅ + bien organizadas |
| `ddl-auto` cambiado | ❌ Sigue `update` | ✅ Cambió a `validate` | ✅ + diferenciado por perfil |

### 2. Migraciones SQL (40%)

| Criterio | Insuficiente | Satisfactorio | Excelente |
|----------|-------------|--------------|-----------|
| Cantidad | ❌ < 2 migraciones | ✅ 3 migraciones | ✅ 4+ migraciones |
| Nombres | ❌ Incorrecto | ✅ `V[num]__[desc].sql` | ✅ + descriptivos |
| Sintaxis MySQL | ❌ Con errores | ✅ Correcta | ✅ + optimizada |
| Sintaxis PostgreSQL | ❌ No diferenciada | ✅ Correcta | ✅ + con índices |
| Relaciones | ❌ Sin FK | ✅ Con FK simples | ✅ + con constraints |

### 3. Ejecución (20%)

| Criterio | Insuficiente | Satisfactorio | Excelente |
|----------|-------------|--------------|-----------|
| App arranca (MySQL) | ❌ Errores | ✅ Arranca | ✅ Sin warnings |
| App arranca (Supabase) | ❌ Errores | ✅ Arranca | ✅ Sin warnings |
| Logs Flyway | ❌ "Failed" | ✅ "Successfully applied" | ✅ + detallados |
| Tabla de historial | ❌ No existe | ✅ Existe | ✅ + 3+ filas |

### 4. Conocimiento (15%)

| Criterio | Insuficiente | Satisfactorio | Excelente |
|----------|-------------|--------------|-----------|
| Explica JPA vs Flyway | ❌ No sabe | ✅ Explica diferencias | ✅ + casos de uso |
| Entiende versionado | ❌ Confundido | ✅ Claro | ✅ + profundo |
| Conoce convenciones | ❌ Viola | ✅ Sigue | ✅ + explica por qué |
| Puede troubleshoot | ❌ Pierde | ✅ Resuelve básicos | ✅ + errores complejos |

---

## 📊 Cálculo de Nota

```
Configuración:  25% × (puntos / 4)
Migraciones:    40% × (puntos / 5)
Ejecución:      20% × (puntos / 4)
Conocimiento:   15% × (puntos / 4)
─────────────────────────────────
TOTAL:          100%
```

**Ejemplos:**
- 3/4 configuración + 5/5 migraciones + 3/4 ejecución + 4/4 conocimiento = 93%
- 2/4 configuración + 3/5 migraciones + 2/4 ejecución + 2/4 conocimiento = 58%

---

## 🚩 Red Flags (Falla Automática)

- ❌ Migraciones editadas después de ser ejecutadas
- ❌ `.env` con credenciales en repositorio
- ❌ `ddl-auto: update` en MySQL/Supabase
- ❌ Nombres de migración incorrectos (V01, v1, V1_, etc)
- ❌ App no arranca o logs muestran "Failed"
- ❌ SQL con errores de sintaxis

---

*[← Volver a Lección 15](01_objetivo_y_alcance.md)*





<!-- START OF FILE: docs_lessons_15-flyway-migrations_08_actividad_individual.md -->
# Documento: docs lessons 15-flyway-migrations 08 actividad individual
---
# Lección 15 — Actividad Individual: Migración Flyway de `Category`

## Contexto

En la lección 05 creaste el recurso `Category` con persistencia en memoria. En la lección 10 lo migraste a JPA con `@Entity` y `JpaRepository`. Ahora que el proyecto usa Flyway para controlar el esquema, **JPA ya no puede crear ni modificar tablas automáticamente** en MySQL y Supabase (`ddl-auto: validate`).

Tu tarea es escribir las migraciones Flyway que crean la tabla `categories` y la vinculan con `tickets`, continuando la secuencia de versiones del proyecto.

---

## Lo que debes entregar

Las migraciones van dentro de `src/main/resources/db/migration/` y siguen la convención establecida en clase:

```
V{versión}__lesson_15_{verbo}_{sujeto}.sql
```

Las versiones deben continuar la secuencia existente (V7 es la última del guión), así que tu actividad parte desde **V8**.

---

## Parte 1: Crear la tabla `categories`

Crea los archivos para ambos motores:

- `db/migration/mysql/V8__lesson_15_create_categories_table.sql`
- `db/migration/supabase/V8__lesson_15_create_categories_table.sql`

La tabla debe reflejar exactamente la entidad `Category` que definiste en la lección 10:

| Campo | Tipo Java | Columna MySQL | Columna PostgreSQL |
|-------|-----------|---------------|--------------------|
| `id` | `Long` | `BIGINT AUTO_INCREMENT` | `BIGSERIAL` |
| `name` | `String` | `VARCHAR(100) NOT NULL UNIQUE` | `VARCHAR(100) NOT NULL UNIQUE` |
| `description` | `String` | `TEXT` | `TEXT` |

Incluye al menos un índice sobre `name`.

---

## Parte 2: Seed de categorías iniciales

Cada bloque de lección debe terminar con un seed. Crea:

- `db/migration/mysql/V9__lesson_15_insert_initial_categories.sql`
- `db/migration/supabase/V9__lesson_15_insert_initial_categories.sql`

Inserta las mismas categorías que usabas como datos de prueba en memoria desde la lección 05:

| name | description |
|------|-------------|
| `Bug` | `Problema o error que afecta el funcionamiento esperado` |
| `Feature` | `Nueva funcionalidad solicitada por el usuario` |
| `Mejora` | `Cambio menor que optimiza una funcionalidad existente` |

---

## Parte 3 (opcional): Vincular `Category` con `Ticket`

Si en la lección 12 ya agregaste `category_id` a `Ticket`, crea la migración que lo formaliza en la BD:

- `db/migration/mysql/V10__lesson_15_add_category_to_tickets.sql`
- `db/migration/supabase/V10__lesson_15_add_category_to_tickets.sql`

```sql
-- MySQL
ALTER TABLE tickets
    ADD COLUMN category_id BIGINT,
    ADD CONSTRAINT fk_tickets_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
```

```sql
-- PostgreSQL
ALTER TABLE tickets
    ADD COLUMN category_id BIGINT,
    ADD CONSTRAINT fk_tickets_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
```

---

## Verificación

Arranca la app con MySQL o Supabase y comprueba los logs:

```
Successfully applied 2 migrations to schema ... (V8, V9)
```

Luego verifica en tu cliente de BD:

| Verificación | Resultado esperado |
|---|---|
| `SELECT * FROM flyway_schema_history` | 9 filas (V1-V9) |
| `SELECT * FROM categories` | 3 filas (Bug, Feature, Mejora) |
| `DESCRIBE categories` / `\d categories` | columnas `id`, `name`, `description` |
| App arranca sin errores con `ddl-auto: validate` | Hibernate valida sin crear nada |

---

## Criterios de evaluación

| Criterio | Puntaje |
|---|---|
| V8 crea la tabla con columnas y tipos correctos (MySQL y Supabase) | 35% |
| V9 inserta las 3 categorías iniciales | 25% |
| Los nombres de archivo siguen el patrón `V{n}__lesson_15_{verbo}_{sujeto}.sql` | 20% |
| La app arranca sin errores en el perfil mysql o supabase | 20% |

---

## Desafío opcional

Si V10 (relación `category_id` en `tickets`) ya funciona, agrega el seed que vincula los tickets existentes a una categoría:

```sql
-- V11__lesson_15_update_ticket_categories.sql
UPDATE tickets SET category_id = 1 WHERE title = 'Error en login';
UPDATE tickets SET category_id = 3 WHERE title = 'Mejora en dashboard';
UPDATE tickets SET category_id = 2 WHERE title = 'Documentacion API';
```

> **¿Por qué UPDATE y no INSERT?** Los tickets ya existen (fueron insertados en V2 y vinculados a usuarios en V5). Aquí solo les asignamos su categoría, no los recreamos.

---

*[← Volver a Lección 15](01_objetivo_y_alcance.md)*






<!-- START OF FILE: docs_lessons_15-flyway-migrations_README.md -->
# Documento: docs lessons 15-flyway-migrations README
---
# Lección 15 — Migraciones de Base de Datos con Flyway

**Aprende a versionear cambios de base de datos como código con Flyway. Implementa migraciones profesionales que funcionan en MySQL, Supabase y H2.**

---

## 📚 Contenidos

| Documento | Duración | Para |
|-----------|----------|------|
| **01. Objetivo y Alcance** | 5 min | Entender qué aprenderás |
| **02. Guión Paso a Paso** ⭐ | 20 min | Instrucciones prácticas |
| **03. Configuración por Perfil** | 10 min | YAML + properties |
| **04. Ejemplos de Migraciones** | 15 min | Scripts SQL listos |
| **05. JPA vs Flyway** | 10 min | Cuándo usar cada uno |
| **06. Troubleshooting** | 10 min | Errores y soluciones |
| **07. Checklist** | 5 min | Verificación |
| **08. Actividad Individual** | - | Tu tarea |

---

## 🎯 Quick Start (5 min)

### 1. Agregar Flyway a `pom.xml`
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.3</version>
</dependency>
```

### 2. Configurar YAML (MySQL)
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration/mysql
  
  jpa:
    hibernate:
      ddl-auto: validate  # CAMBIAR a validate
```

### 3. Crear Migración
```sql
-- src/main/resources/db/migration/mysql/V1__create_tickets_table.sql
CREATE TABLE tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'NEW'
);
```

### 4. Ejecutar
```bash
export SPRING_PROFILES_ACTIVE=mysql
./mvnw spring-boot:run
```

✅ Flyway aplica automáticamente V1 y crea tabla `flyway_schema_history`

---

## 🔑 Conceptos Clave

### ¿Qué es Flyway?

Herramienta que **versionea cambios de BD como Git**:
- V1__create_table.sql
- V2__add_column.sql
- V3__create_index.sql

Cada migración se ejecuta **una sola vez**, en orden. Flyway registra el historial en `flyway_schema_history`.

### Diferencia: JPA vs Flyway

| Aspecto | JPA | Flyway |
|---------|-----|--------|
| Automático | ✅ | ❌ |
| Versionado | ❌ | ✅ |
| Reversible | ❌ | ✅ |
| Producción | ❌ | ✅ |
| H2 | ✅ | ❌ |

---

## 📂 Estructura

```
src/main/resources/
├── application.yml
├── application-h2.yml (Flyway disabled)
├── application-mysql.yml (Flyway enabled)
├── application-supabase.yml (Flyway enabled)
└── db/migration/
    ├── mysql/
    │   ├── V1__create_tickets_table.sql
    │   ├── V2__add_priority_column.sql
    │   └── V3__...
    └── supabase/
        ├── V1__create_tickets_table.sql
        ├── V2__add_priority_column.sql
        └── V3__...
```

---

## ✅ Checklist

- [ ] Flyway en `pom.xml`
- [ ] Carpetas `db/migration/{mysql,supabase}/` creadas
- [ ] `application-*.yml` configurado
- [ ] `ddl-auto: validate` en MySQL/Supabase
- [ ] V1, V2, V3 migraciones creadas
- [ ] App arranca sin errores
- [ ] Logs muestran "Successfully applied 3 migrations"
- [ ] Tabla `flyway_schema_history` con 3 filas

---

## 🚀 Sigue el Guión

Comienza con **[02. Guión Paso a Paso](02_guion_paso_a_paso.md)** para instrucciones detalladas.

---

*Lección 15 de 18 - [← Volver a Lecciones](../)*



