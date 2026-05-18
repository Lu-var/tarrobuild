<!-- START OF FILE: docs_lessons_12-relations_01_objetivo_y_alcance.md -->
# Documento: docs lessons 12-relations 01 objetivo y alcance
---
# Lección 12 — Relaciones entre entidades: usuario creador y usuario asignado

## ¿De dónde venimos?

Tu aplicación persiste tickets en base de datos real. Pero todos los tickets son anónimos: nadie sabe quién los creó ni quién está trabajando en ellos.

En un sistema de soporte real, eso no es aceptable.

---

## El problema que resolvemos

Un ticket tiene dos relaciones con personas:

- **Creador:** quién reportó el problema. Se asigna al crear el ticket y no cambia.
- **Asignado:** el técnico que está trabajando en él. Puede cambiar mientras el ticket está abierto.

En la base de datos, esto se representa con **claves foráneas** (foreign keys):

```
tabla tickets                tabla users
─────────────                ────────────
id                           id
title                        name
description                  email
status
created_by_id  ──────────►  id   (FK: quién lo creó)
assigned_to_id ──────────►  id   (FK: quién está asignado)
```

En Java, JPA traduce estas claves foráneas en referencias directas entre objetos.

---

## ¿Qué vas a construir?

Al terminar esta lección tendrás:

1. Una nueva entidad `User` con su repositorio, servicio y controlador
2. La entidad `Ticket` con dos relaciones `@ManyToOne` a `User`
   - `createdBy`: el usuario que creó el ticket (requerido, se vincula por email)
   - `assignedTo`: el usuario asignado al ticket (opcional, se asigna con PATCH)
3. La entidad `User` con dos relaciones `@OneToMany` (el lado inverso de `@ManyToOne`):
   - `createdTickets`: tickets que el usuario ha creado
   - `assignedTickets`: tickets asignados al usuario
4. `@Column` con personalización de nombres y restricciones
5. `@JoinColumn` para nombrar explícitamente las claves foráneas
6. Endpoints para crear usuarios (`POST /users`) y crear/asignar tickets (`POST /tickets`, `PATCH /tickets/{id}`)
7. DTOs de respuesta (`TicketResult`, `UserResult`) para exponer datos anidados sin serialización circular
8. Excepción personalizada `BadRequestException` para distinguir errores de negocio (409) de errores de cliente (400)

### Lo que vas a poder explicar

- ¿Qué es el "lado dueño" de una relación JPA?
- ¿Qué significa `@ManyToOne` y en qué lado de la relación va?
- ¿Qué significa `@OneToMany` y por qué usa `mappedBy`?
- ¿Qué hace `@JoinColumn` y por qué se necesita?
- ¿Por qué `@Table(name = "users")` y no `@Table(name = "user")`?
- ¿Cuál es la diferencia entre el lado "uno" y el lado "muchos" de una relación?
- ¿Cuándo usar `@OneToOne` en lugar de `@ManyToOne`?
- ¿Por qué `@ManyToMany` casi nunca se usa si la base de datos está normalizada (3FN)?

---

## Nuevos requerimientos

| Requerimiento | Descripción |
|---|---|
| **REQ-16** | Cada ticket debe registrar qué usuario lo creó |
| **REQ-17** | Cada ticket puede ser asignado a un usuario; la asignación puede cambiar |

---

## La estructura que tienes al comenzar

```
src/main/java/cl/duoc/fullstack/tickets/
├── model/
│   └── Ticket.java              ← entidad JPA sin relaciones
├── respository/
│   └── TicketRepository.java
├── service/
│   └── TicketService.java
└── controller/
    └── TicketController.java
```

La estructura al terminar:

```
src/main/java/cl/duoc/fullstack/tickets/
├── exception/
│   └── BadRequestException.java     ← nueva excepción personalizada
├── model/
│   ├── Ticket.java              ← con @ManyToOne a User (createdBy, assignedTo)
│   └── User.java                ← nueva entidad con @OneToMany (createdTickets, assignedTickets)
├── respository/
│   ├── TicketRepository.java
│   └── UserRepository.java      ← nuevo, incluye findByEmail()
├── service/
│   ├── TicketService.java       ← actualizado: busca usuario por email, nuevo assignTicket()
│   └── UserService.java         ← nuevo
├── controller/
│   ├── TicketController.java    ← actualizado: POST acepta email, nuevo PATCH /tickets/{id}
│   └── UserController.java      ← nuevo
└── dto/
    ├── TicketRequest.java       ← actualizado con createdByEmail
    ├── TicketResult.java        ← nuevo DTO de respuesta con UserResult anidado
    ├── AssignTicketRequest.java ← nuevo DTO para PATCH
    ├── UserRequest.java         ← nuevo
    └── UserResult.java          ← nuevo DTO de respuesta de usuario
```

---

## ¿Qué NO cubre esta lección?

| Tema | ¿Cuándo se ve? |
|---|---|
| Tabla de historial de cambios | Lección 13 |
| `@ManyToMany` en profundidad | Se menciona en esta lección junto a la razón por la que no la usamos (3FN) |
| `fetch = LAZY` vs `EAGER` y el problema N+1 | Se explica en el archivo conceptual de esta lección |
| DTOs de respuesta con datos anidados | Cubierto en esta lección con `TicketResult` y `UserResult` |





<!-- START OF FILE: docs_lessons_12-relations_02_guion_paso_a_paso.md -->
# Documento: docs lessons 12-relations 02 guion paso a paso
---
# Lección 12 — Tutorial paso a paso: relaciones entre entidades

---

## Paso 1: crear la entidad `User`

Crea el archivo `src/main/java/cl/duoc/fullstack/tickets/model/User.java`:

