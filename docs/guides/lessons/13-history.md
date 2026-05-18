<!-- START OF FILE: docs_lessons_13-history_01_objetivo_y_alcance.md -->
# Documento: docs lessons 13-history 01 objetivo y alcance
---
# Lección 13 — Tabla de historial: @OneToMany y registro automático de cambios

## ¿De dónde venimos?

Tu aplicación ahora:

- Persiste tickets en base de datos real (L10)
- Conecta a MySQL local o Supabase en la nube (L11)
- Relaciona tickets con usuarios creadores y asignados (L12)

Pero hay un vacío importante: **cuando el estado de un ticket cambia, esa información se pierde**. Si un ticket pasa de `NEW` a `IN_PROGRESS` y luego a `RESOLVED`, no hay registro de cuándo ocurrió cada cambio ni quién lo hizo.

En soporte técnico, esa trazabilidad es fundamental: permite auditar tiempos de respuesta, identificar cuellos de botella y cumplir con acuerdos de nivel de servicio (SLA).

---

## ¿Qué vas a construir?

Al terminar esta lección tendrás:

1. Una nueva entidad `TicketHistory` que registra cada cambio de estado Y de asignado de un ticket
2. La relación `@OneToMany` en `Ticket` → `TicketHistory`
3. Un DTO de respuesta `TicketHistoryResult` para exponer el historial sin exponer la entidad directamente
4. El `TicketService` actualizado para registrar historial automáticamente en `updateById()` y `assignTicket()`
5. Un endpoint `GET /tickets/by-id/{id}/history` en `TicketController` (no hay `TicketHistoryController`)

### Lo que vas a poder explicar

- ¿Qué hace `@OneToMany(mappedBy = "...", cascade = CascadeType.ALL)` en `Ticket`?
- ¿Qué hace `@ManyToOne` en `TicketHistory` apuntando a `Ticket`?
- ¿Qué significa `CascadeType.ALL` y cuándo usarlo?
- ¿Por qué el historial nunca se debe borrar?
- ¿Cómo registra el Service el historial sin que el Controller lo sepa?
- ¿Por qué `TicketHistory` no tiene un controller propio? ¿Qué es una entidad débil?
- ¿Por qué el historial de asignado guarda el email como String y no como FK a User?
- ¿Por qué no se necesita `@JsonIgnore` en las entities de historial?

---

## Nuevo requerimiento

| Requerimiento | Descripción |
|---|---|
| **REQ-18** | El sistema debe registrar automáticamente un historial de cambios de cada ticket: cambios de estado (con el estado anterior y el nuevo) y cambios de asignado (con el email del asignado anterior y el nuevo), incluyendo la fecha y hora de cada cambio |

---

## La estructura que tienes al comenzar

```
src/main/java/cl/duoc/fullstack/tickets/
├── model/
│   ├── Ticket.java
│   └── User.java
├── respository/
│   ├── TicketRepository.java
│   └── UserRepository.java
├── service/
│   ├── TicketService.java
│   └── UserService.java
└── controller/
    ├── TicketController.java
    └── UserController.java
```

La estructura al terminar:

```
src/main/java/cl/duoc/fullstack/tickets/
├── model/
│   ├── Ticket.java              ← con @OneToMany a TicketHistory
│   ├── User.java
│   └── TicketHistory.java       ← nueva entidad
├── dto/
│   ├── TicketRequest.java
│   ├── TicketCommand.java
│   ├── TicketResult.java
│   ├── TicketResponse.java
│   ├── UserRequest.java
│   ├── UserResult.java
│   ├── AssignTicketRequest.java
│   └── TicketHistoryResult.java  ← nuevo DTO de respuesta
├── respository/
│   ├── TicketRepository.java
│   ├── UserRepository.java
│   └── TicketHistoryRepository.java   ← nuevo
├── service/
│   ├── TicketService.java       ← registra historial en updateById() y assignTicket()
│   └── UserService.java
└── controller/
    ├── TicketController.java    ← nuevo endpoint GET /by-id/{id}/history
    └── UserController.java
```

---

## ¿Qué NO cubre esta lección?

| Tema | ¿Por qué queda afuera? |
|---|---|
| `@CreatedDate`, `@LastModifiedDate` (Spring Data Auditing) | Configuración adicional; el patrón manual es más claro para aprender |
| Quién realizó el cambio (`changedByEmail`) | Lo verás en la actividad individual |
| Notificaciones al cambiar estado | Fuera del alcance del curso |
| Paginación del historial | Requiere `Pageable`; el historial por ticket es pequeño en este contexto |




<!-- START OF FILE: docs_lessons_13-history_02_guion_paso_a_paso.md -->
# Documento: docs lessons 13-history 02 guion paso a paso
---
# Lección 13 — Tutorial paso a paso: tabla de historial

---

## Paso 1: crear la entidad `TicketHistory`

Crea `src/main/java/cl/duoc/fullstack/tickets/model/TicketHistory.java`:

```java
package cl.duoc.fullstack.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private Ticket ticket;

  @Column(name = "previous_status", length = 20)
  private String previousStatus;

  @Column(name = "new_status", length = 20)
  private String newStatus;

  @Column(name = "previous_assigned_email", length = 150)
  private String previousAssignedEmail;

  @Column(name = "new_assigned_email", length = 150)
  private String newAssignedEmail;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;

  @Column(length = 255)
  private String comment;
}
```

**Notas importantes:**

- `previousStatus` y `newStatus` registran el cambio de estado. Ambos pueden ser `null` si el registro es solo de cambio de asignado.
- `previousAssignedEmail` y `newAssignedEmail` registran el email del asignado antes y después del cambio. Son `String`, no FK a `User`.
- `comment` es opcional — permite agregar una nota al cambio.
- **No hay `@JsonIgnore`**: esta entity nunca se expone directamente al cliente. El Service la convierte a `TicketHistoryResult` antes de retornar, por lo que Jackson no la serializa y no hay riesgo de bucle circular.

> **¿Por qué el asignado se guarda como email y no como FK a User?**
> El historial es un **log inmutable** que registra un snapshot del estado en el momento del cambio. Si usáramos FK a User y ese usuario fuera eliminado o modificado en el futuro, el historial quedaría inconsistente. El email es el dato identitario que existía en ese momento — autosuficiente y no referencial.

---

## Paso 2: agregar la relación `@OneToMany` en `Ticket`

Abre `Ticket.java` y agrega al final del cuerpo de la clase:

```java
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

// ... dentro de la clase Ticket:

  @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = false)
  private List<TicketHistory> history = new ArrayList<>();
```

> **¿Qué hace `mappedBy = "ticket"`?**
> Le dice a JPA que la clave foránea está en el campo `ticket` de la clase `TicketHistory`. JPA no crea una columna nueva en la tabla `tickets` — la FK ya existe en `ticket_history.ticket_id`.

> **¿Qué hace `cascade = CascadeType.ALL`?**
> Propaga las operaciones de persistencia desde `Ticket` hacia sus `TicketHistory`. Si guardas un `Ticket` que tiene entradas en `history`, JPA también guarda los `TicketHistory` automáticamente.

> **¿Por qué no hay `@JsonIgnore` en la lista `history`?**
> El historial se expone a través del endpoint `GET /tickets/by-id/{id}/history`, que retorna `List<TicketHistoryResult>` (un DTO record). Jackson nunca serializa el `Ticket` ni sus colecciones directamente — el Service convierte todo a DTOs antes de retornar al controller.

> **¿Por qué `orphanRemoval = false`?**
> El historial nunca debe borrarse, aunque se borre el ticket. Lo dejamos en `false` para que los registros históricos persistan incluso si el ticket es eliminado. (En producción se usaría borrado lógico, pero eso está fuera del alcance del curso.)

---

## Paso 3: crear `TicketHistoryRepository`

Crea `src/main/java/cl/duoc/fullstack/tickets/respository/TicketHistoryRepository.java`:

```java
package cl.duoc.fullstack.tickets.respository;

import cl.duoc.fullstack.tickets.model.TicketHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

  // Devuelve el historial de un ticket ordenado del más reciente al más antiguo
  List<TicketHistory> findByTicketIdOrderByChangedAtDesc(Long ticketId);
}
```

---

## Paso 4: crear el DTO `TicketHistoryResult`

Crea `src/main/java/cl/duoc/fullstack/tickets/dto/TicketHistoryResult.java`:

```java
package cl.duoc.fullstack.tickets.dto;

import java.time.LocalDateTime;

public record TicketHistoryResult(
    Long id,
    String previousStatus,
    String newStatus,
    String previousAssignedEmail,
    String newAssignedEmail,
    LocalDateTime changedAt,
    String comment
) {}
```

Este record es el contrato de respuesta del historial. El Service construye instancias de este record a partir de la entity `TicketHistory`, asegurando que Jackson solo serialice el DTO — nunca la entity.

---

## Paso 5: actualizar `TicketService` para registrar el historial

El historial debe registrarse automáticamente cuando el estado o el asignado cambian. El Controller no se entera — es responsabilidad del Service.

Inyecta `TicketHistoryRepository` en `TicketService`:

```java
@Service
public class TicketService {

  private TicketRepository repository;
  private UserRepository userRepository;
  private TicketHistoryRepository historyRepository;   // ← nuevo

  public TicketService(
      TicketRepository repository,
      UserRepository userRepository,
      TicketHistoryRepository historyRepository) {
    this.repository = repository;
    this.userRepository = userRepository;
    this.historyRepository = historyRepository;
  }
```

Agrega un método privado `recordChange` y otro de conversión a DTO:

```java
  private void recordChange(
      Ticket ticket,
      String previousStatus,
      String newStatus,
      String previousAssignedEmail,
      String newAssignedEmail,
      String comment) {

    boolean statusChanged = newStatus != null
        && !newStatus.equalsIgnoreCase(previousStatus == null ? "" : previousStatus);
    boolean assigneeChanged = !java.util.Objects.equals(previousAssignedEmail, newAssignedEmail);

    if (!statusChanged && !assigneeChanged) {
      return; // no hay cambio real → no registrar
    }

    TicketHistory entry = new TicketHistory();
    entry.setTicket(ticket);
    entry.setPreviousStatus(statusChanged ? previousStatus : null);
    entry.setNewStatus(statusChanged ? newStatus : null);
    entry.setPreviousAssignedEmail(assigneeChanged ? previousAssignedEmail : null);
    entry.setNewAssignedEmail(assigneeChanged ? newAssignedEmail : null);
    entry.setChangedAt(LocalDateTime.now());
    entry.setComment(comment);
    historyRepository.save(entry);
  }

  private TicketHistoryResult toHistoryResult(TicketHistory h) {
    return new TicketHistoryResult(
        h.getId(),
        h.getPreviousStatus(),
        h.getNewStatus(),
        h.getPreviousAssignedEmail(),
        h.getNewAssignedEmail(),
        h.getChangedAt(),
        h.getComment()
    );
  }
```

Actualiza `create()` para registrar el historial de creación:

```java
  public TicketResult create(TicketCommand command) {
    if (repository.existsByTitle(command.title())) {
      throw new IllegalArgumentException(
          "Ya existe un ticket con el título '" + command.title() + "'");
    }

    User createdBy = userRepository.findByEmail(command.createdByEmail())
        .orElseThrow(() -> new BadRequestException(
            "No existe un usuario con el email '" + command.createdByEmail() + "'"));

    Ticket ticket = new Ticket();
    ticket.setTitle(command.title());
    ticket.setDescription(command.description());
    ticket.setStatus("NEW");
    ticket.setCreatedAt(LocalDateTime.now());
    ticket.setEstimatedResolutionDate(LocalDate.now().plusDays(5));
    ticket.setCreatedBy(createdBy);

    Ticket saved = repository.save(ticket);

    // Registrar historial: el ticket nació en estado NEW (sin estado anterior)
    recordChange(saved, null, "NEW", null, null, "Ticket creado");

    return toResult(saved);
  }
```

Actualiza `updateById()` para registrar el historial cuando cambia el estado:

```java
  public TicketResult updateById(Long id, TicketCommand command) {
    Ticket ticket = repository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Ticket con id " + id + " no existe"));

    // Capturar valores anteriores para el historial
    String previousStatus = ticket.getStatus();
    String previousAssignedEmail = ticket.getAssignedTo() != null
        ? ticket.getAssignedTo().getEmail()
        : null;

    ticket.setTitle(command.title());
    ticket.setDescription(command.description());

    if (command.status() != null && !command.status().isBlank()) {
      ticket.setStatus(command.status());
    }

    Ticket saved = repository.save(ticket);

    recordChange(saved, previousStatus, saved.getStatus(), previousAssignedEmail, previousAssignedEmail, null);

    return toResult(saved);
  }
```

Actualiza `assignTicket()` para registrar el cambio de asignado:

```java
  public Optional<TicketResult> assignTicket(Long id, AssignTicketRequest request) {
    if (!repository.existsById(id)) {
      return Optional.empty();
    }

    Ticket ticket = repository.findById(id).orElseThrow();

    String previousAssignedEmail = ticket.getAssignedTo() != null
        ? ticket.getAssignedTo().getEmail()
        : null;
    String newAssignedEmail;

    if (request.getAssignedToEmail() == null || request.getAssignedToEmail().isBlank()) {
      ticket.setAssignedTo(null);
      newAssignedEmail = null;
    } else {
      User assignee = userRepository.findByEmail(request.getAssignedToEmail())
          .orElseThrow(() -> new BadRequestException(
              "No existe un usuario con el email '" + request.getAssignedToEmail() + "'"));
      ticket.setAssignedTo(assignee);
      newAssignedEmail = assignee.getEmail();
    }

    Ticket saved = repository.save(ticket);

    recordChange(saved, null, null, previousAssignedEmail, newAssignedEmail, null);

    return Optional.of(toResult(saved));
  }
```

Agrega el método `getHistory()` que el Controller usará:

```java
  public Optional<List<TicketHistoryResult>> getHistory(Long ticketId) {
    if (!repository.existsById(ticketId)) {
      return Optional.empty();
    }
    List<TicketHistoryResult> historial = historyRepository
        .findByTicketIdOrderByChangedAtDesc(ticketId)
        .stream()
        .map(this::toHistoryResult)
        .toList();
    return Optional.of(historial);
  }
```

> **¿Por qué el Controller no sabe que se está registrando historial?**
> `updateById()` y `assignTicket()` registran el historial internamente. Esto aplica el principio de **responsabilidad única**: el Service es dueño de la lógica de negocio, incluida la auditoría. El Controller solo orquesta la petición HTTP.

---

## Paso 6: agregar el endpoint de historial en `TicketController`

Agrega en `TicketController`:

```java
import cl.duoc.fullstack.tickets.dto.TicketHistoryResult;
import java.util.List;

// El constructor NO cambia — TicketController solo inyecta TicketService:
public TicketController(TicketService service) {
  this.service = service;
}

// Nuevo endpoint:
@GetMapping("/by-id/{id}/history")
public ResponseEntity<List<TicketHistoryResult>> getHistory(@PathVariable Long id) {
  return service.getHistory(id)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
}
```

> **¿Por qué el Controller no inyecta `TicketHistoryRepository`?**
> La arquitectura de 5 capas establece que el Controller nunca habla directamente con el Repository. La consulta del historial es responsabilidad del Service, que también se encarga de convertir las entities a DTOs.

> **¿Por qué la URL es `/by-id/{id}/history` y no `/{id}/history`?**
> Para ser coherente con el patrón existente en el mismo Controller: `GET /tickets/by-id/{id}`, `PUT /tickets/by-id/{id}`, etc. El historial es un subrecurso del ticket, representado como `/by-id/{id}/history`.

> **¿Por qué no hay `TicketHistoryController`?**
> `TicketHistory` es una **entidad débil** (weak entity): no puede existir ni tiene sentido sin su Ticket padre. Acceder al historial siempre requiere el ID del ticket. No hay caso de uso donde se consulte el historial sin saber a qué ticket pertenece, por lo que un controller dedicado añadiría complejidad sin valor.

---

## Paso 7: probar el flujo completo

### Crear un ticket

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{ "title": "Red caída en piso 3", "description": "Sin internet desde las 9am", "createdByEmail": "admin@empresa.cl" }
```

Respuesta: `201 Created`, ticket con `id: 1` en estado `NEW`.

### Consultar historial inicial

```
GET http://localhost:8080/ticket-app/tickets/by-id/1/history
```

Respuesta esperada:
```json
[
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

### Cambiar el estado del ticket

```
PUT http://localhost:8080/ticket-app/tickets/by-id/1
Content-Type: application/json

{
  "title": "Red caída en piso 3",
  "description": "Sin internet desde las 9am",
  "status": "IN_PROGRESS",
  "createdByEmail": "admin@empresa.cl"
}
```

### Asignar el ticket a un usuario

```
PATCH http://localhost:8080/ticket-app/tickets/by-id/1/assign
Content-Type: application/json

{ "assignedToEmail": "soporte@empresa.cl" }
```

### Consultar historial actualizado

```
GET http://localhost:8080/ticket-app/tickets/by-id/1/history
```

Respuesta esperada:
```json
[
  {
    "id": 3,
    "previousStatus": null,
    "newStatus": null,
    "previousAssignedEmail": null,
    "newAssignedEmail": "soporte@empresa.cl",
    "changedAt": "2026-04-15T10:40:00",
    "comment": null
  },
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

### Verificar en la base de datos

En phpMyAdmin o Supabase, la tabla `ticket_history` debe mostrar todos los registros con los estados, emails y fechas correctos.

---

## Paso 8: reflexiona antes de cerrar

1. ¿Qué pasaría si `orphanRemoval = true`? ¿El historial se borraría si se borra el ticket?
2. ¿Por qué el `Controller` no sabe que se está registrando historial cuando llama a `updateById()`?
3. Si el mismo estado se envía dos veces (`NEW` → `NEW`) y el asignado no cambia, ¿se crea un registro de historial? ¿Por qué?
4. ¿Por qué el email del asignado es más adecuado que el ID de usuario para un log de auditoría?
5. Si `TicketHistory` tuviera su propio Controller, ¿qué problemas aparecerían? ¿Podría alguien crear un historial falso via POST?




<!-- START OF FILE: docs_lessons_13-history_03_historial_y_auditoria.md -->
# Documento: docs lessons 13-history 03 historial y auditoria
---
# Lección 13 — Historial, auditoría y CascadeType

## ¿Por qué un historial en tabla separada?

Una alternativa ingenua sería guardar el historial como texto en el propio ticket:

```java
// Opción mala: historial como texto en el ticket
private String historial = "NEW → IN_PROGRESS (15/04/2026)\nIN_PROGRESS → RESOLVED (16/04/2026)";
```

Esto parece simple, pero tiene varios problemas:

| Problema | Consecuencia |
|---|---|
| No se puede consultar por fecha o estado | Imposible filtrar "todos los cambios de hoy" |
| No se puede indexar | Búsquedas lentas |
| No tiene estructura | Difícil de parsear desde el cliente |
| Crece sin límite | La columna se hace enorme |

Una tabla de historial resuelve todo esto: cada cambio es una **fila independiente** con sus propios campos indexables.

---

## El patrón de auditoría

El patrón que usaste en esta lección se llama **tabla de auditoría** o **tabla de historial**:

```
Acción en la entidad principal    →    Se registra en la tabla de historial
──────────────────────────────         ──────────────────────────────────────
Crear ticket (estado = NEW)       →    { previous: null, new: "NEW", fecha: ahora }
Actualizar estado a IN_PROGRESS   →    { previous: "NEW", new: "IN_PROGRESS", fecha: ahora }
Actualizar estado a RESOLVED      →    { previous: "IN_PROGRESS", new: "RESOLVED", fecha: ahora }
```

La regla fundamental de una tabla de auditoría es: **nunca se borran sus registros**. Es un log inmutable del ciclo de vida de la entidad.

---

## `CascadeType` — qué operaciones se propagan

`CascadeType` define qué operaciones de JPA sobre la entidad padre se propagan automáticamente a las entidades hijas:

```java
@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
private List<TicketHistory> history = new ArrayList<>();
```

| Valor | Qué propaga |
|---|---|
| `PERSIST` | Si guardas el ticket, también guarda los `TicketHistory` en la lista |
| `MERGE` | Si actualizas el ticket con `merge()`, también actualiza los hijos |
| `REMOVE` | Si borras el ticket, también borra todos sus `TicketHistory` |
| `REFRESH` | Si recargas el ticket desde la BD, también recarga los hijos |
| `ALL` | Todos los anteriores combinados |

> **¿Por qué en este caso `CascadeType.ALL` puede ser riesgoso?**
> Porque incluye `REMOVE`: si alguien borra un ticket, todos sus registros históricos también se borran. En un sistema de auditoría real, eso no es aceptable.
>
> Para el contexto del curso (sin borrado de tickets en producción) es aceptable. Si quisieras proteger el historial, usarías solo `CascadeType.PERSIST` en lugar de `ALL`.

---

## `orphanRemoval` — qué pasa con los huérfanos

```java
@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = false)
private List<TicketHistory> history = new ArrayList<>();
```

`orphanRemoval = true` elimina un `TicketHistory` si lo remueves de la lista `history` del `Ticket`. Como el historial no debe borrarse nunca, lo dejamos en `false`.

| Valor | Comportamiento |
|---|---|
| `orphanRemoval = true` | Si sacas un elemento de la lista, JPA lo borra de la BD |
| `orphanRemoval = false` | Si sacas un elemento de la lista, JPA no lo borra de la BD |

---

## La relación completa vista desde ambos lados

```
Ticket (id=1, status="RESOLVED", assignedTo=soporte@empresa.cl)
│
├── TicketHistory (id=1, prevStatus=null,         newStatus="NEW",        prevEmail=null,  newEmail=null,                changedAt=10:30)
├── TicketHistory (id=2, prevStatus="NEW",        newStatus="IN_PROGRESS",prevEmail=null,  newEmail=null,                changedAt=10:35)
├── TicketHistory (id=3, prevStatus=null,         newStatus=null,         prevEmail=null,  newEmail="soporte@empresa.cl",changedAt=10:40)
└── TicketHistory (id=4, prevStatus="IN_PROGRESS",newStatus="RESOLVED",  prevEmail=null,  newEmail=null,                changedAt=11:00)
```

En la base de datos:

```
tabla tickets:
id | title              | status
1  | Red caída piso 3   | RESOLVED

