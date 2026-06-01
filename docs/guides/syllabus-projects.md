

<!-- START OF FILE: AGENTS.md -->
# Documento: AGENTS
---
# AGENTS.md

Proyecto educativo DSY1103 - Fullstack I

## Proyectos (snapshots por lección)

| Proyecto | Lección | Descripción |
|----------|--------|------------|
| `Tickets/` | base | In-memory (HashMap), sin BD |
| `Tickets-10/` | 10 | + JPA + H2 |
| `Tickets-11/` | 11 | + MySQL + PostgreSQL |
| `Tickets-12/` | 12 | igual a 11 |

Todos usan: Spring Boot 4.0.5 + Java 21

## Microservicios de apoyo

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| `NotificationService/` | 8081 | Envío de notificaciones (in-memory) |
| `AuditService/` | 8082 | Registro de auditoría de tickets (in-memory) |
| `SearchService/` | 8084 | Indexación y búsqueda full-text de tickets (in-memory) |
| `SLAService/` | 8085 | Control de tiempos de resolución / SLA (in-memory) |

Todos los microservicios usan Spring Boot 4.0.5 + Java 21, sin base de datos.

## Comandos (Windows)

```bash
# Ejecutar desde el directorio del proyecto (ej: Tickets, Tickets-10, etc)
cd Tickets-10
mvnw.cmd spring-boot:run
mvnw.cmd test
mvnw.cmd test -Dtest=ClaseTest
mvnw.cmd package -DskipTests
```

## Endpoints

- Base URL: `http://localhost:8080/ticket-app`
- Rutas: `/tickets`, `/tickets/by-id/{id}`

## Datos

- `Tickets/`: in-memory (se reinicia cada ejecución)
- `Tickets-10+`: requiere H2/MySQL/PostgreSQL configurado

## Warnings

- Paquete `respository/` (sin 'o') es intencional — respetar al crear archivos
- Context path `/ticket-app` — no usar `/tickets` directamente

## Arquitectura

5 capas: controller → service → respository → model/dto



<!-- START OF FILE: CLAUDE.md -->
# Documento: CLAUDE
---
# CLAUDE.md

> Ver AGENTS.md para instrucciones rápidas de desarrollo

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

- `Tickets/` — Spring Boot 4 project (the active backend application)
- `docs/` — Course documentation and supplementary study material (read-only reference)
- `Homologacion/` — Separate workspace (independent, not part of the main app)

All development work happens inside `Tickets/`. Run every command from that directory.

## Commands

```bash
cd Tickets

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=TicketsApplicationTests

# Build without running tests
./mvnw package -DskipTests

# Clean build
./mvnw clean package
```

On Windows use `mvnw.cmd` instead of `./mvnw` if the shell does not support Unix-style scripts.

## Stack

- Java 21 / Spring Boot 4.0.5
- Spring Web MVC (no Spring Data JPA — storage is in-memory)
- Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` on models)
- Jakarta Validation (`@NotBlank`, `@Size`) on DTOs with `@Valid` in controllers
- Spring Boot DevTools (hot reload during development)

## Application configuration

`src/main/resources/application.yml`:
- Port: `8080`
- Context path: `/ticket-app`
- Base URL: `http://localhost:8080/ticket-app`

## Architecture

The project follows a strict 5-layer separation (Controller → Service → Repository → Model / DTO):

| Layer | Package | Responsibility |
|---|---|---|
| `controller/` | `TicketController` | HTTP mapping, `ResponseEntity` responses, `@Valid` on DTOs |
| `service/` | `TicketService` | Business rules (duplicate check, auto-set `status`/`createdAt`/`estimatedResolutionDate`); maps DTO → model |
| `respository/` | `TicketRepository` | In-memory `HashMap`-based store; auto-increments `id` |
| `model/` | `Ticket`, `ErrorResponse` | `Ticket` is a Lombok POJO; `ErrorResponse` is a Java `record` |
| `dto/` | `TicketRequest` | Java `record` with Jakarta Validation annotations (`@NotBlank`, `@Size`) |

**Note:** The package name `respository` (missing the first `o`) is intentional — match it exactly when adding new files.

**Data persistence:** There is no database. `TicketRepository` holds data in a `Map<Long, Ticket>` in memory, pre-seeded with two tickets. All data resets on restart.

**Ticket lifecycle set by `TicketService.create()`:**
- `status` → `"NEW"`
- `createdAt` → `LocalDateTime.now()`
- `estimatedResolutionDate` → `LocalDate.now().plusDays(5)`

## Endpoints

All routes are relative to `/ticket-app/tickets`:

| Method | Path | Description |
|---|---|---|
| `GET` | `/tickets` | List all tickets |
| `POST` | `/tickets` | Create ticket (body validated; rejects duplicate titles) |
| `GET` | `/tickets/by-id/{id}` | Get ticket by id |
| `PUT` | `/tickets/by-id/{id}` | Update ticket by id |
| `DELETE` | `/tickets/by-id/{id}` | Delete ticket by id |

Service methods return `Optional<Ticket>` for single-entity lookups; controllers use `.map()` / `.orElse()` to convert to `ResponseEntity`. DELETE returns `boolean` → `204 No Content`.

## Error handling

- Business validation exceptions (`IllegalArgumentException`) are caught in the controller and returned as `409 Conflict` with an `ErrorResponse` body.
- Bean Validation errors (`@Valid`) are handled by an `@ExceptionHandler(MethodArgumentNotValidException.class)` in the controller, returned as `400 Bad Request` with an `ErrorResponse` body.
- `ErrorResponse` is a Java `record`: `public record ErrorResponse(String message) {}`.

## Conventions

- Endpoint paths use kebab-case (e.g., `/by-id/{id}`).
- Responses for create operations return a plain `String` body (`"Ticket Creado"`), not the created object.
- New domain models should use the same Lombok pattern as `Ticket`.
- DTOs, value objects, and error responses use Java `record` types.
- Validation annotations (`@NotBlank`, `@Size`) go on DTOs, not on domain models.



<!-- START OF FILE: README.md -->
# Documento: README
---
# DSY1103 - Fullstack I Backend

Repositorio del curso **Fullstack I** orientado al desarrollo backend con **Spring Boot 4** y **Java 21**.

## Objetivo

Construir APIs REST con buenas prácticas desde el inicio:

- separación por capas (CSR): `Controller`, `Service`, `Repository`, `Model`
- uso correcto de métodos HTTP y códigos de respuesta
- diseño de endpoints versionados (por ejemplo `/api/v1/...`)
- configuración base del proyecto (`application.properties`, `banner.txt`, puerto y `context-path`)

## Stack técnico (verificado en el proyecto)

- Java 21
- Spring Boot `4.0.3`
- Maven Wrapper (`mvnw`)
- Spring Web MVC
- Pruebas con Spring Boot Test

## Estructura del repositorio

- [`Tickets/`](./Tickets/README.md): proyecto Spring Boot principal del curso
- [`docs/`](./docs/README.md): documentación del curso (lecciones y material de apoyo)

## Requisitos

- JDK 21 instalado
- Terminal con permisos de ejecución sobre `mvnw`

## Ejecutar el proyecto

Desde la carpeta `Tickets/`:

```bash
cd Tickets
./mvnw spring-boot:run
```

## Ejecutar pruebas

Desde la carpeta `Tickets/`:

```bash
cd Tickets
./mvnw test
```

## Material de apoyo de la unidad

Toda la documentación del curso está centralizada en [`docs/`](./docs/README.md), incluyendo lecciones y material complementario.

> 🗺️ ¿No sabes qué estudiar primero? Revisa el **[Roadmap de estudio](./docs/roadmap.md)** — organiza todos los extras según el tiempo disponible.

## Estado del repositorio

Este repositorio se usa con foco académico para practicar y evaluar avances por clase.








<!-- START OF FILE: AuditService_README.md -->
# Documento: AuditService README
---
# AuditService

Microservicio de **registro de auditoría**. Guarda un historial de eventos ocurridos sobre entidades del sistema (creación, cambios de estado, asignaciones), permitiendo trazabilidad de todas las acciones.

Forma parte del ecosistema educativo DSY1103. La aplicación Tickets lo consume via FeignClient (lección 14).

---

## Puerto

`8082`

---

## Cómo ejecutar

```bash
cd AuditService
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # macOS / Linux
```

---

## API

### `POST /api/audit`

Registra un evento de auditoría.

**Body:**
```json
{
  "action": "STATUS_CHANGE",
  "entityType": "Ticket",
  "entityId": "1",
  "userId": "10",
  "username": "juan@example.com",
  "details": "Estado cambió de NEW a IN_PROGRESS"
}
```

Campos opcionales: `entityType` (default `"Ticket"`), `userId` (default `0`), `username` (default `"system"`), `details` (default `""`).

**Response:**
```json
{
  "id": 1,
  "action": "STATUS_CHANGE",
  "entityType": "Ticket",
  "entityId": 1,
  "userId": 10,
  "username": "juan@example.com",
  "details": "Estado cambió de NEW a IN_PROGRESS",
  "timestamp": 1714000000000
}
```

**Ejemplo:**
```bash
curl -X POST http://localhost:8082/api/audit \
  -H "Content-Type: application/json" \
  -d '{"action":"TICKET_CREATED","entityType":"Ticket","entityId":"1","username":"system"}'
```

---

### `GET /api/audit/ticket/{ticketId}`

Obtiene todos los eventos de auditoría de un ticket específico.

```bash
curl http://localhost:8082/api/audit/ticket/1
```

---

### `GET /api/audit`

Lista todos los eventos de auditoría registrados.

```bash
curl http://localhost:8082/api/audit
```

---

## Notas

- **Almacenamiento en memoria**: los registros se pierden al reiniciar el servicio.
- **`entityId` y `userId` como String en el body**: el servicio los parsea a `Long` internamente.
- **Acciones comunes**: `TICKET_CREATED`, `STATUS_CHANGE`, `TICKET_ASSIGNED`, `TICKET_CLOSED`.



<!-- START OF FILE: NotificationService_README.md -->
# Documento: NotificationService README
---
# NotificationService

Microservicio de **envío de notificaciones**. Recibe solicitudes de notificación desde otros servicios y las almacena, simulando un sistema de mensajería (email, push, etc.).

Forma parte del ecosistema educativo DSY1103. La aplicación Tickets lo consume via RestClient (lección 14).

---

## Puerto

`8081`

---

## Cómo ejecutar

```bash
cd NotificationService
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # macOS / Linux
```

---

## API

### `POST /api/notifications`

Crea una nueva notificación.

**Body:**
```json
{
  "title": "Ticket asignado",
  "message": "Se te ha asignado el ticket 'Bug en login'",
  "type": "INFO",
  "recipient": "juan@example.com"
}
```

Campos opcionales: `type` (default `"INFO"`), `recipient` (default `"all"`).

**Response:**
```json
{
  "id": 1,
  "title": "Ticket asignado",
  "message": "Se te ha asignado el ticket 'Bug en login'",
  "type": "INFO",
  "recipient": "juan@example.com",
  "sent": false,
  "timestamp": 1714000000000
}
```

**Ejemplo:**
```bash
curl -X POST http://localhost:8081/api/notifications \
  -H "Content-Type: application/json" \
  -d '{"title":"Ticket asignado","message":"Se te asignó Bug en login","type":"INFO","recipient":"juan@example.com"}'
```

---

### `GET /api/notifications`

Lista todas las notificaciones registradas.

```bash
curl http://localhost:8081/api/notifications
```

---

### `GET /api/notifications/{id}`

Obtiene una notificación por su ID.

```bash
curl http://localhost:8081/api/notifications/1
```

---

## Notas

- **Almacenamiento en memoria**: las notificaciones se pierden al reiniciar el servicio.
- **`sent: false`**: el campo simula que la notificación fue recibida pero no despachada; en un sistema real este servicio dispararía el envío.



<!-- START OF FILE: SearchService_README.md -->
# Documento: SearchService README
---
# SearchService

Microservicio de **indexación y búsqueda full-text de tickets**. Mantiene un índice en memoria con el contenido de cada ticket, permitiendo búsquedas por texto libre sobre título y descripción.

Forma parte del ecosistema educativo DSY1103. Los alumnos deben implementar el cliente en su aplicación Tickets (lección 14).

---

## Puerto

`8084`

---

## Cómo ejecutar

```bash
cd SearchService
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # macOS / Linux
```

---

## API

### `POST /api/search/index`

Indexa o reindexar un ticket. Si el ticket ya tiene una entrada en el índice, la reemplaza.

**Body:**
```json
{
  "ticketId": "1",
  "title": "Bug en login",
  "description": "El formulario no valida el campo email",
  "status": "IN_PROGRESS"
}
```

**Response:** `204 No Content`

**Ejemplo:**
```bash
curl -X POST http://localhost:8084/api/search/index \
  -H "Content-Type: application/json" \
  -d '{"ticketId":"1","title":"Bug en login","description":"Falla validación email","status":"NEW"}'
```

---

### `GET /api/search?q={texto}`

Busca tickets cuyo título o descripción contenga el texto indicado. Sin parámetro retorna todos los registros indexados.

**Response:**
```json
[
  {
    "id": 1,
    "ticketId": 1,
    "title": "Bug en login",
    "description": "Falla validación email",
    "status": "NEW",
    "indexedAt": 1714000000000
  }
]
```

**Ejemplos:**
```bash
# Buscar por texto
curl "http://localhost:8084/api/search?q=login"

# Listar todo el índice
curl http://localhost:8084/api/search
```