```java
package cl.duoc.fullstack.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El nombre es requerido")
  @Column(nullable = false, length = 100)
  private String name;

  @NotBlank(message = "El email es requerido")
  @Email(message = "El email no tiene un formato válido")
  @Column(nullable = false, unique = true, length = 150)
  private String email;
}
```

> **¿Por qué `@Table(name = "users")` y no `@Table(name = "user")`?**
> `USER` es una función reservada en SQL (tanto MySQL como PostgreSQL la usan para obtener el usuario conectado). Si nombras la tabla `user`, el motor de base de datos puede confundirse al parsear las consultas. Usar `users` (plural) evita el conflicto y sigue la convención de nombrar tablas en plural.

> **¿Qué hace `@Email`?**
> Es una anotación de validación de Jakarta Bean Validation que verifica que el valor tenga formato de correo electrónico (`algo@dominio.com`). Funciona junto con `@Valid` en el controlador, igual que `@NotBlank`.

---

## Paso 2: crear `UserRequest` (DTO de entrada)

Crea `src/main/java/cl/duoc/fullstack/tickets/dto/UserRequest.java`:

```java
package cl.duoc.fullstack.tickets.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {

  @NotBlank(message = "El nombre es requerido")
  private String name;

  @NotBlank(message = "El email es requerido")
  @Email(message = "El email no tiene un formato válido")
  private String email;
}
```

---

## Paso 3: crear `UserRepository`

Crea `src/main/java/cl/duoc/fullstack/tickets/respository/UserRepository.java`:

```java
package cl.duoc.fullstack.tickets.respository;

import cl.duoc.fullstack.tickets.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);
}
```

---

## Paso 4: crear `UserService`

Crea `src/main/java/cl/duoc/fullstack/tickets/service/UserService.java`:

```java
package cl.duoc.fullstack.tickets.service;

import cl.duoc.fullstack.tickets.dto.UserRequest;
import cl.duoc.fullstack.tickets.model.User;
import cl.duoc.fullstack.tickets.respository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  public List<User> getAll() {
    return repository.findAll();
  }

  public User create(UserRequest request) {
    if (repository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException(
          "Ya existe un usuario con el email '" + request.getEmail() + "'");
    }
    User user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    return repository.save(user);
  }

  public Optional<User> getById(Long id) {
    return repository.findById(id);
  }
}
```

---

## Paso 5: crear `UserController`

Crea `src/main/java/cl/duoc/fullstack/tickets/controller/UserController.java`:

```java
package cl.duoc.fullstack.tickets.controller;

import cl.duoc.fullstack.tickets.dto.UserRequest;
import cl.duoc.fullstack.tickets.model.User;
import cl.duoc.fullstack.tickets.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

  private UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @GetMapping
  public List<User> getAll() {
    return service.getAll();
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody UserRequest request) {
    try {
      User created = service.create(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new cl.duoc.fullstack.tickets.model.ErrorResponse(e.getMessage()));
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<User> getById(@PathVariable Long id) {
    return service.getById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
```

---

## Paso 6: agregar las relaciones a `Ticket`

Abre `Ticket.java` y agrega los dos campos de relación. Primero las importaciones necesarias:

```java
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
```

Luego los campos dentro de la clase, después de `effectiveResolutionDate`:

```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to_id")
  private User assignedTo;
```

**¿Qué hace cada anotación?**

| Anotación | Qué hace |
|---|---|
| `@ManyToOne` | Define la relación: muchos tickets pueden pertenecer a un mismo usuario |
| `fetch = FetchType.LAZY` | No carga el `User` de la base de datos hasta que se accede al campo |
| `@JoinColumn(name = "created_by_id")` | Nombra la columna FK en la tabla `tickets` |

> **¿Por qué no se necesita `@JsonIgnoreProperties`?**
> El entity `Ticket` nunca sale directamente del controlador — `TicketService` lo convierte a `TicketResult` antes de retornarlo. Como Jackson nunca serializa el entity, no hay riesgo de error con objetos LAZY.

> **¿Qué es `FetchType.LAZY`?**
> Cuando cargas un `Ticket`, JPA no carga automáticamente el `User` asociado. Lo carga solo si accedes a `ticket.getCreatedBy()`. Esto mejora el rendimiento: si listas 100 tickets, no haces 100 consultas adicionales a la tabla `users`.
>
> El alternativo `FetchType.EAGER` carga el `User` siempre junto con el `Ticket`. Para relaciones `@ManyToOne` el defecto es `EAGER`, por eso lo especificamos explícitamente como `LAZY`.

---

## Paso 7: actualizar `TicketRequest`

Agrega el campo `createdByEmail` (requerido) a `TicketRequest.java`:

```java
  // Campos ya existentes:
  @NotBlank(message = "El titulo es requerido")
  private String title;

  @NotBlank
  private String description;

  // Campo nuevo — requerido para POST:
  @NotBlank(message = "El email del creador es requerido")
  @Email(message = "El email no tiene un formato válido")
  private String createdByEmail;
```

> **Nota:** La asignación a un usuario (`assignedToEmail`) se realiza por separado mediante `PATCH /tickets/{id}`. No se incluye en el POST.

---

## Paso 8: actualizar `TicketService`

Antes de actualizar el servicio, crea la excepción personalizada que usaremos para distinguir errores de cliente (400) de errores de negocio (409).

Crea `src/main/java/cl/duoc/fullstack/tickets/exception/BadRequestException.java`:

```java
package cl.duoc.fullstack.tickets.exception;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }
}
```

Ahora actualiza `TicketService.java`. El método `create()` busca el usuario **por email** antes de crear el ticket:

```java
@Service
public class TicketService {

  private TicketRepository repository;
  private UserRepository userRepository;

  public TicketService(TicketRepository repository, UserRepository userRepository) {
    this.repository = repository;
    this.userRepository = userRepository;
  }

  public TicketResult create(TicketRequest request) {
    // 1. Validar título duplicado → 409 Conflict (regla de negocio)
    if (repository.existsByTitle(request.getTitle())) {
      throw new IllegalArgumentException(
          "Ya existe un ticket con el título '" + request.getTitle() + "'");
    }

    // 2. Buscar usuario creador por email → 400 Bad Request si no existe
    User creator = userRepository.findByEmail(request.getCreatedByEmail())
        .orElseThrow(() -> new BadRequestException(
            "El email '" + request.getCreatedByEmail() + "' no existe en el sistema"));

    // 3. Crear el ticket
    Ticket ticket = new Ticket();
    ticket.setTitle(request.getTitle());
    ticket.setDescription(request.getDescription());
    ticket.setStatus("NEW");
    ticket.setCreatedAt(LocalDateTime.now());
    ticket.setEstimatedResolutionDate(LocalDate.now().plusDays(5));
    ticket.setCreatedBy(creator);

    return toResult(repository.save(ticket));
  }

  public TicketResult updateById(Long id, TicketRequest request) {
    Ticket ticket = repository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Ticket con id " + id + " no encontrado"));
    ticket.setTitle(request.getTitle());
    ticket.setDescription(request.getDescription());
    if (request.getStatus() != null && !request.getStatus().isBlank()) {
      ticket.setStatus(request.getStatus());
    }
    return toResult(repository.save(ticket));
  }

  // ... getById(), deleteById(), getTickets() sin cambios

  private TicketResult toResult(Ticket ticket) {
    UserResult createdBy = ticket.getCreatedBy() != null
        ? new UserResult(ticket.getCreatedBy().getId(),
                         ticket.getCreatedBy().getName(),
                         ticket.getCreatedBy().getEmail())
        : null;
    UserResult assignedTo = ticket.getAssignedTo() != null
        ? new UserResult(ticket.getAssignedTo().getId(),
                         ticket.getAssignedTo().getName(),
                         ticket.getAssignedTo().getEmail())
        : null;
    return new TicketResult(
        ticket.getId(), ticket.getTitle(), ticket.getDescription(),
        ticket.getStatus(), ticket.getCreatedAt(), ticket.getEstimatedResolutionDate(),
        ticket.getEffectiveResolutionDate(), createdBy, assignedTo);
  }
}
```

> **¿Por qué `BadRequestException` (400) y no `IllegalArgumentException` (409)?**
> - `IllegalArgumentException` → **409 Conflict**: el cliente rompe una regla de negocio (título duplicado que ya existe en el sistema).
> - `BadRequestException` → **400 Bad Request**: el cliente envió datos inválidos (un email que no corresponde a ningún usuario).
> La distinción es semántica: 409 es "colisión", 400 es "dato incorrecto".

> **¿Por qué el Service usa `UserRepository` directamente?**
> Porque el Service coordina entre repositorios. Crear un ticket implica verificar que el usuario existe — esa es lógica de negocio que pertenece al Service, no al Controller.

---

## Paso 8.5: agregar `assignTicket()` a `TicketService`

La asignación de usuario es una operación separada del POST. Agrega este método al servicio:

```java
public Optional<TicketResult> assignTicket(Long ticketId, String assignedToEmail) {
  // 1. Si email vacío o null → desasignar
  if (assignedToEmail == null || assignedToEmail.isBlank()) {
    Optional<Ticket> ticketOpt = repository.findById(ticketId);
    if (!ticketOpt.isPresent()) {
      return Optional.empty();
    }
    Ticket ticket = ticketOpt.get();
    ticket.setAssignedTo(null);
    return Optional.of(toResult(repository.save(ticket)));
  }

  // 2. Validar que el usuario existe ANTES de buscar el ticket (400 Bad Request)
  User assignee = userRepository.findByEmail(assignedToEmail)
      .orElseThrow(() -> new BadRequestException(
          "El email '" + assignedToEmail + "' no existe en el sistema"));

  // 3. Buscar el ticket (404 si no existe)
  Optional<Ticket> ticketOpt = repository.findById(ticketId);
  if (!ticketOpt.isPresent()) {
    return Optional.empty();
  }

  // 4. Asignar y guardar
  Ticket ticket = ticketOpt.get();
  ticket.setAssignedTo(assignee);
  return Optional.of(toResult(repository.save(ticket)));
}
```

> **¿Por qué validar el email ANTES de buscar el ticket?**
> Principio de "fallo rápido": si el email es inválido, retornamos 400 inmediatamente sin hacer la consulta del ticket. Es más eficiente y da mejor feedback al cliente.

---

## Paso 8.6: crear `AssignTicketRequest` DTO

Crea `src/main/java/cl/duoc/fullstack/tickets/dto/AssignTicketRequest.java`:

```java
package cl.duoc.fullstack.tickets.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTicketRequest {

  @Email(message = "El email no tiene un formato válido")
  private String assignedToEmail;  // Opcional — null o vacío desasigna el ticket
}
```

> **¿Por qué solo `@Email` y no `@NotBlank`?**
> `@Email` valida el formato solo si el campo tiene un valor. Un campo `null` o vacío pasa la validación — eso es exactamente lo que queremos, porque vacío significa "desasignar".

---

## Paso 8.7: crear `TicketResult` y `UserResult` DTOs

Estos DTOs permiten que el JSON de respuesta incluya los datos completos del usuario (id, nombre, email) en lugar de solo el ID de la FK.

Crea `src/main/java/cl/duoc/fullstack/tickets/dto/UserResult.java`:

```java
package cl.duoc.fullstack.tickets.dto;

public record UserResult(
    Long id,
    String name,
    String email
) {}
```