tabla ticket_history:
id | ticket_id | previous_status | new_status   | previous_assigned_email | new_assigned_email  | changed_at
1  | 1         | NULL            | NEW          | NULL                    | NULL                | 2026-04-15 10:30:00
2  | 1         | NEW             | IN_PROGRESS  | NULL                    | NULL                | 2026-04-15 10:35:00
3  | 1         | NULL            | NULL         | NULL                    | soporte@empresa.cl  | 2026-04-15 10:40:00
4  | 1         | IN_PROGRESS     | RESOLVED     | NULL                    | NULL                | 2026-04-15 11:00:00
```

La tabla `tickets` solo guarda el **estado actual**. La tabla `ticket_history` guarda **toda la evolución**.

---

## El flujo completo de un cambio de estado

```
[Cliente]
  │ PUT /tickets/1  {"status": "IN_PROGRESS"}
  ↓
[TicketController.updateById(id=1, request)]
  │ Llama a service.updateById(1, request)
  │ No sabe nada sobre historial
  ↓
[TicketService.updateById(id=1, request)]
  │ Carga el ticket de la BD → status actual = "NEW"
  │ request.getStatus() = "IN_PROGRESS" ≠ "NEW" → hay cambio
  │ Actualiza ticket.status = "IN_PROGRESS"
  │ Llama a recordChange(ticket, "NEW", "IN_PROGRESS", null, null, null)
  │   └─ crea TicketHistory y llama a historyRepository.save(entry)
  │ Llama a repository.save(ticket) → persiste el nuevo estado
  ↓
[Cliente]
  │ 200 OK con TicketResult (DTO)
```

El Controller es ajeno al historial. Toda la lógica de auditoría vive en el Service. Este es el principio de **responsabilidad única** aplicado a la capa de servicio.

El mismo principio aplica para el endpoint de historial: `TicketController` llama a `service.getHistory(id)`, que retorna `List<TicketHistoryResult>`. El Controller nunca toca el `TicketHistoryRepository` directamente.

---

## ¿Por qué no necesitamos `@JsonIgnore`?

En otros tutoriales verás `@JsonIgnore` en el campo `ticket` de `TicketHistory` y en la lista `history` de `Ticket`. Esto se hace para evitar el bucle de serialización:

```
Jackson serializa TicketHistory
  → intenta serializar Ticket
    → intenta serializar cada TicketHistory de la lista
      → intenta serializar Ticket (de nuevo)
        → bucle infinito → StackOverflowError