---

### `GET /api/search/ticket/{ticketId}`

Obtiene la entrada de índice de un ticket específico.

**Response:** `200 OK` con el objeto indexado, o `404 Not Found` si no está indexado.

```bash
curl http://localhost:8084/api/search/ticket/1
```

---

## Notas

- **Almacenamiento en memoria**: el índice se pierde al reiniciar el servicio.
- **Reindexación**: enviar `POST /api/search/index` con el mismo `ticketId` actualiza la entrada existente.
- **Búsqueda**: es case-insensitive y busca por subcadena en título y descripción.



<!-- START OF FILE: SLAService_README.md -->
# Documento: SLAService README
---
# SLAService

Microservicio de **control de tiempos de resolución (Service Level Agreement)**. Registra cuándo se abrió un ticket y calcula automáticamente el plazo de resolución según su prioridad. Permite consultar el estado del SLA y cerrarlo cuando el ticket se resuelve.

Forma parte del ecosistema educativo DSY1103. Los alumnos deben implementar el cliente en su aplicación Tickets (lección 14).

---

## Puerto

`8085`

---

## Cómo ejecutar

```bash
cd SLAService
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # macOS / Linux
```

---

## Plazos por prioridad

| Prioridad | Plazo |
|-----------|-------|
| `HIGH`    | 24 horas |
| `MEDIUM`  | 72 horas (3 días) |
| `LOW`     | 168 horas (7 días) |

---

## API

### `POST /api/sla/start`

Inicia el SLA de un ticket. Calcula el `deadline` a partir de la prioridad. Si el ticket ya tiene un SLA abierto, retorna el existente sin crear uno nuevo.

**Body:**
```json
{
  "ticketId": "1",
  "priority": "HIGH"
}
```

**Response:**
```json
{
  "id": 1,
  "ticketId": 1,
  "priority": "HIGH",
  "deadline": "2025-01-02T10:00:00Z",
  "status": "OPEN",
  "startedAt": "2025-01-01T10:00:00Z"
}
```

**Ejemplo:**
```bash
curl -X POST http://localhost:8085/api/sla/start \
  -H "Content-Type: application/json" \
  -d '{"ticketId":"1","priority":"HIGH"}'
```

---

### `GET /api/sla/{ticketId}`

Obtiene el estado actual del SLA de un ticket.

**Response:** `200 OK` con el registro SLA, o `404 Not Found` si no existe.

```bash
curl http://localhost:8085/api/sla/1
```

---

### `PUT /api/sla/{ticketId}/close`

Cierra el SLA de un ticket. Debe llamarse cuando el ticket se resuelve o cierra.

**Response:** `200 OK` con el registro actualizado (`status: "CLOSED"`, `closedAt` añadido).

```bash
curl -X PUT http://localhost:8085/api/sla/1/close
```

---

### `GET /api/sla`

Lista todos los registros SLA (abiertos y cerrados).

```bash
curl http://localhost:8085/api/sla
```

---

## Notas

- **Almacenamiento en memoria**: los registros se pierden al reiniciar el servicio.
- **Un SLA por ticket**: si ya existe un SLA abierto para un ticket, `POST /api/sla/start` retorna el existente.
- **Prioridad por defecto**: si no se envía `priority`, se asume `MEDIUM` (72 horas).



<!-- START OF FILE: Tickets_README.md -->
# Documento: Tickets README
---
# Tickets API

Subproyecto backend del repositorio del curso **DSY1103 - Fullstack I**.

API REST construida con **Spring Boot 4** y **Java 21** para la gestión de tickets de soporte.

---

## 🛠️ Tecnologías

| Herramienta          | Versión  |
|----------------------|----------|
| Java                 | 21       |
| Spring Boot          | 4.0.3    |
| Spring Web MVC       | (incluido en Boot) |
| Lombok               | (incluido en Boot) |
| Spring Boot DevTools | (incluido en Boot) |
| Maven Wrapper        | (incluido) |

---

## 📁 Estructura del proyecto

```
src/
└── main/
    ├── java/cl/duoc/fullstack/tickets/
    │   ├── TicketsApplication.java      # Punto de entrada
    │   ├── controller/
    │   │   └── TicketController.java    # Controlador REST
    │   ├── model/
    │   │   └── Ticket.java              # Modelo de dominio
    │   ├── respository/
    │   │   └── TicketRepository.java    # Repositorio en memoria
    │   └── service/
    │       └── TicketService.java       # Lógica de negocio
    └── resources/
        └── application.properties
```

---

## 📦 Modelo

### `Ticket`

| Campo         | Tipo     | Descripción               |
|---------------|----------|---------------------------|
| `id`          | `Long`   | Identificador del ticket  |
| `title`       | `String` | Título del ticket         |
| `description` | `String` | Descripción del ticket    |
| `status`      | `String` | Estado del ticket (`NEW`, etc.) |

---

## 🔌 Endpoints

Base URL: `http://localhost:8080`

| Método | Ruta       | Descripción                     |
|--------|------------|---------------------------------|
| `GET`  | `/tickets` | Retorna la lista de todos los tickets |

### Ejemplo de respuesta `GET /tickets`

```json
[
  {
    "id": 1,
    "title": "Ticket 1",
    "description": "Ticket 1",
    "status": "NEW"
  },
  {
    "id": 2,
    "title": "Ticket 2",
    "description": "Ticket 2",
    "status": "NEW"
  }
]
```

---

## 🚀 Ejecutar

```bash
./mvnw spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

---

## 🧪 Probar

```bash
./mvnw test
```

---

## 📚 Referencia

- README principal: [`../README.md`](../README.md)
- Material de clases: [`../docs/lessons/04-responsabilities/`](../docs/lessons/04-responsabilities/)



<!-- START OF FILE: Tickets-10_README.md -->
# Documento: Tickets-10 README
---
# Tickets-10 — Lección 10: Introducción a JPA

Subproyecto del curso **DSY1103 - Fullstack I**.

Migra el almacenamiento de HashMap en memoria a **Spring Data JPA** con base de datos H2 embebida.

---

## 🔄 Cambios desde Lección 09 (base Tickets)

### Dependencias nuevas
| Dependencia | Para qué sirve |
|---|---|
| `spring-boot-starter-data-jpa` | Habilita JPA/Hibernate |
| `h2` | Base de datos en memoria para desarrollo |
| `spring-boot-h2console` | Consola web para inspeccionar H2 |

### Modelo — `Ticket.java`
- Convertida a entidad JPA: `@Entity`, `@Table(name = "tickets")`
- PK con auto-increment: `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- Campos nuevos: `createdAt`, `estimatedResolutionDate`, `effectiveResolutionDate`
- Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`

### Repositorio — `TicketRepository.java`
- Reemplaza el HashMap por `JpaRepository<Ticket, Long>`
- Derivación de queries: `existsByTitleIgnoreCase`, `findByStatusIgnoreCase`, `findAllByOrderByCreatedAtAsc`

### Servicio — `TicketService.java`
- CRUD completo delegando a JPA
- Validación de título único → lanza `IllegalArgumentException` (409 Conflict)

### Patrón de DTOs (Java records)
- `TicketRequest` → entrada HTTP con validaciones (`@NotBlank`, `@Size`)
- `TicketCommand` → objeto interno que lleva los datos hasta el Service
- `TicketResult` → objeto de retorno del Service
- `TicketResponse` → salida HTTP (mapeada desde `TicketResult` en el Controller)

### Controlador — `TicketController.java`
- CRUD completo: GET, POST, GET/by-id, PUT/by-id, DELETE/by-id
- Filtro por estado: `GET /tickets?status=NEW`
- Manejo de errores de validación (`@ExceptionHandler`)
- Error de negocio → `409 Conflict`

### Configuración
- `application.yml`: context-path `/ticket-app`, puerto 8080
- H2 en memoria: `jdbc:h2:mem:tickets_db`, `ddl-auto: create-drop`
- Consola H2 disponible en `/ticket-app/h2-console`

### Inicialización de datos — `DataInitializer.java`
- Crea tickets de ejemplo al arrancar si la tabla está vacía

---

## 🛠️ Tecnologías

| Herramienta | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Web MVC | (incluido) |
| Spring Data JPA / Hibernate | (incluido) |
| H2 Database | (incluido) |
| Lombok | (incluido) |
| Jakarta Validation | (incluido) |
| Maven Wrapper | (incluido) |

---

## 📁 Estructura del proyecto

```
src/main/java/cl/duoc/fullstack/tickets/
├── TicketsApplication.java
├── config/
│   └── DataInitializer.java        # Datos iniciales de ejemplo
├── controller/
│   └── TicketController.java       # Endpoints REST
├── dto/
│   ├── TicketRequest.java          # Entrada HTTP (con validaciones)
│   ├── TicketCommand.java          # Objeto interno Controller → Service
│   ├── TicketResult.java           # Objeto interno Service → Controller
│   └── TicketResponse.java         # Salida HTTP
├── model/
│   ├── Ticket.java                 # Entidad JPA
│   └── ErrorResponse.java          # Record para respuestas de error
├── respository/
│   └── TicketRepository.java       # JpaRepository<Ticket, Long>
└── service/
    └── TicketService.java          # Lógica de negocio
```

---

## 📦 Modelo de datos

### `Ticket`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK auto-incremental |
| `title` | `String` | Título (único, requerido) |
| `description` | `String` | Descripción (requerida) |
| `status` | `String` | `NEW`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `createdAt` | `LocalDateTime` | Fecha/hora de creación (auto) |
| `estimatedResolutionDate` | `LocalDate` | Fecha estimada (createdAt + 5 días) |
| `effectiveResolutionDate` | `LocalDateTime` | Fecha real de resolución |

---

## 🔌 Endpoints

Base URL: `http://localhost:8080/ticket-app`

| Método | Ruta | Body | Descripción | Respuesta OK |
|---|---|---|---|---|
| `GET` | `/tickets` | — | Listar todos (opcional `?status=`) | `200` lista |
| `POST` | `/tickets` | `TicketRequest` | Crear ticket | `201` ticket creado |
| `GET` | `/tickets/by-id/{id}` | — | Obtener por ID | `200` / `404` |
| `PUT` | `/tickets/by-id/{id}` | `TicketRequest` | Actualizar ticket | `200` / `404` |
| `DELETE` | `/tickets/by-id/{id}` | — | Eliminar ticket | `204` / `404` |

### Errores posibles

| Código | Causa |
|---|---|
| `400 Bad Request` | Validación fallida (`@NotBlank`, `@Size`) |
| `404 Not Found` | ID no existe |
| `409 Conflict` | Título duplicado |

---

## 🚀 Ejecutar

```bash
cd Tickets-10

# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

La aplicación arranca con H2 en memoria. Sin configuración adicional.

Consola H2: `http://localhost:8080/ticket-app/h2-console`
JDBC URL: `jdbc:h2:mem:tickets_db`

---

## 🧪 Tests

```bash
.\mvnw.cmd test
```

---

**Base**: Lección 09 — Repositorio Customizado  
**Stack**: Spring Boot 4.0.5 · Java 21 · JPA/Hibernate · H2  
**Estado**: ✅ Completada


<!-- START OF FILE: Tickets-11_README.md -->
# Documento: Tickets-11 README
---
# Tickets-11 — Lección 11: Configuración Multi-Base de Datos

Subproyecto del curso **DSY1103 - Fullstack I**.

Extiende Lección 10 incorporando **perfiles de Spring Boot** para conectar a H2, MySQL o PostgreSQL/Supabase sin cambiar código Java.

---

## 🔄 Cambios desde Lección 10

### Dependencias nuevas
| Dependencia | Para qué sirve |
|---|---|
| `mysql-connector-j` | Driver JDBC para MySQL (XAMPP) |
| `postgresql` | Driver JDBC para PostgreSQL (Supabase) |
| `spring-dotenv` | Carga automática de archivos `.env` |

> `spring-boot-h2console` ya estaba en L10.

### Perfiles de Spring Boot

Un perfil = un archivo `application-{perfil}.yml` con la configuración de esa base de datos.

| Perfil | Archivo | Base de datos |
|---|---|---|
| `h2` | `application-h2.yml` | H2 en memoria (desarrollo rápido) |
| `mysql` | `application-mysql.yml` | MySQL local (XAMPP) |
| `supabase` | `application-supabase.yml` | PostgreSQL en la nube |

### Archivos `.env` por entorno

| Archivo | Perfil activo | Descripción |
|---|---|---|
| `.env.local` | `h2` | Desarrollo local sin BD externa |
| `.env.dev` | `mysql` | Desarrollo con MySQL/XAMPP |
| `.env.test` | `supabase` | Pruebas contra Supabase |
| `.env.prod` | `supabase` | Producción (mismas variables, distintos valores) |
| `.env.example` | — | Plantilla — copiar y rellenar |

> ⚠️ **Nunca** hacer commit de un `.env` con credenciales reales. Solo `.env.example` va al repositorio.

### Sin cambios de código Java
El modelo, repositorio, servicio, controlador y DTOs son idénticos a Lección 10. Solo cambia la configuración.

---

## 🌍 Cómo activar un perfil

### Opción 1 — Archivo `.env` (recomendada)

```bash
# Copiar el entorno que necesitas
copy .env.local .env      # → perfil h2
copy .env.dev .env        # → perfil mysql
copy .env.test .env       # → perfil supabase

# Ejecutar
.\mvnw.cmd spring-boot:run
```

### Opción 2 — Variable de entorno