Crea `src/main/java/cl/duoc/fullstack/tickets/dto/TicketResult.java`:

```java
package cl.duoc.fullstack.tickets.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TicketResult(
    Long id,
    String title,
    String description,
    String status,
    LocalDateTime createdAt,
    LocalDate estimatedResolutionDate,
    LocalDateTime effectiveResolutionDate,
    UserResult createdBy,
    UserResult assignedTo
) {}
```

El JSON de respuesta resultante tendrá esta forma:

```json
{
  "id": 1,
  "title": "Teclado no funciona",
  "status": "NEW",
  "createdBy": {
    "id": 1,
    "name": "Ana García",
    "email": "ana.garcia@empresa.com"
  },
  "assignedTo": null
}
```

---

## Paso 8.8: agregar `PATCH /tickets/{id}` al controlador

En `TicketController.java`, agrega el endpoint de asignación y actualiza el POST para capturar `BadRequestException`:

```java
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody TicketRequest request) {
  try {
    TicketResult result = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  } catch (IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
  } catch (BadRequestException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
  }
}

@PatchMapping("/{id}")
public ResponseEntity<?> assignTicket(
    @PathVariable Long id,
    @Valid @RequestBody AssignTicketRequest request) {
  try {
    return service.assignTicket(id, request.getAssignedToEmail())
        .map(result -> ResponseEntity.ok(result))
        .orElse(ResponseEntity.notFound().build());
  } catch (BadRequestException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(e.getMessage()));
  }
}
```

| Caso | HTTP |
|---|---|
| Email no existe en el sistema | `400 Bad Request` |
| Ticket no encontrado | `404 Not Found` |
| Asignación/desasignación exitosa | `200 OK` |

---

## Paso 9: Agregar @OneToMany en User

En `User.java`, agrega los imports necesarios:

```java
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
```

Luego, dentro de la clase User después del campo email, agrega:

```java
@OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
private List<Ticket> createdTickets = new ArrayList<>();

@OneToMany(mappedBy = "assignedTo", fetch = FetchType.LAZY)
private List<Ticket> assignedTickets = new ArrayList<>();
```

**¿Qué hace cada parte?**

| Elemento | Propósito |
|----------|-----------|
| `@OneToMany` | Un User tiene muchos Tickets |
| `mappedBy = "createdBy"` | Apunta al campo @ManyToOne en Ticket |
| `fetch = FetchType.LAZY` | No carga tickets al obtener User (eficiente) |
| `new ArrayList<>()` | Inicializar vacía |

> **¿Por qué no se necesita `@JsonIgnore`?**
> `User` tampoco se serializa directamente — el servicio lo convierte a `UserResult` antes de retornarlo. Las listas `createdTickets` / `assignedTickets` nunca son expuestas al JSON de respuesta.

**Trade-off: LAZY vs EAGER**

Usamos `LAZY` porque:
- Si un User tiene 1000 tickets, no cargarlos todos es mucho más eficiente
- Cargamos solo cuando el cliente los necesita
- Por defecto `@OneToMany` es LAZY

Si un User tenía pocos tickets (< 10) y los necesitabas siempre: usarías `EAGER`

**Alternativa si el User tiene MÁS DE 100 tickets:**

En lugar de `@OneToMany`, usa función en TicketRepository:

```java
List<Ticket> findByCreatedById(Long userId);
List<Ticket> findByAssignedToId(Long userId);
```

El cliente controla cuándo cargarlos con paginación.

---

## Paso 10: probar la funcionalidad completa

### Crear un usuario

```
POST http://localhost:8080/ticket-app/users
Content-Type: application/json

{
  "name": "Ana García",
  "email": "ana.garcia@empresa.com"
}
```

Respuesta esperada: `201 Created` con el usuario incluyendo su `id`.

### Crear un segundo usuario

```
POST http://localhost:8080/ticket-app/users
Content-Type: application/json

{
  "name": "Carlos López",
  "email": "carlos.lopez@empresa.com"
}
```

### Crear un ticket con creador por email

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{
  "title": "Teclado no funciona",
  "description": "Las teclas F1-F4 no responden",
  "createdByEmail": "ana.garcia@empresa.com"
}
```

Respuesta esperada: `201 Created`. El campo `createdBy` incluirá el objeto `User` completo. El campo `assignedTo` será `null`.

### Crear un ticket con email inexistente

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{
  "title": "Monitor parpadeante",
  "description": "El monitor parpadea al encender",
  "createdByEmail": "no-existe@empresa.com"
}
```

Respuesta esperada: `400 Bad Request` — el email no existe en el sistema.

### Asignar un ticket a un usuario (PATCH)

```
PATCH http://localhost:8080/ticket-app/tickets/1
Content-Type: application/json

{
  "assignedToEmail": "carlos.lopez@empresa.com"
}
```

Respuesta esperada: `200 OK` con el ticket actualizado y `assignedTo` con los datos de Carlos.

### Desasignar un ticket

```
PATCH http://localhost:8080/ticket-app/tickets/1
Content-Type: application/json

{
  "assignedToEmail": ""
}
```

Respuesta esperada: `200 OK` con `assignedTo: null`.

### Verificar en la base de datos

En phpMyAdmin o el Table Editor de Supabase, la tabla `tickets` debería mostrar las columnas `created_by_id` y `assigned_to_id` con los IDs correspondientes.

---

> Los DTOs `TicketResult` y `UserResult` se implementaron en **Paso 8.7**. Son requeridos en esta lección para exponer datos de usuario anidados en la respuesta JSON sin serialización circular.