```

En este proyecto **no usamos `@JsonIgnore`** porque el Service nunca retorna entities directamente:

```
[TicketController] llama a service.getHistory(id)
     ↓
[TicketService] consulta el repository → obtiene List<TicketHistory>
     │ convierte cada entrada a TicketHistoryResult (record)
     ↓
[TicketController] retorna List<TicketHistoryResult>
     ↓
[Jackson] serializa TicketHistoryResult — un record plano, sin referencias circulares
```

`TicketHistoryResult` es un record con campos simples (Strings, LocalDateTime). Jackson lo serializa sin problema. La entity `TicketHistory` nunca llega a Jackson.

---

## ¿Por qué email y no FK a `User` para registrar el asignado?

Es tentador guardar el asignado como FK:

```java
// Tentación: FK a User
@ManyToOne
private User previousAssignedTo;

// Lo que usamos: email como String
private String previousAssignedEmail;
```

La tabla de historial es un **log inmutable**. Cada fila es un snapshot del estado en un momento dado. Hay varias razones para preferir el email:

| Razón | Explicación |
|---|---|
| **Independencia referencial** | Si el User se elimina, el historial no queda huérfano ni se borra en cascada |
| **Inmutabilidad real** | El email capturado en el momento del cambio no cambia aunque el usuario actualice su perfil |
| **Lectura directa** | En un log de auditoría, quieres ver el dato directamente, no hacer JOIN a otra tabla |
| **Coherencia con L12** | La asignación ya se hace por email en `AssignTicketRequest` — es natural mantener ese identificador en el historial |

Este patrón es común en sistemas de auditoría y Event Sourcing: guardar el valor en el momento del evento, no una referencia al objeto actual.

---

## ¿Por qué no hay `TicketHistoryController`?

`TicketHistory` es una **entidad débil** (weak entity): no tiene identidad propia ni puede existir sin un `Ticket` padre. Sus características:

- No tiene significado fuera del contexto de un Ticket
- Siempre se consulta en relación a un Ticket específico: "dame el historial del ticket N"
- No tiene operaciones de creación, actualización o borrado propias — su ciclo de vida está 100% gestionado por `TicketService`

Por eso el endpoint de historial vive en `TicketController`:

```
GET /tickets/by-id/{id}/history
```

Si tuviéramos un `TicketHistoryController` con `POST /ticket-history`, alguien podría insertar entradas falsas en el historial — violando la integridad del log de auditoría. Al no tener controller propio, la única forma de escribir en el historial es a través de las operaciones de negocio del Service.




<!-- START OF FILE: docs_lessons_13-history_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 13-history 04 checklist rubrica minima
---
# Lección 13 — Checklist y rúbrica mínima

---

## Checklist de `TicketHistory.java`

- ☐ La clase tiene `@Entity` y `@Table(name = "ticket_history")`
- ☐ El campo `id` tiene `@Id` y `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- ☐ El campo `ticket` tiene `@ManyToOne(fetch = FetchType.LAZY)` y `@JoinColumn(name = "ticket_id", nullable = false)` — **sin** `@JsonIgnore`
- ☐ El campo `previousStatus` tiene `@Column(name = "previous_status")` y **no** tiene `nullable = false` (puede ser null)
- ☐ El campo `newStatus` tiene `@Column(name = "new_status")` y **no** tiene `nullable = false` (puede ser null si el registro es solo de cambio de asignado)
- ☐ El campo `previousAssignedEmail` es `String` con `@Column(name = "previous_assigned_email")` — **no** es FK a User
- ☐ El campo `newAssignedEmail` es `String` con `@Column(name = "new_assigned_email")` — **no** es FK a User
- ☐ El campo `changedAt` tiene `@Column(name = "changed_at", nullable = false)`
- ☐ La clase tiene `@NoArgsConstructor` (requerido por JPA) y `@AllArgsConstructor`
- ☐ **No hay** `@JsonIgnore` ni imports de `com.fasterxml.jackson` en la clase

---

## Checklist de `Ticket.java` (relación OneToMany)

- ☐ Tiene el campo `history` con `@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = false)`
- ☐ El campo `history` se inicializa con `new ArrayList<>()` (nunca debe ser null)
- ☐ El valor `mappedBy = "ticket"` coincide exactamente con el nombre del campo en `TicketHistory`
- ☐ **No hay** `@JsonIgnore` en el campo `history`

---

## Checklist de `TicketHistoryResult.java`

- ☐ Es un Java `record` en el paquete `dto/`
- ☐ Tiene los campos: `Long id`, `String previousStatus`, `String newStatus`, `String previousAssignedEmail`, `String newAssignedEmail`, `LocalDateTime changedAt`, `String comment`
- ☐ **No tiene** anotaciones JPA ni Jackson

---