```powershell
# PowerShell
$env:SPRING_PROFILES_ACTIVE="mysql"
.\mvnw.cmd spring-boot:run
```

### Opción 3 — Argumento de Maven

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=supabase
```

---

## 🛠️ Tecnologías

| Herramienta | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Web MVC | (incluido) |
| Spring Data JPA / Hibernate | (incluido) |
| H2 Database | (incluido) |
| MySQL Connector/J | (incluido) |
| PostgreSQL JDBC | (incluido) |
| spring-dotenv | 4.0.0 |
| Lombok | (incluido) |
| Jakarta Validation | (incluido) |
| Maven Wrapper | (incluido) |

---

## 📁 Estructura del proyecto

```
src/main/java/cl/duoc/fullstack/tickets/
├── TicketsApplication.java
├── config/
│   └── DataInitializer.java        # Datos iniciales de ejemplo
├── controller/
│   └── TicketController.java       # Endpoints REST
├── dto/
│   ├── TicketRequest.java          # Entrada HTTP (con validaciones)
│   ├── TicketCommand.java          # Objeto interno Controller → Service
│   ├── TicketResult.java           # Objeto interno Service → Controller
│   └── TicketResponse.java         # Salida HTTP
├── model/
│   ├── Ticket.java                 # Entidad JPA
│   └── ErrorResponse.java          # Record para respuestas de error
├── respository/
│   └── TicketRepository.java       # JpaRepository<Ticket, Long>
└── service/
    └── TicketService.java          # Lógica de negocio

src/main/resources/
├── application.yml                 # Config base (sin credenciales)
├── application-h2.yml              # Perfil H2
├── application-mysql.yml           # Perfil MySQL
└── application-supabase.yml        # Perfil Supabase/PostgreSQL
```

---

## 📦 Modelo de datos

### `Ticket`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK auto-incremental |
| `title` | `String` | Título (único, requerido) |
| `description` | `String` | Descripción (requerida) |
| `status` | `String` | `NEW`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `createdAt` | `LocalDateTime` | Fecha/hora de creación (auto) |
| `estimatedResolutionDate` | `LocalDate` | Fecha estimada (createdAt + 5 días) |
| `effectiveResolutionDate` | `LocalDateTime` | Fecha real de resolución |

---

## 🔌 Endpoints

Base URL: `http://localhost:8080/ticket-app`

| Método | Ruta | Body | Descripción | Respuesta OK |
|---|---|---|---|---|
| `GET` | `/tickets` | — | Listar todos (opcional `?status=`) | `200` lista |
| `POST` | `/tickets` | `TicketRequest` | Crear ticket | `201` ticket creado |
| `GET` | `/tickets/by-id/{id}` | — | Obtener por ID | `200` / `404` |
| `PUT` | `/tickets/by-id/{id}` | `TicketRequest` | Actualizar ticket | `200` / `404` |
| `DELETE` | `/tickets/by-id/{id}` | — | Eliminar ticket | `204` / `404` |

### Errores posibles

| Código | Causa |
|---|---|
| `400 Bad Request` | Validación fallida (`@NotBlank`, `@Size`) |
| `404 Not Found` | ID no existe |
| `409 Conflict` | Título duplicado |

---

## 🚀 Ejecutar

```bash
cd Tickets-11

# 1. Seleccionar entorno
copy .env.local .env    # H2 — sin configuración adicional

# 2. Iniciar
.\mvnw.cmd spring-boot:run
```

---

## 🧪 Tests

```bash
.\mvnw.cmd test
```

---

**Base**: Lección 10 — JPA + H2  
**Stack**: Spring Boot 4.0.5 · Java 21 · JPA/Hibernate · H2 · MySQL · PostgreSQL  
**Estado**: ✅ Completada


<!-- START OF FILE: Tickets-12_README.md -->
# Documento: Tickets-12 README
---
# Tickets-12 — Lección 12: Relaciones entre Entidades JPA

Subproyecto del curso **DSY1103 - Fullstack I**.

Extiende Lección 11 incorporando **relaciones JPA** (`@ManyToOne` / `@OneToMany`) entre `Ticket` y `User`, gestión de usuarios y asignación de tickets por email.

---

## 🔄 Cambios desde Lección 11

### Entidad nueva — `User`
- `@Entity`, `@Table(name = "users")`
- Campos: `id` (PK), `name`, `email` (único)
- `@OneToMany(mappedBy = "createdBy")` y `@OneToMany(mappedBy = "assignedTo")` para navegación inversa

### Relaciones en `Ticket`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "created_by_id")
private User createdBy;         // FK → users.id

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assigned_to_id")
private User assignedTo;        // FK → users.id (nullable)
```

Sin `@JsonIgnoreProperties` — los entities nunca salen del Service (se convierten a DTOs).

### DTOs actualizados
| DTO | Qué se agregó |
|---|---|
| `TicketRequest` | Campo `createdByEmail` (`@NotBlank` + `@Email`) |
| `TicketCommand` | Campo `createdByEmail` |
| `TicketResult` | Campos `UserResult createdBy` y `UserResult assignedTo` |
| `TicketResponse` | Campos `UserResult createdBy` y `UserResult assignedTo` |

### DTOs nuevos
| DTO | Descripción |
|---|---|
| `UserRequest` | Entrada HTTP para crear usuario (`name`, `email`) |
| `UserResult` | Record interno con `id`, `name`, `email` |
| `AssignTicketRequest` | Body del `PATCH` — solo `@Email` (null/vacío = desasignar) |

### Excepción nueva — `BadRequestException`
Distingue dos tipos de error del cliente:

| Excepción | HTTP | Cuándo |
|---|---|---|
| `IllegalArgumentException` | `409 Conflict` | Regla de negocio (título duplicado) |
| `BadRequestException` | `400 Bad Request` | Dato inválido (email no registrado) |

### `TicketService` actualizado
- Depende de `TicketRepository` + `UserRepository`
- `create()`: busca el creador por email con `orElseThrow(BadRequestException)`
- `assignTicket()`: asigna o desasigna usuario via email; null/vacío = desasignar
- `toResult()`: mapea `Ticket` → `TicketResult` con `UserResult` anidados

### Nuevos: `UserRepository`, `UserService`, `UserController`
- CRUD de usuarios en `/users`
- Validación de email único → `409 Conflict`

### `TicketController` actualizado
- `POST /tickets`: captura también `BadRequestException` → `400`
- Nuevo `PATCH /tickets/by-id/{id}`: asignar/desasignar usuario

### `DataInitializer` actualizado
- Crea 2 usuarios de ejemplo (Ana García, Carlos López)
- Los tickets iniciales referencian esos usuarios (`setCreatedBy`)

---

## 🛠️ Tecnologías

| Herramienta | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Web MVC | (incluido) |
| Spring Data JPA / Hibernate | (incluido) |
| H2 Database | (incluido) |
| MySQL Connector/J | (incluido) |
| PostgreSQL JDBC | (incluido) |
| spring-dotenv | 4.0.0 |
| Lombok | (incluido) |
| Jakarta Validation | (incluido) |
| Maven Wrapper | (incluido) |

---

## 📁 Estructura del proyecto

```
src/main/java/cl/duoc/fullstack/tickets/
├── TicketsApplication.java
├── config/
│   └── DataInitializer.java        # Crea usuarios + tickets de ejemplo
├── controller/
│   ├── TicketController.java       # Endpoints de tickets (+ PATCH asignación)
│   └── UserController.java         # Endpoints de usuarios
├── dto/
│   ├── TicketRequest.java          # Entrada HTTP ticket (+ createdByEmail)
│   ├── TicketCommand.java          # Objeto interno Controller → Service
│   ├── TicketResult.java           # Objeto interno Service → Controller (+ UserResult)
│   ├── TicketResponse.java         # Salida HTTP ticket (+ UserResult)
│   ├── UserRequest.java            # Entrada HTTP usuario
│   ├── UserResult.java             # Record interno (id, name, email)
│   └── AssignTicketRequest.java    # Body del PATCH (assignedToEmail)
├── exception/
│   └── BadRequestException.java    # RuntimeException → 400 Bad Request
├── model/
│   ├── Ticket.java                 # Entidad JPA (+ @ManyToOne createdBy, assignedTo)
│   ├── User.java                   # Entidad JPA nueva
│   └── ErrorResponse.java          # Record para respuestas de error
├── respository/
│   ├── TicketRepository.java       # JpaRepository<Ticket, Long>
│   └── UserRepository.java         # JpaRepository<User, Long> + findByEmail
└── service/
    ├── TicketService.java          # Lógica de negocio (+ UserRepository)
    └── UserService.java            # Lógica de usuarios

src/main/resources/
├── application.yml
├── application-h2.yml
├── application-mysql.yml
└── application-supabase.yml
```

---

## 📦 Modelo de datos

### `Ticket`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK auto-incremental |
| `title` | `String` | Título (único, requerido) |
| `description` | `String` | Descripción (requerida) |
| `status` | `String` | `NEW`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `createdAt` | `LocalDateTime` | Fecha/hora de creación (auto) |
| `estimatedResolutionDate` | `LocalDate` | Fecha estimada (createdAt + 5 días) |
| `effectiveResolutionDate` | `LocalDateTime` | Fecha real de resolución |
| `createdBy` | `User` | FK `created_by_id` — requerido |
| `assignedTo` | `User` | FK `assigned_to_id` — nullable |

### `User`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK auto-incremental |
| `name` | `String` | Nombre (requerido) |
| `email` | `String` | Email (único, requerido) |

---

## 🔌 Endpoints

Base URL: `http://localhost:8080/ticket-app`

### Tickets

| Método | Ruta | Body | Descripción | Respuesta OK |
|---|---|---|---|---|
| `GET` | `/tickets` | — | Listar todos (opcional `?status=`) | `200` lista |
| `POST` | `/tickets` | `TicketRequest` | Crear ticket (requiere `createdByEmail`) | `201` |
| `GET` | `/tickets/by-id/{id}` | — | Obtener por ID | `200` / `404` |
| `PUT` | `/tickets/by-id/{id}` | `TicketRequest` | Actualizar ticket | `200` / `404` |
| `PATCH` | `/tickets/by-id/{id}` | `AssignTicketRequest` | Asignar/desasignar usuario | `200` / `404` |
| `DELETE` | `/tickets/by-id/{id}` | — | Eliminar ticket | `204` / `404` |

### Usuarios

| Método | Ruta | Body | Descripción | Respuesta OK |
|---|---|---|---|---|
| `GET` | `/users` | — | Listar todos los usuarios | `200` lista |
| `POST` | `/users` | `UserRequest` | Crear usuario | `201` |
| `GET` | `/users/{id}` | — | Obtener usuario por ID | `200` / `404` |

### Errores posibles

| Código | Causa |
|---|---|
| `400 Bad Request` | Validación fallida o email de creador/asignado no existe |
| `404 Not Found` | ID no existe |
| `409 Conflict` | Título de ticket duplicado o email de usuario duplicado |

### Ejemplo: crear ticket

```http
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{
  "title": "Teclado no funciona",
  "description": "Las teclas F1-F4 no responden",
  "createdByEmail": "ana.garcia@empresa.com"
}
```

### Ejemplo: asignar ticket

```http
PATCH http://localhost:8080/ticket-app/tickets/by-id/1
Content-Type: application/json

{ "assignedToEmail": "carlos.lopez@empresa.com" }
```

### Ejemplo: desasignar ticket

```http
PATCH http://localhost:8080/ticket-app/tickets/by-id/1
Content-Type: application/json

{ "assignedToEmail": "" }
```

---

## 🌍 Cómo ejecutar

```bash
cd Tickets-12

# 1. Seleccionar entorno
copy .env.local .env    # H2 — sin configuración adicional
copy .env.dev .env      # MySQL (XAMPP)
copy .env.test .env     # Supabase

# 2. Iniciar
.\mvnw.cmd spring-boot:run
```

---

## 🧪 Tests

```bash
.\mvnw.cmd test
```

---

**Base**: Lección 11 — Multi-Base de Datos  
**Stack**: Spring Boot 4.0.5 · Java 21 · JPA/Hibernate · H2 · MySQL · PostgreSQL  
**Estado**: ✅ Completada


<!-- START OF FILE: Tickets-13_README.md -->
# Documento: Tickets-13 README
---
# Tickets-13 — Lección 13: Historial de Cambios

Subproyecto del curso **DSY1103 - Fullstack I**.

Extiende Lección 12 incorporando un **historial persistente de cambios** en el ticket: registra automáticamente cada vez que cambia el estado o el asignado, exponiendo la auditoría a través de un endpoint dedicado.

---

## 🔄 Cambios desde Lección 12

### Entidad nueva — `TicketHistory`
- `@Entity`, `@Table(name = "ticket_history")`
- Relación `@ManyToOne(fetch = FetchType.LAZY)` a `Ticket` (FK `ticket_id`, NOT NULL)
- Campos de auditoría:

| Campo | Tipo | Descripción |
|---|---|---|
| `previousStatus` | `String` | Estado antes del cambio (null si el cambio fue solo de asignado) |
| `newStatus` | `String` | Estado después del cambio (null si el cambio fue solo de asignado) |
| `previousAssignedEmail` | `String` | Email del asignado anterior (String, no FK) |
| `newAssignedEmail` | `String` | Email del nuevo asignado (String, no FK) |
| `changedAt` | `LocalDateTime` | Momento del cambio (NOT NULL) |
| `comment` | `String` | Nota opcional |

Sin `@JsonIgnore` — la entidad nunca sale del Service (se convierte a `TicketHistoryResult`).