<!-- START OF FILE: docs_lessons_12-relations_03_relaciones_jpa.md -->
# Documento: docs lessons 12-relations 03 relaciones jpa
---
# Lección 12 — Relaciones JPA: @ManyToOne, @OneToMany, @OneToOne y @ManyToMany

## Las dos caras de una relación

En una relación entre dos tablas hay siempre dos perspectivas:

```
Un User puede tener muchos Tickets   →  @OneToMany  (perspectiva del User)
Un Ticket pertenece a un solo User   →  @ManyToOne  (perspectiva del Ticket)
```

Son la misma relación vista desde cada extremo. JPA necesita que definas **al menos una de las dos perspectivas**. La otra es opcional y se llama "lado inverso".

---

## `@ManyToOne` — el lado dueño de la relación

```java
// En la clase Ticket:
@ManyToOne
@JoinColumn(name = "created_by_id")
private User createdBy;
```

`@ManyToOne` significa: "muchos `Ticket` pueden apuntar a un mismo `User`".

Este lado se llama **dueño** (*owner side*) de la relación porque es el que tiene la clave foránea en la tabla. La columna `created_by_id` existe en la tabla `tickets`, no en la tabla `users`.

```
tabla tickets              tabla users
──────────────             ────────────
id                         id
title                      name
created_by_id  ──────►    id     ← la FK vive en tickets
```

---

## `@JoinColumn` — el nombre de la clave foránea

```java
@ManyToOne
@JoinColumn(name = "created_by_id")
private User createdBy;
```

`@JoinColumn(name = "created_by_id")` define el nombre exacto de la columna FK en la tabla. Si omites `@JoinColumn`, Hibernate genera un nombre automático (generalmente `fieldname_id`). Es buena práctica siempre explicitarlo.

| Atributo | Qué hace | Ejemplo |
|---|---|---|
| `name` | Nombre de la columna FK | `created_by_id` |
| `nullable` | Si la FK puede ser NULL | `nullable = false` |
| `referencedColumnName` | Columna referenciada en la tabla destino (por defecto: PK) | Raramente se usa |

---

## `@OneToMany` — el lado inverso (opcional)

Si además quieres navegar desde un `User` hacia sus tickets:

```java
// En la clase User (lado inverso — no tiene FK propia):
@OneToMany(mappedBy = "createdBy")
private List<Ticket> createdTickets = new ArrayList<>();
```

`mappedBy = "createdBy"` le dice a JPA: "la FK está en el campo `createdBy` de la clase `Ticket`". El lado `@OneToMany` no crea columna propia — apunta al `@ManyToOne` que ya tiene la FK.

> **Sobre serialización circular:** `@OneToMany` puede causar bucle infinito si el entity se serializa directamente a JSON (`User` → `createdTickets` → `Ticket.createdBy` → `User` → ...). La solución correcta — y la que usa esta lección — es **no exponer el entity**: el Service convierte `Ticket` a `TicketResult` y `User` a `UserResult` antes de retornarlos. Jackson nunca ve el entity directamente.

**En esta lección, `@OneToMany` en `User` ES REQUERIDO.** Lo implementaremos en Paso 9 del tutorial.

---

## Trade-off: @OneToMany con LAZY vs EAGER

`@OneToMany` por defecto es `LAZY` (no carga automáticamente todos los tickets).

| Estrategia | Comportamiento | Cuándo usar |
|-----------|----------------|------------|
| **LAZY** (defecto) | Carga tickets solo si llamas `user.getCreatedTickets()` | Usuario puede tener muchos tickets (100+, 1000+) |
| **EAGER** | Carga TODOS los tickets siempre | Usuario tiene pocos tickets típicamente (< 10) |

**Ejemplo LAZY (recomendado):**
```java
@OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
private List<Ticket> createdTickets = new ArrayList<>();
```
Cuando cargas un User, los tickets NO se cargan. Se cargan solo si accedes a `user.getCreatedTickets()`.

**Alternativa: Si el User puede tener > 100 tickets**

No uses `@OneToMany`. En su lugar, crea función en TicketRepository:

```java
public interface TicketRepository extends JpaRepository<Ticket, Long> {
  List<Ticket> findByCreatedById(Long userId);
  Page<Ticket> findByCreatedById(Long userId, Pageable pageable);
}
```

El cliente carga tickets ON-DEMAND con paginación. Mejor rendimiento.

---

## `@Column` — personalizar columnas

Atributos más usados y cuándo aplicarlos:

```java
// Texto obligatorio con longitud máxima
@Column(nullable = false, length = 100)
private String name;

// Texto largo sin límite de longitud
@Column(nullable = false, columnDefinition = "TEXT")
private String description;

// Valor único en toda la tabla (como un email)
@Column(nullable = false, unique = true, length = 150)
private String email;

// Nombre distinto al del campo Java (para seguir convención snake_case en SQL)
@Column(name = "created_at")
private LocalDateTime createdAt;

// Número con precisión exacta (para precios)
@Column(precision = 10, scale = 2)
private BigDecimal price;
```

Si omites `@Column`, Hibernate crea la columna con el nombre del campo, acepta NULL y usa el tipo por defecto para ese tipo Java.

---

## `FetchType.LAZY` vs `FetchType.EAGER`

Cuando cargas un `Ticket`, ¿cuándo se carga el `User` asociado?

| `FetchType` | Comportamiento | SQL generado |
|---|---|---|
| `LAZY` | Carga el `User` solo cuando accedes a `ticket.getCreatedBy()` | 1 query para `Ticket` + 1 query para `User` solo si se accede |
| `EAGER` | Carga el `User` siempre, junto con el `Ticket` | 1 query con JOIN que ya incluye el `User` |

**¿Cuál usar?**