## Checklist de `TicketHistoryRepository.java`

- ☐ Es una interfaz que extiende `JpaRepository<TicketHistory, Long>`
- ☐ Tiene el método `List<TicketHistory> findByTicketIdOrderByChangedAtDesc(Long ticketId)`
- ☐ Está en el paquete `respository/` (typo intencional)

---

## Checklist de `TicketService.java`

- ☐ El constructor recibe `TicketHistoryRepository` (además de los repositorios de L12)
- ☐ Existe el método privado `recordChange(Ticket, String, String, String, String, String)` que recibe: ticket, previousStatus, newStatus, previousAssignedEmail, newAssignedEmail, comment
- ☐ `recordChange()` solo guarda si hay cambio real (estado diferente O email diferente)
- ☐ `create()` llama a `recordChange()` después de guardar el ticket (con `previousStatus = null`, `newStatus = "NEW"`, emails = null)
- ☐ `updateById()` usa `orElseThrow` (no `.map()`), captura estado anterior, llama a `recordChange()` solo si el estado cambió
- ☐ `assignTicket()` captura email anterior, llama a `recordChange()` con el cambio de email
- ☐ Existe el método `getHistory(Long ticketId)` que retorna `Optional<List<TicketHistoryResult>>` (vacío si el ticket no existe)
- ☐ Existe el método privado `toHistoryResult(TicketHistory)` que convierte entity a DTO

---

## Checklist de `TicketController.java`

- ☐ El constructor **no** inyecta `TicketHistoryRepository` — solo `TicketService`
- ☐ Tiene el endpoint `GET /by-id/{id}/history` que devuelve `ResponseEntity<List<TicketHistoryResult>>`
- ☐ El endpoint llama a `service.getHistory(id)` (no al repository directamente)
- ☐ Si el ticket no existe, el endpoint devuelve `404 Not Found`
- ☐ Si el ticket existe, el endpoint devuelve `200 OK` con la lista (puede estar vacía)
- ☐ La lista viene ordenada de más reciente a más antiguo

---

## Checklist de pruebas

- ☐ Crear un ticket → `GET /tickets/by-id/{id}/history` muestra 1 entrada con `previousStatus: null`, `newStatus: "NEW"`, emails null
- ☐ Actualizar estado a `IN_PROGRESS` → historial muestra 2 entradas; la nueva con `previousStatus: "NEW"`, `newStatus: "IN_PROGRESS"`
- ☐ Asignar ticket a usuario por email → historial muestra nueva entrada con `previousAssignedEmail: null`, `newAssignedEmail: "email@..."`, status fields null
- ☐ Reasignar a otro usuario → historial muestra nueva entrada con `previousAssignedEmail: "anterior@..."`, `newAssignedEmail: "nuevo@..."`
- ☐ Enviar el mismo estado sin cambio real → historial **no** agrega una entrada nueva
- ☐ `GET /tickets/by-id/999/history` → `404 Not Found`
- ☐ En phpMyAdmin / Supabase, la tabla `ticket_history` tiene las filas correctas con los emails como strings

---

## Errores comunes

| Error | Causa probable | Solución |
|---|---|---|
| `StackOverflowError` al hacer `GET /history` | El endpoint retorna `List<TicketHistory>` (entity) en lugar de `List<TicketHistoryResult>` (DTO) | Verificar que `service.getHistory()` retorna DTOs y el controller los retorna directamente |
| Historial crea entrada aunque no hay cambio | `recordChange` no verifica si hay cambio real | Agregar la verificación `!statusChanged && !assigneeChanged → return` |
| `mappedBy` error al arrancar | El valor en `mappedBy` no coincide con el nombre del campo | Verificar que `mappedBy = "ticket"` coincida con `private Ticket ticket` en `TicketHistory` |
| La tabla `ticket_history` no se crea | `TicketHistory` no tiene `@Entity` o no está en el paquete escaneado | Verificar las anotaciones y el paquete de la clase |
| El primer registro de historial falla por `previousStatus NOT NULL` | La columna tiene `nullable = false` | Cambiar a `@Column(name = "previous_status")` sin `nullable = false` |
| Controller retorna 404 para ticket existente | `service.getHistory()` no está implementado correctamente | Verificar que retorna `Optional.of(lista)` (no `Optional.empty()`) cuando el ticket existe aunque no tenga historial |




<!-- START OF FILE: docs_lessons_13-history_05_actividad_individual.md -->
# Documento: docs lessons 13-history 05 actividad individual
---
# Lección 13 — Actividad individual: registrar quién hizo el cambio

## Contexto

Tu sistema ya registra qué cambió (estado y asignado) y cuándo. Esta actividad extiende el historial para registrar también **quién** realizó el cambio: el email del usuario que ejecutó la operación.

En sistemas con autenticación (Spring Security), este dato vendría automáticamente del usuario autenticado en el contexto de la petición. Por ahora, lo recibiremos explícitamente en el cuerpo de la petición.

---

## Parte 1: agregar `changedByEmail` a `TicketHistory`

Agrega este campo a `TicketHistory.java`:

```java
@Column(name = "changed_by_email", length = 150)
private String changedByEmail;
```

- Es `String`, no FK a `User` — mismo razonamiento que `previousAssignedEmail` y `newAssignedEmail`: el historial es inmutable, debe guardar snapshots, no referencias.
- Puede ser `null` si no se proporciona (por compatibilidad retroactiva).

---

## Parte 2: actualizar `TicketHistoryResult`

Agrega el campo al record:

```java
public record TicketHistoryResult(
    Long id,
    String previousStatus,
    String newStatus,
    String previousAssignedEmail,
    String newAssignedEmail,
    String changedByEmail,       // ← nuevo
    LocalDateTime changedAt,
    String comment
) {}
```

---

## Parte 3: actualizar `recordChange` en `TicketService`

Agrega el parámetro `changedByEmail` al método privado:

```java
private void recordChange(
    Ticket ticket,
    String previousStatus,
    String newStatus,
    String previousAssignedEmail,
    String newAssignedEmail,
    String changedByEmail,       // ← nuevo parámetro
    String comment) {

  boolean statusChanged = newStatus != null
      && !newStatus.equalsIgnoreCase(previousStatus == null ? "" : previousStatus);
  boolean assigneeChanged = !java.util.Objects.equals(previousAssignedEmail, newAssignedEmail);

  if (!statusChanged && !assigneeChanged) {
    return;
  }

  TicketHistory entry = new TicketHistory();
  entry.setTicket(ticket);
  entry.setPreviousStatus(statusChanged ? previousStatus : null);
  entry.setNewStatus(statusChanged ? newStatus : null);
  entry.setPreviousAssignedEmail(assigneeChanged ? previousAssignedEmail : null);
  entry.setNewAssignedEmail(assigneeChanged ? newAssignedEmail : null);
  entry.setChangedByEmail(changedByEmail);  // ← nuevo
  entry.setChangedAt(LocalDateTime.now());
  entry.setComment(comment);
  historyRepository.save(entry);
}
```

Actualiza también `toHistoryResult()`:

```java
private TicketHistoryResult toHistoryResult(TicketHistory h) {
  return new TicketHistoryResult(
      h.getId(),
      h.getPreviousStatus(),
      h.getNewStatus(),
      h.getPreviousAssignedEmail(),
      h.getNewAssignedEmail(),
      h.getChangedByEmail(),     // ← nuevo
      h.getChangedAt(),
      h.getComment()
  );
}
```

---

## Parte 4: recibir `changedByEmail` desde el cliente

Para que el cliente pueda informar quién realiza el cambio, agrega el campo a los DTOs de comando:

**`TicketCommand.java`** — agrega el campo:
```java
public record TicketCommand(
    String title,
    String description,
    String status,
    String createdByEmail,
    String changedByEmail       // ← nuevo (puede ser null)
) {}
```

**`AssignTicketRequest.java`** — agrega el campo:
```java
private String changedByEmail;   // ← nuevo (puede ser null)
```

Actualiza `updateById()` y `assignTicket()` en `TicketService` para pasar `changedByEmail` a `recordChange`:

```java
// En updateById():
recordChange(saved, previousStatus, saved.getStatus(),
    previousAssignedEmail, previousAssignedEmail,
    command.changedByEmail(), null);    // ← pasar changedByEmail

// En assignTicket():
recordChange(saved, null, null,
    previousAssignedEmail, newAssignedEmail,
    request.getChangedByEmail(), null); // ← pasar changedByEmail
```

---

## Pruebas requeridas

| Prueba | Resultado esperado |
|---|---|
| Actualizar estado enviando `changedByEmail` | La entrada de historial muestra el email en `changedByEmail` |
| Asignar ticket enviando `changedByEmail` | La entrada de historial muestra el email en `changedByEmail` |
| Actualizar sin enviar `changedByEmail` | La entrada de historial tiene `changedByEmail: null` (no falla) |
| `GET /tickets/by-id/{id}/history` | El campo `changedByEmail` aparece en todas las entradas (puede ser null en las anteriores) |

---

## Criterios de evaluación

| Criterio | Puntaje |
|---|---|
| `TicketHistory` tiene el campo `changedByEmail` como `String` (no FK a User) | 20% |
| `TicketHistoryResult` incluye `changedByEmail` | 15% |
| `recordChange` acepta y registra `changedByEmail` | 25% |
| `updateById()` y `assignTicket()` pasan `changedByEmail` al método de registro | 25% |
| El campo es opcional (`null` cuando no se proporciona) y no provoca errores | 15% |

---

## Reflexión final

Cuando implementes Spring Security en un proyecto real, el `changedByEmail` no vendrá del cuerpo de la petición — vendrá del `SecurityContext`:

```java
String changedByEmail = SecurityContextHolder.getContext()
    .getAuthentication().getName();
```

El cliente ya no necesita enviarlo porque el servidor sabe quién es el usuario autenticado. El diseño con email como parámetro explícito que usaste aquí es la misma arquitectura — solo cambia de dónde viene el dato.