> **¿Por qué email y no FK a User?**  
> El historial es un log inmutable que registra un snapshot del estado en el momento del cambio. Si el usuario fuera eliminado o modificado, el historial quedaría inconsistente con una FK. El email es el dato identitario autosuficiente que existía en ese momento.

### Relación nueva en `Ticket`
```java
@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = false)
private List<TicketHistory> history = new ArrayList<>();
```
`orphanRemoval = false`: el historial nunca debe borrarse, incluso si se elimina el ticket.

### DTO nuevo — `TicketHistoryResult`
Record que expone el historial al cliente. El Service construye instancias a partir de `TicketHistory`:
```java
public record TicketHistoryResult(
    Long id, String previousStatus, String newStatus,
    String previousAssignedEmail, String newAssignedEmail,
    LocalDateTime changedAt, String comment
) {}
```

### Repositorio nuevo — `TicketHistoryRepository`
```java
List<TicketHistory> findByTicketIdOrderByChangedAtDesc(Long ticketId);
```

### `TicketService` actualizado
- Inyecta `TicketHistoryRepository` (constructor injection)
- `create()` → llama `recordChange(saved, null, "NEW", null, null, "Ticket creado")` después del `save`
- `updateById()` → ahora retorna `TicketResult` (lanza `NoSuchElementException` si no existe en lugar de `Optional.empty()`); captura estado anterior antes de guardar y llama `recordChange`
- `assignTicket()` → captura email del asignado anterior, llama `recordChange` después de guardar
- `recordChange()` (privado): solo persiste un `TicketHistory` si hay cambio real de estado o de asignado
- `getHistory(Long ticketId)` → retorna `Optional<List<TicketHistoryResult>>`; vacío si el ticket no existe
- `toHistoryResult()` (privado): mapea `TicketHistory` → `TicketHistoryResult`

### `TicketController` actualizado
- `updateTicketById`: captura `NoSuchElementException` → `404` (antes manejaba `Optional.empty()`)
- Nuevo endpoint `GET /tickets/by-id/{id}/history` que llama `service.getHistory(id)`

> **¿Por qué no hay `TicketHistoryController`?**  
> `TicketHistory` es una entidad débil: no tiene sentido sin su ticket padre. El historial se accede siempre como subrecurso del ticket, por lo que el endpoint vive en `TicketController` y no hay controller dedicado.

---

## 🛠️ Tecnologías

| Herramienta | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Web MVC | (incluido) |
| Spring Data JPA / Hibernate | (incluido) |
| H2 Database | (incluido) |
| MySQL Connector/J | (incluido) |
| PostgreSQL JDBC | (incluido) |
| spring-dotenv | 4.0.0 |
| Lombok | (incluido) |
| Jakarta Validation | (incluido) |
| Maven Wrapper | (incluido) |

---

## 📁 Estructura del proyecto

```
src/main/java/cl/duoc/fullstack/tickets/
├── TicketsApplication.java
├── config/
│   └── DataInitializer.java        # Crea usuarios + tickets de ejemplo
├── controller/
│   ├── TicketController.java       # Endpoints de tickets (+ GET historial)
│   └── UserController.java         # Endpoints de usuarios
├── dto/
│   ├── TicketRequest.java          # Entrada HTTP ticket
│   ├── TicketCommand.java          # Objeto interno Controller → Service
│   ├── TicketResult.java           # Objeto interno Service → Controller
│   ├── TicketResponse.java         # Salida HTTP ticket
│   ├── TicketHistoryResult.java    # Salida HTTP historial (record)   ← NUEVO
│   ├── UserRequest.java            # Entrada HTTP usuario
│   ├── UserResult.java             # Record interno (id, name, email)
│   └── AssignTicketRequest.java    # Body del PATCH (assignedToEmail)
├── exception/
│   └── BadRequestException.java    # RuntimeException → 400 Bad Request
├── model/
│   ├── Ticket.java                 # Entidad JPA (+ @OneToMany history)  ← ACTUALIZADO
│   ├── TicketHistory.java          # Entidad JPA de auditoría            ← NUEVO
│   ├── User.java                   # Entidad JPA
│   └── ErrorResponse.java          # Record para respuestas de error
├── respository/
│   ├── TicketRepository.java       # JpaRepository<Ticket, Long>
│   ├── TicketHistoryRepository.java # JpaRepository + findByTicketId... ← NUEVO
│   └── UserRepository.java         # JpaRepository<User, Long> + findByEmail
└── service/
    ├── TicketService.java          # Lógica de negocio (+ historial)     ← ACTUALIZADO
    └── UserService.java            # Lógica de usuarios

src/main/resources/
├── application.yml
├── application-h2.yml
├── application-mysql.yml
└── application-supabase.yml
```

---

## 📦 Modelo de datos

### `Ticket`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK auto-incremental |
| `title` | `String` | Título (único, requerido) |
| `description` | `String` | Descripción (requerida) |
| `status` | `String` | `NEW`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `createdAt` | `LocalDateTime` | Fecha/hora de creación (auto) |
| `estimatedResolutionDate` | `LocalDate` | Fecha estimada (createdAt + 5 días) |
| `effectiveResolutionDate` | `LocalDateTime` | Fecha real de resolución |
| `createdBy` | `User` | FK `created_by_id` — requerido |
| `assignedTo` | `User` | FK `assigned_to_id` — nullable |
| `history` | `List<TicketHistory>` | Historial de cambios (lazy, cascade ALL) |

### `User`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK auto-incremental |
| `name` | `String` | Nombre (requerido) |
| `email` | `String` | Email (único, requerido) |

### `TicketHistory`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | PK auto-incremental |
| `ticket` | `Ticket` | FK `ticket_id` NOT NULL |
| `previousStatus` | `String` | Estado anterior (null si solo cambió asignado) |
| `newStatus` | `String` | Estado nuevo (null si solo cambió asignado) |
| `previousAssignedEmail` | `String` | Email anterior (null si solo cambió estado) |
| `newAssignedEmail` | `String` | Email nuevo (null si solo cambió estado) |
| `changedAt` | `LocalDateTime` | Timestamp del cambio (NOT NULL) |
| `comment` | `String` | Nota opcional |

---

## 🔌 Endpoints

Base URL: `http://localhost:8080/ticket-app`

### Tickets

| Método | Ruta | Body | Descripción | Respuesta OK |
|---|---|---|---|---|
| `GET` | `/tickets` | — | Listar todos (opcional `?status=`) | `200` lista |
| `POST` | `/tickets` | `TicketRequest` | Crear ticket | `201` |
| `GET` | `/tickets/by-id/{id}` | — | Obtener por ID | `200` / `404` |
| `PUT` | `/tickets/by-id/{id}` | `TicketRequest` | Actualizar ticket | `200` / `404` |
| `PATCH` | `/tickets/by-id/{id}` | `AssignTicketRequest` | Asignar/desasignar usuario | `200` / `404` |
| `DELETE` | `/tickets/by-id/{id}` | — | Eliminar ticket | `204` / `404` |
| `GET` | `/tickets/by-id/{id}/history` | — | Historial de cambios del ticket | `200` / `404` |

### Usuarios

| Método | Ruta | Body | Descripción | Respuesta OK |
|---|---|---|---|---|
| `GET` | `/users` | — | Listar todos los usuarios | `200` lista |
| `POST` | `/users` | `UserRequest` | Crear usuario | `201` |
| `GET` | `/users/{id}` | — | Obtener usuario por ID | `200` / `404` |

### Errores posibles

| Código | Causa |
|---|---|
| `400 Bad Request` | Validación fallida o email de creador/asignado no existe |
| `404 Not Found` | ID no existe |
| `409 Conflict` | Título de ticket duplicado o email de usuario duplicado |

### Ejemplo: consultar historial

```http
GET http://localhost:8080/ticket-app/tickets/by-id/1/history
```

Respuesta:
```json
[
  {
    "id": 2,
    "previousStatus": "NEW",
    "newStatus": "IN_PROGRESS",
    "previousAssignedEmail": null,
    "newAssignedEmail": null,
    "changedAt": "2026-04-15T10:35:00",
    "comment": null
  },
  {
    "id": 1,
    "previousStatus": null,
    "newStatus": "NEW",
    "previousAssignedEmail": null,
    "newAssignedEmail": null,
    "changedAt": "2026-04-15T10:30:00",
    "comment": "Ticket creado"
  }
]
```

---

## 🌍 Cómo ejecutar

```bash
cd Tickets-13

# 1. Seleccionar entorno
copy .env.local .env    # H2 — sin configuración adicional
copy .env.dev .env      # MySQL (XAMPP)
copy .env.test .env     # Supabase

# 2. Iniciar
.\mvnw.cmd spring-boot:run
```

---

## 🧪 Tests

```bash
.\mvnw.cmd test
```

---

**Base**: Lección 12 — Relaciones JPA  
**Stack**: Spring Boot 4.0.5 · Java 21 · JPA/Hibernate · H2 · MySQL · PostgreSQL  
**Estado**: ✅ Completada


<!-- START OF FILE: Tickets-14_README.md -->
# Documento: Tickets-14 README
---
# Tickets-14 — Lección 14: Comunicación entre Microservicios

Proyecto Spring Boot que extiende Tickets-13 incorporando comunicación con microservicios externos usando **RestClient** y **FeignClient**.

## Stack

- Java 21 / Spring Boot 4.0.5
- Spring Web MVC + Spring Data JPA
- Spring Cloud OpenFeign 4.0.3
- H2 (desarrollo) / MySQL / PostgreSQL (producción)
- Lombok + Jakarta Validation

## Arquitectura

```
TicketController
    └── TicketService
            ├── TicketRepository       (JPA — H2/MySQL/PostgreSQL)
            ├── TicketHistoryRepository
            ├── UserRepository
            ├── NotificationClient     (RestClient → NotificationService :8081)
            └── AuditServiceClient     (FeignClient → AuditService :8082)
```

## Microservicios integrados

| Servicio            | Puerto | Cliente        | Descripción                          |
|---------------------|--------|----------------|--------------------------------------|
| NotificationService | 8081   | RestClient     | Notifica por email al crear/asignar  |
| AuditService        | 8082   | FeignClient    | Registra cambios de estado           |

## Nuevos endpoints

| Método | Ruta                           | Descripción                          |
|--------|--------------------------------|--------------------------------------|
| GET    | `/tickets/by-id/{id}/audit`    | Historial de auditoría (AuditService)|

> Los demás endpoints son idénticos a Tickets-13.

## Comportamiento con servicios caídos

- **NotificationClient** (RestClient): patrón fire-and-forget. Si NotificationService no responde, el ticket ya fue guardado y solo se loguea el error.
- **AuditServiceClient** (FeignClient): fallback automático. Si AuditService no responde, `logEvent()` retorna `null` y `getAuditByTicket()` retorna lista vacía.

## Cómo ejecutar

```bash
# Con H2 (sin base de datos externa)
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2

# Con MySQL
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql

# Con Supabase/PostgreSQL
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=supabase

# Ejecutar tests
.\mvnw.cmd test -Dspring.profiles.active=h2
```

## Variables de entorno

Copia `.env.example` a `.env` y ajusta los valores según tu entorno. **No commitees `.env` con credenciales reales.**

### Local — perfil `h2` (sin BD externa)

```env
SPRING_PROFILES_ACTIVE=h2
```

### Dev — perfil `mysql` (XAMPP/MySQL)

```env
SPRING_PROFILES_ACTIVE=mysql
DB_HOST=localhost
DB_PORT=3306
DB_NAME=tickets_db
DB_USER=root
DB_PASSWORD=
```

### Test/Prod — perfil `supabase` (PostgreSQL)

```env
SPRING_PROFILES_ACTIVE=supabase
DB_HOST=db.xxxxxxxxxxxx.supabase.co
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=your-supabase-password
```

## URL base

`http://localhost:8080/ticket-app`

## Archivos nuevos respecto a Tickets-13

```
src/main/java/.../
  client/
    NotificationClient.java        # RestClient → NotificationService
    AuditServiceClient.java        # FeignClient interface
    AuditServiceClientFallback.java# Fallback cuando AuditService no responde
  config/
    RestClientConfig.java          # Timeouts para RestClient (5s connect, 10s read)
  dto/
    NotificationRequest.java       # Payload para NotificationService
    AuditRequest.java              # Payload para AuditService
    AuditEvent.java                # Respuesta de AuditService
```



<!-- START OF FILE: Tickets-15_README.md -->
# Documento: Tickets-15 README
---
# Tickets-15: Lección 15 - Comunicación entre Microservicios

## 📋 Descripción

Este proyecto implementa la **Lección 15: Comunicación entre Microservicios** del curso DSY1103 Fullstack I.

Implementa comunicación HTTP con servicio externo de notificaciones usando OpenFeign y RestClient.

## 🎯 Caso de Uso Extendido (Sistema de Tickets con Gestión de Usuarios)

### Roles definidos
| Rol     | Descripción              |
|---------|--------------------------|
| USER    | Crea tickets, ve estado  |
| AGENT   | Recibe tickets asignados |
| ADMIN   | Supervisa y gestiona     |

### Modelo de datos
- **User**: id, name, email, role (USER/AGENT/ADMIN), active
- **Ticket**: relaciones con User, Category, Tags
- **Category**: One-to-Many con Ticket
- **Tag**: Many-to-Many con Ticket
- **TicketHistory**: historial de cambios de estado

---

## 🔄 Cambios desde Lección 14