- `@ManyToOne` tiene `EAGER` por defecto en JPA, pero conviene cambiarlo a `LAZY` para evitar cargas innecesarias
- `@OneToMany` tiene `LAZY` por defecto — es el comportamiento correcto

```java
@ManyToOne(fetch = FetchType.LAZY)   // ← especificar explícitamente
@JoinColumn(name = "created_by_id")
private User createdBy;
```

> **¿Qué es el problema N+1?**
> Si cargas N tickets con EAGER y cada uno tiene un User, JPA hace 1 query para los tickets + N queries para los usuarios = N+1 queries. Con LAZY + un JOIN cuando sea necesario, lo reduces a 1 o 2 queries. Para este curso, LAZY es suficiente. En producción, esto se gestiona con `@EntityGraph` o JPQL con `JOIN FETCH`.

---

## Resumen: las 4 anotaciones de relación

| Anotación | En qué clase va | Para qué sirve | FK |
|---|---|---|---|
| `@ManyToOne` | La que tiene la FK (ej: `Ticket`) | "Este Ticket apunta a un User" | En esta tabla |
| `@JoinColumn` | Junto con `@ManyToOne` o `@OneToOne` | Define el nombre de la columna FK | — |
| `@OneToMany(mappedBy=...)` | La que NO tiene la FK (ej: `User`) | "Un User tiene muchos Tickets" | En la otra tabla |
| `@OneToOne` | La que tiene la FK única | "Esta entidad pertenece a exactamente otra" | En esta tabla (`UNIQUE`) |
| `@ManyToMany` | Cualquiera de las dos | "Muchos A ↔ muchos B" (raro en producción, reemplazar con entidad intermedia) | Tabla intermedia |


---

## El esquema resultante en la base de datos

Después de agregar las relaciones, Hibernate crea este esquema:

```sql
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE tickets (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                     VARCHAR(50) NOT NULL,
    description               TEXT NOT NULL,
    status                    VARCHAR(20) NOT NULL,
    created_at                DATETIME,
    estimated_resolution_date DATE,
    effective_resolution_date DATETIME,
    created_by_id             BIGINT,     -- FK → users.id
    assigned_to_id            BIGINT,     -- FK → users.id
    FOREIGN KEY (created_by_id)  REFERENCES users(id),
    FOREIGN KEY (assigned_to_id) REFERENCES users(id)
);
```

No escribes este SQL. Hibernate lo genera según las anotaciones.

---

## `@OneToOne` — Relación 1 a 1

Usa `@OneToOne` cuando **una entidad pertenece exactamente a otra, y viceversa**.

**Ejemplo en nuestro sistema:** un Ticket puede tener un `AuditLog` que registra exactamente cuándo fue creado y por quién, de forma única e irrepetible — un log por ticket, un ticket por log.

```java
// En la clase Ticket:
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "audit_log_id", unique = true)
private AuditLog auditLog;
```

La diferencia clave con `@ManyToOne` es que la FK tiene restricción `UNIQUE`: no puede haber dos tickets apuntando al mismo `AuditLog`.

```
tabla tickets              tabla audit_logs
──────────────             ─────────────────
id                         id
audit_log_id (UNIQUE) ──►  id
```

**Cuando elegir `@OneToOne` vs `@ManyToOne`:**

| Pregunta | `@OneToOne` | `@ManyToOne` |
|---|---|---|
| ¿Puede haber dos A apuntando a la misma B? | No | Sí |
| ¿La FK en la tabla tiene `UNIQUE`? | Sí | No |
| Ejemplo | Ticket → AuditLog | Ticket → User |

> **Consejo:** Si no estás seguro, pregúntate: "¿puede otro registro usar la misma entidad destino?". Si la respuesta es no → `@OneToOne`. Si es sí → `@ManyToOne`.

---

## `@ManyToMany` — Por qué casi nunca la usamos

`@ManyToMany` modela una relación donde muchos registros de A se relacionan con muchos de B.

**Ejemplo teórico:** un Ticket puede tener varias Etiquetas (`Tag`), y una etiqueta puede estar en varios tickets.

En JPA se vería así:

```java
// En Ticket:
@ManyToMany
@JoinTable(
    name = "ticket_tags",
    joinColumns = @JoinColumn(name = "ticket_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private List<Tag> tags = new ArrayList<>();
```

JPA crea automáticamente una **tabla intermedia** (`ticket_tags`) con dos columnas FK.

**¿Por qué casi nunca la usamos en producción?**

La **Tercera Forma Normal (3FN)** establece que toda dependencia funcional debe pasar por la clave primaria. En una `@ManyToMany` pura, la tabla intermedia solo tiene dos FKs — no puede guardar atributos adicionales sobre la relación (¿cuándo se asignó el tag?, ¿quién lo asignó?).

En cuanto necesitas guardar datos SOBRE la relación, la tabla intermedia se convierte en una entidad propia:

```
❌ @ManyToMany puro (tabla intermedia sin atributos):
   ticket_tags(ticket_id, tag_id)

✅ Entidad intermedia normalizada (3FN):
   ticket_tags(id, ticket_id, tag_id, assigned_at, assigned_by_id)
   → Ahora es @ManyToOne desde TicketTag a Ticket y a Tag
```

**Regla práctica:** si la relación tiene o podría tener atributos propios en el futuro → usa dos `@ManyToOne` apuntando a una entidad intermedia. Es más flexible y cumple 3FN.

```java
// Entidad intermedia (la forma correcta normalizada):
@Entity
@Table(name = "ticket_tags")
public class TicketTag {

  @ManyToOne
  @JoinColumn(name = "ticket_id")
  private Ticket ticket;

  @ManyToOne
  @JoinColumn(name = "tag_id")
  private Tag tag;

  @Column(name = "assigned_at")
  private LocalDateTime assignedAt;
}
```

> **Resumen:** `@ManyToMany` existe en JPA pero, gracias a la normalización de bases de datos (3FN), en la práctica casi siempre la reemplazamos por una entidad intermedia con dos `@ManyToOne`. Esto es más mantenible, extensible y coherente con el modelo relacional.

---





<!-- START OF FILE: docs_lessons_12-relations_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 12-relations 04 checklist rubrica minima
---
# Lección 12 — Checklist y rúbrica mínima

---

## Checklist de `User.java`

- ☐ La clase tiene `@Entity` y `@Table(name = "users")` (plural, evita conflicto con palabra reservada SQL)
- ☐ El campo `id` tiene `@Id` y `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- ☐ El campo `name` tiene `@Column(nullable = false, length = 100)`
- ☐ El campo `email` tiene `@Column(nullable = false, unique = true, length = 150)` y `@Email`
- ☐ La clase tiene `@NoArgsConstructor` (requerido por JPA)
- ☐ Todas las importaciones son de `jakarta.persistence.*`
- ☐ Tiene campo `createdTickets` con `@OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)`
- ☐ Tiene campo `assignedTickets` con `@OneToMany(mappedBy = "assignedTo", fetch = FetchType.LAZY)`
- ☐ Ambas colecciones inicializadas con `new ArrayList<>()`

---

## Checklist de `UserRequest.java`

- ☐ Existe el archivo `UserRequest.java` en el paquete `dto`
- ☐ Tiene los campos `name` y `email` con sus validaciones (`@NotBlank`, `@Email`)

---

## Checklist de `UserRepository.java`

- ☐ Es una interfaz que extiende `JpaRepository<User, Long>`
- ☐ Tiene `boolean existsByEmail(String email)`
- ☐ Tiene `Optional<User> findByEmail(String email)`

---

## Checklist de `UserService.java`

- ☐ Tiene `getAll()` que retorna `repository.findAll()`
- ☐ Tiene `create(UserRequest request)` que verifica duplicado por email con `existsByEmail()` antes de guardar
- ☐ Tiene `getById(Long id)` que retorna `Optional<User>`
- ☐ Lanza `IllegalArgumentException` cuando el email ya existe

---

## Checklist de `UserController.java`

- ☐ Mapeado en `/users`
- ☐ `GET /users` → lista todos los usuarios
- ☐ `POST /users` → crea usuario con `@Valid`, devuelve `201 Created` o `409 Conflict`
- ☐ `GET /users/{id}` → devuelve `200 OK` o `404 Not Found`

---

## Checklist de `Ticket.java` (relaciones)

- ☐ Tiene el campo `createdBy` con `@ManyToOne(fetch = FetchType.LAZY)` y `@JoinColumn(name = "created_by_id")`
- ☐ Tiene el campo `assignedTo` con `@ManyToOne(fetch = FetchType.LAZY)` y `@JoinColumn(name = "assigned_to_id")`
- ☐ **No** hay `@OneToMany` en `Ticket` apuntando a `User` (la dirección es Ticket → User, no al revés)

---

## Checklist de `TicketRequest.java`

- ☐ Tiene el campo `createdByEmail` con `@NotBlank` y `@Email` — requerido para POST
- ☐ **No** tiene `assignedToId` ni `createdById` — la vinculación es por email, la asignación se hace con PATCH

---

## Checklist de `BadRequestException.java`

- ☐ Existe la clase `BadRequestException` en el paquete `exception`
- ☐ Extiende `RuntimeException`
- ☐ Tiene un constructor `BadRequestException(String message)` que llama `super(message)`

---

## Checklist de `AssignTicketRequest.java`

- ☐ Existe el archivo `AssignTicketRequest.java` en el paquete `dto`
- ☐ Tiene el campo `assignedToEmail` con `@Email` (sin `@NotBlank` — null/vacío desasigna)

---

## Checklist de `TicketResult.java` y `UserResult.java`

- ☐ Existen ambos archivos en el paquete `dto`
- ☐ `UserResult` es un record con campos `id`, `name`, `email`
- ☐ `TicketResult` es un record con campos `id`, `title`, `description`, `status`, `createdAt`, `estimatedResolutionDate`, `effectiveResolutionDate`, `createdBy` (UserResult), `assignedTo` (UserResult)

---

## Checklist de `TicketService.java`

- ☐ El constructor recibe tanto `TicketRepository` como `UserRepository`
- ☐ `create()` busca el usuario por email con `userRepository.findByEmail()` (requerido)
- ☐ `create()` lanza `BadRequestException` (400) si el email no existe en el sistema
- ☐ `create()` lanza `IllegalArgumentException` (409) si el título ya existe
- ☐ `create()` **no** asigna `assignedTo` — eso se hace exclusivamente con `assignTicket()`
- ☐ `assignTicket()` asigna o desasigna un usuario a un ticket por email
- ☐ `assignTicket()` lanza `BadRequestException` si el email no existe
- ☐ `assignTicket()` retorna `Optional.empty()` si el ticket no existe → controlador responde 404
- ☐ `updateById()` actualiza título, descripción y estado, pero **no** modifica `createdBy` ni `assignedTo`
- ☐ Existe un método privado `toResult(Ticket)` que convierte la entidad en `TicketResult` con `UserResult` anidado

---

## Checklist de `TicketController.java`

- ☐ `POST /tickets` captura tanto `IllegalArgumentException` (409) como `BadRequestException` (400)
- ☐ `PATCH /tickets/{id}` acepta `AssignTicketRequest` con `@Valid`
- ☐ `PATCH /tickets/{id}` retorna 400 si email inválido, 404 si ticket no existe, 200 si OK

---

## Checklist de pruebas

- ☐ `POST /users` → crea usuario, `201 Created` con el objeto `User` incluyendo `id`
- ☐ `POST /users` con email duplicado → `409 Conflict`
- ☐ `POST /users` con email inválido → `400 Bad Request`
- ☐ `POST /tickets` con `createdByEmail` válido → ticket creado con el objeto `createdBy` anidado
- ☐ `POST /tickets` con `createdByEmail` inexistente → `400 Bad Request`
- ☐ `POST /tickets` sin `createdByEmail` → `400 Bad Request` (campo requerido)
- ☐ `PATCH /tickets/{id}` con `assignedToEmail` válido → ticket actualizado con `assignedTo` anidado
- ☐ `PATCH /tickets/{id}` con `assignedToEmail` vacío → `assignedTo` queda `null`
- ☐ `PATCH /tickets/{id}` con `assignedToEmail` inexistente → `400 Bad Request`
- ☐ `PATCH /tickets/{id}` con id que no existe → `404 Not Found`
- ☐ En la base de datos, las columnas `created_by_id` y `assigned_to_id` tienen los IDs correctos
- ☐ La tabla `users` existe en phpMyAdmin / Supabase con las columnas correctas

---

## Errores comunes

| Error | Causa probable | Solución |
|---|---|---|
| `StackOverflowError` al hacer `GET /tickets` | Se está retornando el entity directamente en vez de un DTO | Verificar que el Service retorne `TicketResult` / `UserResult`, no el entity |
| `could not initialize proxy` | Objeto LAZY accedido fuera de sesión JPA | Asegurarse de acceder a los datos dentro de la transacción del servicio |
| `Column 'created_by_id' cannot be null` | La columna tiene `nullable = false` pero se pasa null | Cambiar `@JoinColumn(name=..., nullable = false)` a `nullable = true` (la FK es opcional) |
| `Table 'users' doesn't exist` | `ddl-auto` no creó la tabla | Verificar que `User` tiene `@Entity` y reiniciar con `ddl-auto: update` |