### 1. Dependencia Agregada
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>5.0.1</version>
</dependency>
```

### 2. Notificación Cliente (FeignClient)
```java
@FeignClient(
    name = "notificationService",
    url = "${notification.service.url:http://localhost:8081}",
    fallback = NotificationClientFallback.class
)
public interface NotificationClient {
    @PostMapping("/api/notifications/send")
    Map<String, Object> sendNotification(@RequestBody Map<String, String> notification);
}
```

### 3. Fallback para Fallos
```java
@Component
public class NotificationClientFallback implements NotificationClient {
    private static final Logger logger = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public Map<String, Object> sendNotification(Map<String, String> notification) {
        logger.warn("Notification service unavailable. Notification not sent: {}", notification.get("title"));
        return Collections.singletonMap("status", "fallback");
    }
}
```

### 4. Configuración de Timeouts
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
```

### 5. Integración en TicketService
- Notificaciones al crear ticket
- Notificaciones al actualizar ticket

### 6. Habilitar Feign
```java
@SpringBootApplication
@EnableFeignClients
public class TicketsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketsApplication.class, args);
    }
}
```

---

## 📊 Requisitos del Caso Extendido por Lección

| Lección | Requisitos del Caso Extendido |
|---------|------------------------------|
| 10 | ✅ User entity con roles, Ticket con User relaciones, seed de datos |
| 11 | ✅ Perfiles con diferentes configs de BD para usuarios (H2, MySQL, Supabase) |
| 12 | ✅ Category (One-to-Many), Tag (Many-to-Many), CRUD completo |
| 13 | ✅ TicketHistory, registro automático, endpoint de historial |
| 14 | ✅ Flyway migrations con Foreign Keys a users |
| 15 | ✅ FeignClient + RestClient, notificaciones en crear/actualizar |
| 16 | Security con 3 roles (USER/AGENT/ADMIN) |
| 17 | Logging de operaciones de usuarios |
| 18 | Excepciones para casos de usuarios |

---

## 🧪 Uso

```bash
# Desarrollo (H2)
./mvnw spring-boot:run

# MySQL
./mvnw spring-boot:run -Dspring.profiles.active=mysql

# Supabase
./mvnw spring-boot:run -Dspring.profiles.active=supabase
```

### Probar notificaciones

El **NotificationService** debe estar corriendo en `http://localhost:8081`:

```bash
cd NotificationService
./mvnw spring-boot:run
```

Desde outra terminal, ejecutar Tickets:

```bash
cd Tickets-15
./mvnw spring-boot:run
```

Las notificaciones se envían automáticamente al crear o actualizar tickets.

---

## 📦 NotificationService (Microservicio Externo)

El proyecto `NotificationService/` es un microservicio independiente que recibe notificaciones del cliente.

**Puerto**: 8081  
**Endpoint**: `POST /api/notifications/send`

## ✅ Validación

- [x] Proyecto compila sin errores
- [x] FeignClient configurado con fallback
- [x] Timeouts configurados (5 segundos)
- [x] Integración en TicketService funciona

## 📝 Archivos

| Archivo | Descripción |
|---------|-------------|
| `pom.xml` | Dependencia OpenFeign |
| `client/NotificationClient.java` | FeignClient para notificaciones |
| `client/NotificationClientFallback.java` | Fallback cuando servicio no está disponible |
| `client/NotificationRestClient.java` | RestClient alternativo |
| `TicketsApplication.java` | @EnableFeignClients |
| `service/TicketService.java` | Notificaciones al crear/actualizar |
| `application.yml` | Configuración de Feign/RestClient |