<!-- START OF FILE: docs_lessons_12-relations_05_actividad_individual.md -->
# Documento: docs lessons 12-relations 05 actividad individual
---
# Lección 12 — Actividad Personal: Extender con Category

## ¿Qué es esta actividad?

Esta es una **ACTIVIDAD PERSONAL** que complementa el tutorial base.

En `02_guion_paso_a_paso.md` cubrimos:
- ✓ User (Entity, DTO, Repository, Service, Controller)
- ✓ Ticket (relaciones @ManyToOne a User, búsqueda por email)
- ✓ User con @OneToMany (Paso 9)
- ✓ DTOs de respuesta TicketResult / UserResult (Paso 8.7)
- ✓ Asignación con PATCH /tickets/{id} (Paso 8.8)

**Category** no tiene guion paso-a-paso. En su lugar, tienes directrices para diseñarla e implementarla autónomamente.

Esto es tu oportunidad de practicar el patrón completo:
Entity → DTO → Repository → Service → Controller

---

## Directrices: Implementa Category Autónomamente

Sigue el mismo patrón que `User` del tutorial:

### 1. Crear la entidad `Category`

En `src/main/java/cl/duoc/fullstack/tickets/model/Category.java`:

- `@Entity` y `@Table(name = "categories")`
- Campo `id` con `@Id` y `@GeneratedValue`
- Campo `name` con `@Column(nullable = false, unique = true, length = 100)` y `@NotBlank`
- Campo `description` con `@Column(columnDefinition = "TEXT")` y `@NotBlank`
- Anotaciones Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`

### 2. Crear `CategoryRequest` DTO

En `src/main/java/cl/duoc/fullstack/tickets/dto/CategoryRequest.java`:

- Campos: `name`, `description`
- Validaciones: `@NotBlank` en ambos
- Lombok: `@Getter`, `@Setter`

### 3. Crear `CategoryRepository`

En `src/main/java/cl/duoc/fullstack/tickets/respository/CategoryRepository.java`:

- Extiende `JpaRepository<Category, Long>`
- Métodos útiles: `existsByName()`, `findByName()`

### 4. Crear `CategoryService`

En `src/main/java/cl/duoc/fullstack/tickets/service/CategoryService.java`:

- `getAll()` lista todas
- `create(CategoryRequest)` valida duplicado por name
- `getById(Long id)` retorna Optional
- Excepciones: `IllegalArgumentException` si name duplicado

### 5. Crear `CategoryController`

En `src/main/java/cl/duoc/fullstack/tickets/controller/CategoryController.java`:

- `@RestController` en `/categories`
- `GET /categories` lista
- `POST /categories` crea con `@Valid`, `201 Created` o `409 Conflict`
- `GET /categories/{id}` por id

### 6. Agregar @ManyToOne a Ticket

En `Ticket.java`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id")
private Category category;
```

### 7. Agregar `categoryId` a `TicketRequest`

```java
private Long categoryId;  // opcional
```

### 8. Actualizar `TicketService`

En el método `create()`, resuelve la categoría si se proporciona `categoryId`
(análogo a `createdByEmail` — busca en repositorio y lanza excepción si no existe).

### 9. Pruebas

- POST /categories (crear categorías)
- GET /categories (listar)
- POST /tickets con categoryId válido
- GET /tickets (verificar vinculación)

### 10. Desafío Opcional

Implementa filtro por categoría:

```
GET /tickets?categoryId=1
```

Agrega a `TicketRepository`:
```java
List<Ticket> findByCategoryId(Long categoryId);
```

Agrega a `TicketController`:
```java
@GetMapping
public List<Ticket> list(@RequestParam(required = false) Long categoryId) {
  if (categoryId != null) {
    return ticketService.findByCategory(categoryId);
  }
  return ticketService.getAll();
}
```