**NotificationService/** (microservicio externo):
| Archivo | Descripción |
|---------|-------------|
| `pom.xml` | Dependencias Spring Boot |
| `NotificationsApplication.java` | Aplicación principal |
| `controller/NotificationController.java` | Endpoint de notificaciones |

---

**Base**: Lección 14 (Flyway)  
**Stack**: Spring Boot 4.0.5, Java 21, JPA/Hibernate, OpenFeign, H2, MySQL, PostgreSQL  
**Estado**: ✅ Completada


<!-- START OF FILE: Tickets-16_README.md -->
# Documento: Tickets-16 README
---
# Tickets-16: Lección 16 - Spring Security

## 📋 Descripción

Este proyecto implementa la **Lección 16: Spring Security** del curso DSY1103 Fullstack I.

Parte desde la base funcional de `Tickets-15`, conserva la comunicación HTTP con microservicios externos usando OpenFeign y RestClient, y agrega autenticación/autorización con Spring Security.

## 🎯 Caso de Uso Extendido (Sistema de Tickets con Gestión de Usuarios)

### Roles definidos
| Rol     | Descripción              |
|---------|--------------------------|
| USER    | Crea tickets, ve estado  |
| AGENT   | Recibe tickets asignados |
| ADMIN   | Supervisa y gestiona     |

### Modelo de datos
- **User**: id, name, email, role (USER/AGENT/ADMIN), active
- **Ticket**: relaciones con User, Category, Tags
- **Category**: One-to-Many con Ticket
- **Tag**: Many-to-Many con Ticket
- **TicketHistory**: historial de cambios de estado

---

## 🔄 Historial heredado: Cambios desde Lección 14 a Lección 15

### 1. Dependencia Agregada
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>5.0.1</version>
</dependency>
```

### 2. Notificación Cliente (FeignClient)
```java
@FeignClient(
    name = "notificationService",
    url = "${notification.service.url:http://localhost:8081}",
    fallback = NotificationClientFallback.class
)
public interface NotificationClient {
    @PostMapping("/api/notifications/send")
    Map<String, Object> sendNotification(@RequestBody Map<String, String> notification);
}
```

### 3. Fallback para Fallos
```java
@Component
public class NotificationClientFallback implements NotificationClient {
    private static final Logger logger = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public Map<String, Object> sendNotification(Map<String, String> notification) {
        logger.warn("Notification service unavailable. Notification not sent: {}", notification.get("title"));
        return Collections.singletonMap("status", "fallback");
    }
}
```

### 4. Configuración de Timeouts
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
```

### 5. Integración en TicketService
- Notificaciones al crear ticket
- Notificaciones al actualizar ticket

### 6. Habilitar Feign
```java
@SpringBootApplication
@EnableFeignClients
public class TicketsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketsApplication.class, args);
    }
}
```

---

## 📊 Requisitos del Caso Extendido por Lección

| Lección | Requisitos del Caso Extendido |
|---------|------------------------------|
| 10 | ✅ User entity con roles, Ticket con User relaciones, seed de datos |
| 11 | ✅ Perfiles con diferentes configs de BD para usuarios (H2, MySQL, Supabase) |
| 12 | ✅ Category (One-to-Many), Tag (Many-to-Many), CRUD completo |
| 13 | ✅ TicketHistory, registro automático, endpoint de historial |
| 14 | ✅ Flyway migrations con Foreign Keys a users |
| 15 | ✅ FeignClient + RestClient, notificaciones en crear/actualizar |
| 16 | Security con 3 roles (USER/AGENT/ADMIN) |
| 17 | Logging de operaciones de usuarios |
| 18 | Excepciones para casos de usuarios |

---

## 🧪 Uso

```bash
# Desarrollo (H2)
./mvnw spring-boot:run

# MySQL
./mvnw spring-boot:run -Dspring.profiles.active=mysql

# Supabase
./mvnw spring-boot:run -Dspring.profiles.active=supabase
```

### Probar notificaciones

El **NotificationService** debe estar corriendo en `http://localhost:8081`:

```bash
cd NotificationService
./mvnw spring-boot:run
```

Desde otra terminal, ejecutar Tickets:

```bash
cd Tickets-16
./mvnw spring-boot:run
```

Las notificaciones se envían automáticamente al crear o actualizar tickets.

---

## 📦 NotificationService (Microservicio Externo)

El proyecto `NotificationService/` es un microservicio independiente que recibe notificaciones del cliente.

**Puerto**: 8081
**Endpoint**: `POST /api/notifications/send`

## ✅ Validación

- [x] Proyecto compila sin errores
- [x] FeignClient configurado con fallback
- [x] Timeouts configurados (5 segundos)
- [x] Integración en TicketService funciona

## 📝 Archivos

| Archivo | Descripción |
|---------|-------------|
| `pom.xml` | Dependencia OpenFeign |
| `client/NotificationClient.java` | FeignClient para notificaciones |
| `client/NotificationClientFallback.java` | Fallback cuando servicio no está disponible |
| `client/NotificationRestClient.java` | RestClient alternativo |
| `TicketsApplication.java` | @EnableFeignClients |
| `service/TicketService.java` | Notificaciones al crear/actualizar |
| `application.yml` | Configuración de Feign/RestClient |

**NotificationService/** (microservicio externo):
| Archivo | Descripción |
|---------|-------------|
| `pom.xml` | Dependencias Spring Boot |
| `NotificationsApplication.java` | Aplicación principal |
| `controller/NotificationController.java` | Endpoint de notificaciones |

---

**Base heredada**: Lección 15 (Comunicación entre Microservicios)
**Stack**: Spring Boot 4.0.5, Java 21, JPA/Hibernate, OpenFeign, Spring Security, H2, MySQL, PostgreSQL

---

## Cambios desde Lección 15: Spring Security

Este snapshot parte desde `Tickets-15` y agrega seguridad con Spring Security para proteger la API REST de tickets.

### 1. Dependencia de Spring Security

Se agregó `spring-boot-starter-security` al `pom.xml`.

### 2. Modelo `User` preparado para autenticación

La entidad `User` ahora incluye:

- `password`: hash BCrypt de la contraseña.
- `role`: enum `USER`, `AGENT`, `ADMIN`.
- `active`: permite deshabilitar usuarios sin eliminarlos.

### 3. Usuarios desde base de datos

Se implementó `CustomUserDetailsService`, que carga usuarios desde `UserRepository.findByEmail(email)`.

Spring Security usa el email como username para HTTP Basic Auth.

### 4. Contraseñas BCrypt

Se agregó un bean `PasswordEncoder` con `BCryptPasswordEncoder`.

Las credenciales de prueba son:

| Email | Contraseña | Rol |
|-------|------------|-----|
| `admin@empresa.com` | `pass123` | ADMIN |
| `ana.garcia@empresa.com` | `user123` | USER |
| `carlos.lopez@empresa.com` | `user123` | AGENT |

### 5. Migraciones Flyway

Se agregaron migraciones por perfil:

- `src/main/resources/db/migration/mysql/V8__lesson_16_add_security_to_users.sql`
- `src/main/resources/db/migration/supabase/V8__lesson_16_add_security_to_users.sql`

Estas migraciones agregan `password`, `role` y `active` a `users`, actualizan usuarios existentes y crean el usuario administrador.

### 6. Seguridad HTTP Basic y API stateless

Se agregó `SecurityConfig` con:

- `SecurityFilterChain`.
- `csrf.disable()`.
- `SessionCreationPolicy.STATELESS`.
- `httpBasic(Customizer.withDefaults())`.
- Reglas de autorización por endpoint.
- `@EnableMethodSecurity` para habilitar `@PreAuthorize`.

### 7. Reglas de autorización

| Endpoint | Regla |
|----------|-------|
| `GET /tickets` y `GET /tickets/by-id/**` | Público |
| `POST /tickets` | USER, AGENT o ADMIN |
| `PUT /tickets/by-id/{id}` | USER, AGENT o ADMIN + regla por ticket específico |
| `PATCH /tickets/by-id/{id}` | ADMIN |
| `DELETE /tickets/by-id/{id}` | ADMIN |
| `GET /tickets/by-id/{id}/history` | ADMIN |
| `GET /tickets/by-id/{id}/audit` | ADMIN |
| `GET /users` y `GET /users/by-id/**` | Público |
| Mutaciones de `/users/**` | ADMIN |

### 8. Restricción de edición de tickets

Se agregó `TicketSecurity` y se anotó `PUT /tickets/by-id/{id}` con:

```java
@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")
```

La regla es:

- USER solo puede editar tickets creados por él.
- AGENT solo puede editar tickets asignados a él.
- ADMIN puede editar cualquier ticket.

### 9. Datos H2

`DataInitializer` ahora corre solo con perfil `h2`, crea usuarios con BCrypt y deja tickets de prueba para validar edición por creador/asignado.

### 10. Validación esperada

| Caso | Resultado esperado |
|------|--------------------|
| GET `/ticket-app/tickets` sin auth | `200 OK` |
| POST `/ticket-app/tickets` sin auth | `401 Unauthorized` |
| POST con USER válido | `201 Created` |
| DELETE con USER válido | `403 Forbidden` |
| DELETE con ADMIN válido | `204 No Content` si existe |
| PUT con USER creador | `200 OK` si payload válido |
| PUT con USER no creador | `403 Forbidden` |
| PUT con AGENT asignado | `200 OK` si payload válido |
| PUT con AGENT no asignado | `403 Forbidden` |

**Estado Lección 16**: Implementada



<!-- START OF FILE: Tickets-17_README.md -->
# Documento: Tickets-17 README
---
# Tickets-17: Lección 17 - Logging

## Descripción

Este proyecto implementa la **Lección 17: Logging** del curso DSY1103 Fullstack I.

Agrega logs usando @Slf4j de Lombok.

## Cambios desde Lección 16

### Anotación agregada
```java
@Slf4j
@Service
public class TicketService {
```

### Logging en TicketService
- `log.info("Creando ticket: {}", request.title());`

## Configuración

El logging ya viene preconfigurado con Spring Boot. Los niveles se configuran en `application.yml`:

```yaml
logging:
  level:
    cl.duoc.fullstack.tickets: INFO
```

## Estado

✅ Completado


<!-- START OF FILE: Tickets-18_README.md -->
# Documento: Tickets-18 README
---
# Tickets-18: Lección 18 - Global Exception Handling

## Descripción

Este proyecto implementa la **Lección 18: Global Exception Handling** del curso DSY1103 Fullstack I.

Manejo centralizado de excepciones con @ControllerAdvice.

## Cambios desde Lección 17

### Nuevo archivo: GlobalExceptionHandler
```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
```

### Excepciones manejadas
- `IllegalArgumentException` → 400 Bad Request
- `MethodArgumentNotValidException` → 400 Bad Request
- `EntityNotFoundException` → 404 Not Found
- `BadCredentialsException` → 401 Unauthorized
- `AccessDeniedException` → 403 Forbidden
- `Exception` genérico → 500 Internal Server Error

## Estado

✅ Completado


<!-- START OF FILE: .github_copilot-instructions.md -->
# Documento: .github copilot-instructions
---
# Copilot Instructions — DSY1103 Fullstack I Backend

## Repository layout

Each `Tickets-N/` folder is a **standalone lesson snapshot** — independent Spring Boot projects that progressively add features. The highest-numbered one is the most complete.

| Project | Lesson | Key addition |
|---------|--------|-------------|
| `Tickets/` | base | In-memory `HashMap`, no DB |
| `Tickets-10/` | 10 | JPA + H2 |
| `Tickets-11/` | 11 | MySQL + PostgreSQL profiles |
| `Tickets-12/` | 12 | Same as 11 |
| `Tickets-13/` | 13 | TicketHistory entity |
| `Tickets-14/` | 14 | Flyway + RestClient/FeignClient (AuditService) |
| `Tickets-15/` | 15 | OpenFeign to NotificationService |
| `Tickets-16/` | 16 | Spring Security (HTTP Basic, 3 roles) |
| `Tickets-17/` | 17 | `@Slf4j` logging |
| `Tickets-18/` | 18 | `@ControllerAdvice` global exception handler |

Supporting microservices (all in-memory, Spring Boot 4 + Java 21):

| Service | Port | Endpoint |
|---------|------|---------|
| `NotificationService/` | 8081 | `POST /api/notifications` |
| `AuditService/` | 8082 | varies |
| `SearchService/` | 8084 | varies |
| `SLAService/` | 8085 | varies |

`Homologacion/` is an independent workspace — not part of the main app.

## Commands (run from inside the project directory)

```cmd
# Windows — always use mvnw.cmd
cd Tickets-18

mvnw.cmd spring-boot:run
mvnw.cmd spring-boot:run -Dspring.profiles.active=mysql
mvnw.cmd spring-boot:run -Dspring.profiles.active=supabase

mvnw.cmd test
mvnw.cmd test -Dtest=TicketServiceTest

mvnw.cmd package -DskipTests
mvnw.cmd clean package
```

## Architecture (all Tickets-N projects)

5-layer package structure under `cl.duoc.fullstack.tickets`:

```
controller/   HTTP mapping, @Valid on DTOs, ResponseEntity responses
service/      Business rules, DTO→model mapping, Optional<T> returns
respository/  JPA repositories (or HashMap in base Tickets/)
model/        JPA entities (Lombok POJOs)
dto/          Java records — requests, results, ErrorResponse
```

> **`respository` (missing the first 'o') is intentional.** Always match this spelling when adding new files.

## Key conventions

### Models vs DTOs
- Models (`model/`) are Lombok entities: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`
- DTOs (`dto/`) are Java `record` types — requests, result projections, and `ErrorResponse`
- Validation annotations (`@NotBlank`, `@Size`) go on DTOs only, never on models

### Endpoints
- Context path: `/ticket-app` — always required in full URLs (`http://localhost:8080/ticket-app/tickets`)
- Paths use kebab-case: `/by-id/{id}`, `/ticket-app`
- Single-entity lookups: service returns `Optional<T>`, controller uses `.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build())`
- DELETE: service returns `boolean` → controller returns `204 No Content`

### Error handling
- `IllegalArgumentException` (business validation) → `409 Conflict` in earlier lessons; `400 Bad Request` in Tickets-18+ via `@ControllerAdvice`
- Bean Validation (`@Valid`) → `400 Bad Request` with field error messages joined by `", "`
- Tickets-18+ uses `GlobalExceptionHandler` in `config/` with `@ControllerAdvice @Slf4j`
- `ErrorResponse` is always `public record ErrorResponse(String message) {}`

### Database profiles
- `h2` — default dev profile (`ddl-auto: create-drop`, Flyway disabled)
- `mysql` — XAMPP/local MySQL (env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`)
- `supabase` — PostgreSQL on Supabase (same env vars)
- Profile activation: `SPRING_PROFILES_ACTIVE=h2` in `.env` (copy from `.env.example`)

### Security (Tickets-16+)
- HTTP Basic Auth, stateless sessions
- Roles: `USER`, `AGENT`, `ADMIN`
- Seed users from `DataInitializer`: `admin/admin123`, `agent1/agent123`, `user1/user123`
- Permissions: `/tickets/**` → USER+AGENT+ADMIN; `/users/**`, `/categories/**`, `/tags/**` → ADMIN only

### FeignClient pattern (Tickets-14+)
- Interface in `client/` annotated with `@FeignClient`
- Always has a `*Fallback` companion `@Component` class
- Timeouts configured in `application.yml` under `feign.client.config.default`
- `@EnableFeignClients` on `TicketsApplication`

### "Homologar A con B"
When asked to "homologar A con B": empty project A completely and replace with the full content of B. This is a total replacement, not a diff/patch.



<!-- START OF FILE: docs_README.md -->
# Documento: docs README
---
# 📖 Documentación — DSY1103 Fullstack I Backend

Esta carpeta centraliza toda la documentación del curso **DSY1103 - Fullstack I (Backend)**.  
Está organizada en dos grandes secciones: las **lecciones** de clase y el **material de apoyo** complementario.

---

## 🗂️ Estructura

```
docs/
├── lessons/        # Contenido directo de cada lección
│   ├── 01-web-and-http/
│   ├── 02-apis-and-rest/
│   ├── 03-first-api/
│   ├── 04-responsabilities/
│   └── 05-post/
└── extras/         # Material de apoyo autónomo
    ├── ejercicios/
    ├── env-variables/
    ├── git-github/
    ├── gitflow/
    ├── java-para-spring-boot/
    ├── java-poo/
    ├── json/
    ├── logica-proposicional/
    ├── lombok/
    ├── markdown/
    ├── matematicas-para-programar/
    ├── maven/
    ├── richardson-maturity-model/
    ├── solid/
    ├── terminal/
    ├── tips-de-programacion/
    └── yaml/
```

---

## 📚 Lecciones

Documentación generada por cada lección del curso. Incluye objetivos, guiones, decisiones de diseño, rúbricas y actividades individuales.

| # | Lección | Descripción | Documentos |
|---|---------|-------------|------------|
| 01 | [La Web y HTTP](./lessons/01-web-and-http/) | Fundamentos de la Web, modelo cliente-servidor, DNS, HTTP, request, response, métodos y códigos de estado | [Objetivo y alcance](./lessons/01-web-and-http/01_objetivo_y_alcance.md) · [La Web y HTTP](./lessons/01-web-and-http/02_la_web_y_http.md) · [Request, Response y Códigos](./lessons/01-web-and-http/03_request_response_y_codigos.md) · [Rúbrica mínima](./lessons/01-web-and-http/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/01-web-and-http/05_actividad_individual.md) |
| 02 | [APIs y REST](./lessons/02-apis-and-rest/) | Frontend vs Backend, monolito vs microservicios, qué es una API, principios REST y buenas prácticas de diseño | [Objetivo y alcance](./lessons/02-apis-and-rest/01_objetivo_y_alcance.md) · [Arquitecturas y roles](./lessons/02-apis-and-rest/02_arquitecturas_y_roles.md) · [APIs, REST y buenas prácticas](./lessons/02-apis-and-rest/03_apis_rest_y_buenas_practicas.md) · [Rúbrica mínima](./lessons/02-apis-and-rest/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/02-apis-and-rest/05_actividad_individual.md) |
| 03 | [Tu primera API](./lessons/03-first-api/) | Crear un proyecto Spring Boot desde cero con IntelliJ, construir `GET /greetings` y entender el ciclo HTTP completo | [Objetivo y alcance](./lessons/03-first-api/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/03-first-api/02_guion_paso_a_paso.md) · [Cómo funciona HTTP](./lessons/03-first-api/03_como_funciona_http.md) · [Rúbrica mínima](./lessons/03-first-api/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/03-first-api/05_actividad_individual_greetings.md) |
| 04 | [Separación de responsabilidades](./lessons/04-responsabilities/) | Patrón Controller → Service → Repository (CSR) aplicado a una API REST de tickets | [Objetivo y alcance](./lessons/04-responsabilities/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/04-responsabilities/02_guion_paso_a_paso.md) · [Decisiones REST y CSR](./lessons/04-responsabilities/03_decisiones_rest_y_csr.md) · [Rúbrica mínima](./lessons/04-responsabilities/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/04-responsabilities/05_actividad_individual_users.md) |
| 05 | [POST y creación de recursos](./lessons/05-post/) | Recibir datos del cliente con `@RequestBody`, lógica de negocio en el `Service` (validación, estado y fechas) y respuesta `201 Created` con `ResponseEntity` | [Objetivo y alcance](./lessons/05-post/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/05-post/02_guion_paso_a_paso.md) · [Decisiones POST y HTTP](./lessons/05-post/03_decisiones_post_y_http.md) · [Rúbrica mínima](./lessons/05-post/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/05-post/05_actividad_individual_categories.md) |
| 06 | [CRUD completo](./lessons/06-crud/) | Implementar `GET /id`, `PUT /id` y `DELETE /id`; idempotencia y reglas REST para métodos de escritura | [Objetivo y alcance](./lessons/06-crud/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/06-crud/02_guion_paso_a_paso.md) · [Reglas REST e idempotencia](./lessons/06-crud/03_reglas_rest_e_idempotencia.md) · [Rúbrica mínima](./lessons/06-crud/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/06-crud/05_actividad_individual.md) |
| 07 | [Manejo de errores](./lessons/07-errors/) | Respuestas de error estructuradas con `ErrorResponse`, `@ExceptionHandler` y códigos HTTP correctos | [Objetivo y alcance](./lessons/07-errors/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/07-errors/02_guion_paso_a_paso.md) · [Manejo global vs local](./lessons/07-errors/03_manejo_global_vs_local.md) · [Rúbrica mínima](./lessons/07-errors/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/07-errors/05_actividad_individual.md) |
| 08 | [DTOs](./lessons/08-dto/) | Separar el modelo de dominio del contrato de la API con `TicketRequest`; `@Valid` y validación declarativa | [Objetivo y alcance](./lessons/08-dto/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/08-dto/02_guion_paso_a_paso.md) · [Por qué DTO](./lessons/08-dto/03_por_que_dto.md) · [Rúbrica mínima](./lessons/08-dto/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/08-dto/05_actividad_individual.md) |
| 09 | [Repository con Map](./lessons/09-map-repository/) | Refactorizar a `Map<Long, Ticket>` para acceso O(1); filtro `?status=` con `@RequestParam` | [Objetivo y alcance](./lessons/09-map-repository/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/09-map-repository/02_guion_paso_a_paso.md) · [Map vs List y CSR](./lessons/09-map-repository/03_map_vs_list_y_csr.md) · [Rúbrica mínima](./lessons/09-map-repository/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/09-map-repository/05_actividad_individual.md) |
| 10 | [JPA y ORM](./lessons/10-jpa-intro/) | Migrar de almacenamiento en memoria a base de datos real con `@Entity`, `@Id`, `@GeneratedValue` y `JpaRepository` | [Objetivo y alcance](./lessons/10-jpa-intro/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/10-jpa-intro/02_guion_paso_a_paso.md) · [JPA y ORM](./lessons/10-jpa-intro/03_jpa_y_orm.md) · [Rúbrica mínima](./lessons/10-jpa-intro/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/10-jpa-intro/05_actividad_individual.md) |
| 11 | [Configuración de base de datos](./lessons/11-database-config/) | Conectar a MySQL local (XAMPP) y a Supabase (PostgreSQL en la nube); opciones de `ddl-auto` | [Objetivo y alcance](./lessons/11-database-config/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/11-database-config/02_guion_paso_a_paso.md) · [MySQL vs PostgreSQL](./lessons/11-database-config/03_mysql_vs_postgresql.md) · [Rúbrica mínima](./lessons/11-database-config/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/11-database-config/05_actividad_individual.md) |
| 12 | [Relaciones entre entidades](./lessons/12-relations/) | `@ManyToOne`, `@OneToMany`, `@JoinColumn` y `@Column`; entidad `User` con usuario creador y asignado en `Ticket` | [Objetivo y alcance](./lessons/12-relations/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/12-relations/02_guion_paso_a_paso.md) · [Relaciones JPA](./lessons/12-relations/03_relaciones_jpa.md) · [Rúbrica mínima](./lessons/12-relations/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/12-relations/05_actividad_individual.md) |
| 13 | [Tabla de historial](./lessons/13-history/) | `@OneToMany` con `CascadeType.ALL`; entidad `TicketHistory` y registro automático de cambios de estado en el `Service` | [Objetivo y alcance](./lessons/13-history/01_objetivo_y_alcance.md) · [Guión paso a paso](./lessons/13-history/02_guion_paso_a_paso.md) · [Historial y auditoría](./lessons/13-history/03_historial_y_auditoria.md) · [Rúbrica mínima](./lessons/13-history/04_checklist_rubrica_minima.md) · [Actividad individual](./lessons/13-history/05_actividad_individual.md) |

---

## 🧩 Extras — Material de apoyo

Temas transversales necesarios para el desarrollo profesional como desarrollador Full Stack.  
Se estudian de forma **autónoma** en paralelo al curso.

| # | Tema | Descripción | Enlace |
|---|------|-------------|--------|
| 1 | Git y GitHub | Control de versiones distribuido y flujo de trabajo colaborativo | [→ Ver](./extras/git-github/README.md) |
| 2 | GitFlow | Modelo de ramificación con ramas `main`, `develop`, `feature/*`, `release/*` y `hotfix/*` | [→ Ver](./extras/gitflow/README.md) |
| 3 | Java para Spring Boot | Mini curso desde sintaxis esencial hasta arquitectura Spring Boot (Java 21 LTS) | [→ Ver](./extras/java-para-spring-boot/README.md) |
| 4 | JSON | Formato de intercambio de datos en APIs REST y su manejo con Jackson en Spring Boot | [→ Ver](./extras/json/README.md) |
| 5 | Lombok | Librería Java para eliminar código boilerplate mediante anotaciones | [→ Ver](./extras/lombok/README.md) |
| 6 | Markdown | Lenguaje de marcado para documentación técnica | [→ Ver](./extras/markdown/README.md) |
| 7 | Maven | Gestión de dependencias, ciclo de vida de build y estructura estándar de proyectos Java | [→ Ver](./extras/maven/README.md) |
| 8 | Modelo de Madurez de Richardson | Niveles 0–3 para clasificar la calidad de una API REST | [→ Ver](./extras/richardson-maturity-model/README.md) |
| 9 | Principios SOLID | Cinco principios de diseño OO para código mantenible y extensible | [→ Ver](./extras/solid/README.md) |
| 10 | Variables de Entorno | Configuración segura con `.env`, Spring Boot (`@Value`, perfiles) e IntelliJ IDEA | [→ Ver](./extras/env-variables/README.md) |
| 11 | Lógica Proposicional | Proposiciones, operadores lógicos, tablas de verdad y leyes de De Morgan aplicadas a Java | [→ Ver](./extras/logica-proposicional/README.md) |
| 12 | Ejercicios Prácticos | 20 ejercicios progresivos que integran Java, POO y Lógica Proposicional en casos de uso reales | [→ Ver](./extras/ejercicios/README.md) |
| 13 | Terminal — Bash y Windows | Comandos esenciales de terminal para desarrolladores backend (Linux/macOS y Windows) | [→ Ver](./extras/terminal/README.md) |
| 14 | YAML | Formato de configuración estándar de Spring Boot (`application.yml`) y sus conceptos clave | [→ Ver](./extras/yaml/README.md) |
| 15 | Matemáticas para Programar | Operaciones básicas, contador, acumulador, descuentos, cargos, redondeo y `BigDecimal` | [→ Ver](./extras/matematicas-para-programar/README.md) |
| 16 | Tips de Programación | 24 situaciones reales de menor a mayor complejidad: razonamiento, error común y solución en Java (consola y REST API) | [→ Ver](./extras/tips-de-programacion/README.md) |

> 📋 Ver índice completo de extras: [`extras/README.md`](./extras/README.md)
> 🗺️ Ver roadmap de estudio recomendado: [`roadmap.md`](./roadmap.md)

---

## 🔗 Referencias del repositorio

| Recurso | Enlace |
|---------|--------|
| README principal | [`../README.md`](../README.md) |
| Proyecto Tickets (API) | [`../Tickets/README.md`](../Tickets/README.md) |
| 🗺️ Roadmap de estudio | [`roadmap.md`](./roadmap.md) |

---

*Última actualización: Marzo 2026 — DSY1103 Fullstack I Backend*




<!-- START OF FILE: docs_roadmap.md -->
# Documento: docs roadmap
---
# 🗺️ Roadmap de Estudio — Material de Apoyo

> **Curso:** DSY1103 Fullstack I Backend — Spring Boot 4 · Java 21
> **Actualizado:** Marzo 2026

Este roadmap te ayuda a organizar el estudio del [material de apoyo](./extras/README.md) según el tiempo que tienes disponible. No es obligatorio leerlo todo de una vez — la idea es ir leyendo **lo correcto, en el momento correcto**.

---

## Mapa de dependencias

Este árbol muestra qué extras dependen de otros y en qué orden tiene más sentido estudiarlos. Los niveles más altos impactan directamente tu capacidad de programar; los más bajos complementan tu perfil profesional.

```
Nivel 1 — Programación (mayor impacto inmediato)
├── 📚  Conceptos de Programación → vocabulario universal, previo o paralelo a Java
├── ☕  Java para Spring Boot   → lenguaje base de todo el curso
│       └── 🧠  Lógica Proposicional  → condiciones, validaciones y reglas de negocio
├── ➕  Matemáticas para Programar → operaciones, contadores, descuentos, redondeo
├── 💡  Tips de Programación   → razonamiento y resolución de situaciones reales
├── 🛠️  Lombok                 → menos código repetitivo desde el primer día
├── 🔷  JSON                   → formato de todas las respuestas REST
├── 📄  YAML                   → configuración de Spring Boot
├── 🔐  Variables de Entorno   → configuración segura sin hardcodear valores
└── 📦  Maven                  → entender cómo se construye y ejecuta el proyecto

Nivel 2 — Control de versiones
└── 🌿  Git y GitHub           → historial, colaboración, entrega de actividades

Nivel 3 — Entorno de trabajo
├── 🗂️  Uso de PC              → sistema de archivos, dónde viven tus proyectos
├── 🖥️  Terminal (Bash / Windows)
└── 📝  Markdown

Nivel 4 — Complementario
├── 🧱  SOLID
├── 📊  Modelo de Madurez de Richardson
├── 🌊  GitFlow
└── 🏋️  Ejercicios Prácticos
```

> 💡 No necesitas terminar un nivel para avanzar al siguiente. El árbol muestra **qué ayuda entender mejor qué**, no una secuencia estricta.

---

## Tabla resumen de extras

| Extra | Nivel | Tiempo estimado |
|-------|-------|-----------------|
| [📚 Conceptos de Programación](./extras/conceptos-de-programacion/README.md) | 🔴 1 — Programación | 7 – 10 h |
| [☕ Java para Spring Boot](./extras/java-para-spring-boot/README.md) | 🔴 1 — Programación | 8 – 12 h |
| [🧠 Lógica Proposicional](./extras/logica-proposicional/README.md) | 🔴 1 — Programación | 2 – 3 h |
| [➕ Matemáticas para Programar](./extras/matematicas-para-programar/README.md) | 🔴 1 — Programación | 2 – 3 h |
| [💡 Tips de Programación](./extras/tips-de-programacion/README.md) | 🔴 1 — Programación | 3 – 4 h |
| [🛠️ Lombok](./extras/lombok/README.md) | 🔴 1 — Programación | 1 – 1.5 h |
| [🔷 JSON](./extras/json/README.md) | 🔴 1 — Programación | 1 – 1.5 h |
| [📄 YAML](./extras/yaml/README.md) | 🔴 1 — Programación | 1 – 2 h |
| [🔐 Variables de Entorno](./extras/env-variables/README.md) | 🔴 1 — Programación | 1 – 2 h |
| [📦 Maven](./extras/maven/README.md) | 🔴 1 — Programación | 1 – 2 h |
| [🌿 Git y GitHub](./extras/git-github/README.md) | 🟠 2 — Control de versiones | 2 – 3 h |
| [🗂️ Uso de PC](./extras/uso-de-pc/README.md) | 🟡 3 — Entorno | 1 – 1.5 h |
| [🖥️ Terminal](./extras/terminal/README.md) | 🟡 3 — Entorno | 2 – 3 h |
| [📝 Markdown](./extras/markdown/README.md) | 🟡 3 — Entorno | 30 – 45 min |
| [🧱 SOLID](./extras/solid/README.md) | 🟢 4 — Complementario | 2 – 3 h |
| [📊 Richardson](./extras/richardson-maturity-model/README.md) | 🟢 4 — Complementario | 30 – 45 min |
| [🌊 GitFlow](./extras/gitflow/README.md) | 🟢 4 — Complementario | 1 – 1.5 h |
| [🏋️ Ejercicios Prácticos](./extras/ejercicios/README.md) | 🟢 4 — Complementario | 5 – 10 h |

**Total estimado:** 38 – 63 horas según profundidad

---

## Rutas según tiempo disponible

---

### ⚡ Ruta Express
> **Para quién:** tienes muy poco tiempo libre y necesitas lo más útil ahora mismo.
> **Tiempo total:** 4 – 5 horas
> **Objetivo:** impactar directamente tu forma de escribir código desde esta semana.

```
1. ☕  Java para Spring Boot (módulos 00 – 03)  → 2 h   sintaxis, tipos, control de flujo
2. 🔷  JSON                                     → 1 h   leer y entender respuestas REST
3. 📄  YAML                                     → 1 h   configurar application.yml sin errores
4. 🛠️  Lombok                                   → 30 min eliminar boilerplate en tus clases
```

> ⏭️ Con esto puedes seguir el código de clase sin quedarte atrás. El resto se va sumando semana a semana.

---

### 📅 Ruta Semanal
> **Para quién:** tienes entre 1 y 2 horas libres durante la semana, distribuidas en días de semana.
> **Tiempo total:** distribuido en 8 semanas
> **Objetivo:** avanzar de forma sostenida sin saturarte.

| Semana | Extra(s) | Tiempo | Enfoque |
|--------|----------|--------|---------|
| **1** | ☕ Java para Spring Boot (módulos 00 – 03) | 2 h | Sintaxis base y control de flujo |
| **2** | ☕ Java para Spring Boot (módulos 04 – 07) | 2 h | Métodos, clases, POO |
| **3** | ☕ Java para Spring Boot (módulos 08 – 12) | 2 h | Colecciones, lambdas, generics |
| **4** | 🧠 Lógica Proposicional + 🛠️ Lombok | 2 h | Condiciones limpias, menos boilerplate |
| **5** | 🔷 JSON + 📄 YAML + 📦 Maven | 2 h | Datos, configuración, build |
| **6** | 🔐 Variables de Entorno + 🌿 Git y GitHub | 2 h | Configuración segura y control de versiones |
| **7** | 🖥️ Terminal + 📝 Markdown | 2 h | Entorno de trabajo profesional |
| **8** | 🧱 SOLID + 📊 Richardson | 2 h | Calidad y diseño de APIs |

> 🏋️ Los Ejercicios Prácticos y GitFlow quedan como actividad libre cuando termines cada bloque o cuando el ritmo del curso lo permita.

---

### 🔥 Ruta de Fin de Semana
> **Para quién:** puedes dedicar 2 – 3 horas un sábado o domingo, sin que interfiera con el descanso.
> **Tiempo por sesión:** máximo 3 horas
> **Objetivo:** avanzar un extra completo por fin de semana, sin prisa.

Cada fin de semana es una sesión independiente. Elige según en qué parte del roadmap estás:

| Fin de semana | Extra | Duración | Qué vas a lograr |
|---------------|-------|----------|------------------|
| **1** | ☕ Java para Spring Boot (módulos 00 – 05) | 2.5 h | Tener el lenguaje base claro |
| **2** | ☕ Java para Spring Boot (módulos 06 – 12) | 2.5 h | POO, colecciones y lambdas |
| **3** | 🔷 JSON + 📄 YAML + 📦 Maven | 2.5 h | Datos y configuración del proyecto |
| **4** | 🔐 Variables de Entorno + 🛠️ Lombok | 2 h | Configuración segura y menos código |
| **5** | 🌿 Git y GitHub | 2.5 h | Control de versiones completo |
| **6** | 🧠 Lógica Proposicional | 2 h | Condiciones y validaciones más claras |
| **7** | 🖥️ Terminal + 📝 Markdown | 2 h | Entorno de trabajo |
| **8** | 🧱 SOLID + 📊 Richardson | 2 h | Calidad de código y APIs |

> ☕ El café del domingo cuenta. Dos horas con foco valen más que ocho horas distracted.

---

### 🏆 Ruta Completa
> **Para quién:** quieres dominar todo el material de apoyo sin dejar cabos sueltos.
> **Tiempo total:** 25 – 45 horas
> **Objetivo:** construir una base profesional sólida en paralelo al curso.

Sigue el orden del mapa de dependencias, dedicando al menos una sesión a cada extra antes de avanzar al siguiente nivel:

**Nivel 1 — Programación** `~30 h`
1. [Conceptos de Programación](./extras/conceptos-de-programacion/README.md) — todos los módulos (01 – 13)
2. [Java para Spring Boot](./extras/java-para-spring-boot/README.md) — todos los módulos (00 – 12)
3. [Lógica Proposicional](./extras/logica-proposicional/README.md)
4. [Matemáticas para Programar](./extras/matematicas-para-programar/README.md)
5. [Tips de Programación](./extras/tips-de-programacion/README.md)
6. [Lombok](./extras/lombok/README.md)
7. [JSON](./extras/json/README.md)
8. [YAML](./extras/yaml/README.md)
9. [Variables de Entorno](./extras/env-variables/README.md)
10. [Maven](./extras/maven/README.md)

**Nivel 2 — Control de versiones** `~3 h`

11. [Git y GitHub](./extras/git-github/README.md)

**Nivel 3 — Entorno de trabajo** `~4 h`

12. [Uso de PC](./extras/uso-de-pc/README.md) — sistema de archivos y organización
13. [Terminal](./extras/terminal/README.md) — Bash y/o CMD + PowerShell
14. [Markdown](./extras/markdown/README.md)

**Nivel 4 — Complementario** `~12 h`

15. [SOLID](./extras/solid/README.md)
16. [Modelo de Madurez de Richardson](./extras/richardson-maturity-model/README.md)
17. [GitFlow](./extras/gitflow/README.md)
18. [Ejercicios Prácticos](./extras/ejercicios/README.md) — Serie I + Serie II

---

## Rutas según tu perfil

Cada perfil parte de una base distinta. Elige el que mejor te describa para ahorrar tiempo en lo que ya dominas y enfocarte en lo que realmente necesitas.

| Perfil | Tiempo total | Para quién |
|--------|--------------|------------|
| 🎓 Estándar | 19 – 25 h | Sigues el curso al ritmo de clases |
| 🐣 Desde Cero | 51 – 66 h | Nunca has programado |
| 🔁 Refresh Java | 13 – 17 h | Estudiaste Java hace años |
| 🌐 Java → Web | 11 – 15 h | Sabes Java pero no HTTP/REST |
| 🚀 Spring Pro | 2 – 4 h | Ya construiste APIs con Spring Boot |

---

### 🎓 Ruta Estándar — Seguimiento del curso
> **Para quién:** estás cursando DSY1103 y quieres ir al ritmo de las clases sin atrasarte.
> **Tiempo total:** 19 – 25 h distribuidas durante el semestre
> **Objetivo:** llegar preparado a cada clase con los prerrequisitos resueltos.

**Antes de empezar (semana 0)**
1. 🗂️ Uso de PC — sistema de archivos y dónde guardar tus proyectos (lectura rápida si ya te manejas)
2. 📚 Conceptos de Programación — módulos 01, 03, 04 (vocabulario, estructuras de datos, errores)
3. ☕ Java para Spring Boot — módulos 00 – 03 (sintaxis y control de flujo)
4. 🔷 JSON + 📄 YAML
5. 🛠️ Lombok

**Primeras 4 semanas del curso**
6. ☕ Java para Spring Boot — módulos 04 – 12 (POO, colecciones, lambdas)
7. 📚 Conceptos de Programación — módulos 02, 07 (paradigmas, principios de buen código)
8. 📦 Maven
9. 🔐 Variables de Entorno
10. 🌿 Git y GitHub

**Cuando el ritmo del curso lo permita**
11. 🧠 Lógica Proposicional
12. 🧱 SOLID + 📊 Richardson
13. 🏋️ Ejercicios Prácticos (Serie I)

> 📌 Esta ruta asume que vas resolviendo los `Tickets-N/` en paralelo. Los extras refuerzan lo que ya estás viendo en clase.

---

### 🐣 Ruta Desde Cero — Sin conocimientos de programación
> **Para quién:** nunca has programado y este curso es tu punto de partida.
> **Tiempo total:** 51 – 66 h
> **Objetivo:** construir una base sólida antes de tocar Spring Boot.

**Fase 0 — Conocer tu computador** `~1 h`
1. 🗂️ Uso de PC — sistema de archivos, rutas, dónde viven los proyectos (obligatorio antes de abrir IntelliJ)

**Fase 1 — Pensamiento computacional y vocabulario** `~12 h`
2. 💡 Tips de Programación — cómo razonar problemas
3. 📚 Conceptos de Programación — módulos 01, 02, 03, 04 (vocabulario, paradigmas, estructuras de datos, errores)
4. 🧠 Lógica Proposicional — condiciones y validaciones
5. ➕ Matemáticas para Programar — operadores, contadores, redondeo

**Fase 2 — Lenguaje base** `~14 h`
6. ☕ Java para Spring Boot — TODOS los módulos (00 – 12), sin saltar
7. 🖥️ Terminal — operaciones básicas
8. 📝 Markdown — para documentar tu trabajo

**Fase 3 — Formatos y configuración** `~6 h`
9. 🔷 JSON
10. 📄 YAML
11. 📦 Maven
12. 🛠️ Lombok
13. 🔐 Variables de Entorno

**Fase 4 — Versionado y diseño** `~10 h`
14. 🌿 Git y GitHub
15. 📚 Conceptos de Programación — módulos 05, 06, 07 (recursividad, Big O, principios de buen código)
16. 🧱 SOLID
17. 📊 Richardson

**Fase 5 — Práctica deliberada** `~5 – 10 h`
18. 🏋️ Ejercicios Prácticos (Serie I, luego Serie II)

> 🛟 No avances de fase si no entendiste la anterior. Cada base sostiene la siguiente — saltar etapas se paga después.

---

### 🔁 Ruta Refresh Java — Tuve Java alguna vez
> **Para quién:** estudiaste Java hace años y necesitas reactivarlo sin partir de cero.
> **Tiempo total:** 13 – 17 h
> **Objetivo:** recordar lo esencial y aprender lo que cambió (records, lambdas, streams).

**Refresh rápido** `~3 h`
1. ☕ Java para Spring Boot — módulos 00 – 03 (lectura rápida)
2. ☕ Java para Spring Boot — módulos 04 – 07 (POO)

**Probablemente lo nuevo para ti** `~5 h`
3. ☕ Java para Spring Boot — módulos 08 – 12 (colecciones modernas, lambdas, streams, generics)
4. 📚 Conceptos de Programación — módulos 07, 10 (principios modernos, memoria/GC explicada bien)
5. 🛠️ Lombok — convención que no existía cuando aprendiste

**Configuración y formato** `~4 h`
6. 🔷 JSON
7. 📄 YAML
8. 📦 Maven
9. 🔐 Variables de Entorno

**Si te sobra tiempo**
10. 📚 Conceptos de Programación — módulos 11, 12, 13 (patrones de diseño GoF)
11. 🧱 SOLID — repaso de diseño
12. 🏋️ Ejercicios Prácticos (Serie I) — para confirmar que recordaste

> ⏭️ Si al revisar los módulos 00 – 07 sientes que todo es familiar, sáltalos. El verdadero refresh está en el 08 – 12.

---

### 🌐 Ruta Java → Web — Sé Java pero no sé nada de internet
> **Para quién:** dominas Java pero nunca has trabajado con HTTP, APIs, JSON o servidores web.
> **Tiempo total:** 11 – 15 h
> **Objetivo:** entender cómo Java se conecta con el mundo web.

**Puedes saltarte sin culpa**
- ☕ Java para Spring Boot — solo revisa módulos 11 – 12 si no manejas lambdas/streams
- 🧠 Lógica Proposicional · ➕ Matemáticas — ya las dominas

**Lo que sí necesitas** `~7 h`
1. 🔷 JSON — formato base de toda comunicación REST
2. 📄 YAML — configuración declarativa de Spring
3. 📦 Maven — gestión de dependencias y ciclo de build
4. 🔐 Variables de Entorno — separar configuración del código
5. 🛠️ Lombok — convención estándar del ecosistema Spring
6. 📊 Richardson — niveles de madurez REST (clave conceptual para diseñar bien)
7. 🧱 SOLID — diseño en capas Controller / Service / Repository

**Conceptos de backend** `~3 h`
8. 📚 Conceptos de Programación — módulos 08, 09 (concurrencia/hilos, I/O bloqueante vs no bloqueante)
9. 📚 Conceptos de Programación — módulos 11, 12, 13 (patrones de diseño que verás en Spring)

**Entorno profesional** `~3 h`
10. 🌿 Git y GitHub — flujo de trabajo
11. 🖥️ Terminal — operar el proyecto desde consola

> 🌐 Tu mayor brecha no es Java, es entender HTTP, REST y cómo se serializa todo. Prioriza JSON + Richardson.

---

### 🚀 Ruta Spring Pro — Ya tengo experiencia con Spring Boot
> **Para quién:** has construido APIs con Spring Boot antes y solo necesitas alinearte con este repo.
> **Tiempo total:** 2 – 4 h
> **Objetivo:** ubicarte rápido en el código y detectar las particularidades del proyecto.

**Lectura obligatoria** `~30 min`
1. `CLAUDE.md` y `AGENTS.md` — convenciones del repo (paquete `respository` con typo intencional, etc.)
2. README de cada `Tickets-N/` — qué cambia entre lecciones

**Extras que vale la pena revisar** `~2 – 3 h`
3. 📊 Richardson — para alinear vocabulario REST con el del curso
4. 🌊 GitFlow — si vas a contribuir o entregar
5. 🧱 SOLID — repaso rápido de diseño en capas
6. 🏋️ Ejercicios Prácticos (Serie II) — desafíos no triviales

> ⏭️ Omite el Nivel 1 completo salvo que detectes algo desconocido. Usa el repo como referencia, no como tutorial.

---

## ¿Y si tengo aún menos tiempo?

Si debes elegir **un solo extra por semana**, este es el orden de mayor a menor impacto real en tu código:

| Semana | Extra único | Por qué primero |
|--------|-------------|-----------------|
| 1 | ☕ Java para Spring Boot | El código de clase no tiene sentido sin el lenguaje |
| 2 | 🔷 JSON + 📄 YAML | Rápidos y necesarios para el proyecto actual |
| 3 | 🔐 Variables de Entorno | Evita malos hábitos desde el inicio |
| 4 | 🛠️ Lombok | Menos código que escribir, más tiempo para entender |
| 5 | 🌿 Git y GitHub | Necesario para entregar actividades correctamente |
| 6 | 🧠 Lógica Proposicional | Mejora la calidad de tus condiciones y validaciones |
| 7 | 📦 Maven | Entiende qué hace el proyecto por debajo |
| 8+ | 🖥️ Terminal, 🧱 SOLID, 🏋️ Ejercicios | Según el tiempo y el interés |

---

## Leyenda de niveles

| Nivel | Descripción |
|-------|-------------|
| 🔴 **1 — Programación** | Impacta directamente cómo escribes código hoy |
| 🟠 **2 — Control de versiones** | Necesario para trabajar y entregar actividades |
| 🟡 **3 — Entorno** | Mejora tu flujo de trabajo como desarrollador |
| 🟢 **4 — Complementario** | Enriquece tu perfil profesional sin urgencia |

---

## Cómo usar este repositorio

Este repositorio contiene el material de apoyo del curso, organizado en dos grandes áreas:

1. **Proyectos progresivos** (`Tickets/`, `Tickets-10/`, ..., `Tickets-18/`) — cada carpeta es una lección独立性
2. **Extras** (`docs/extras/`) — material teóricos opcionales

### Según tu nivel

#### 🐣 Nuevo en programación
> No has programado antes o llevas poco tiempo.

Usa los proyectos en orden. Cada `Tickets-N/` incluye TODO lo necesario para esa lección:
- Clases completas con comentarios
- Tests para verificar
- Configuración lista para ejecutar

**Extras requeridos:**
- ☕ Java para Spring Boot (módulos 00 – 07)
- 🔷 JSON
- 📄 YAML
- 🛠️ Lombok

**Extras opcionales:**
- 🧠 Lógica Proposicional
- ➕ Matemáticas para Programar
- 🖥️ Terminal

```
Secuencia sugerida:
Tickets/   → Tickets-10/ → ... → Tickets-18/
   (base)      (JPA)            (Exceptions)
```

---

#### 🔰 Principiante
> Ya conoces lo básico de Java u otro lenguaje.

Puedes saltar directamente a la lección que necesitas:
- ¿Necesitas JPA? → `Tickets-10/`
- ¿Necesitas Security? → `Tickets-16/`
- ¿Necesitas Microservicios? → `Tickets-15/`
- ¿Necesitas Exception Handling? → `Tickets-18/`

**Extras requeridos:**
- 📄 YAML
- 📦 Maven
- 🔐 Variables de Entorno

**Extras opcionales:**
- 💡 Tips de Programación
- 🧱 SOLID
- 📊 Richardson

---

#### 🚀 Avanzado
> Ya has trabajado con Spring Boot antes.

Usa este repo como referencia rápida:
- Busca el patrón que necesitas en los proyectos
- Revisa los extras para profundizar
- Omitir lo que ya dominas

**Extras opcionales:**
- 🌊 GitFlow
- 🏋️ Ejercicios Prácticos (Serie II)
- 🧱 SOLID

---

### Estructura de cada proyecto

Cada carpeta `Tickets-N/` sigue la misma estructura:

```
Tickets-N/
├── src/main/java/           # Código fuente
│   └── cl/duoc/fullstack/
│       ├── controller/      # Endpoints REST
│       ├── service/        # Lógica de negocio
│       ├── repository/     # Acceso a datos
│       ├── model/         # Entidades
│       ├── dto/           # Objetos de transferencia
│       └── config/        # Configuración
├── src/main/resources/
│   ├── application.yml    # Configuración principal
│   ├── application-h2.yml
│   └── db/migration/    # Flyway migrations
└── README.md           # Cambios de la lección
```

---

### Comandos comunes

```bash
# Ejecutar proyecto (desde su directorio)
cd Tickets-10
mvnw.cmd spring-boot:run

# Compilar
mvnw.cmd compile

# Ejecutar tests
mvnw.cmd test
```

---

*Última actualización: Marzo 2026 — DSY1103 Fullstack I Backend*

