<!-- START OF FILE: docs_lessons_07-errors_01_objetivo_y_alcance.md -->
# Documento: docs lessons 07-errors 01 objetivo y alcance
---
# Lección 07 - Errores bien hechos: ¿qué vas a aprender?

## ¿De dónde venimos?

En la lección anterior completaste el CRUD de tickets. Tu API ya puede crear, leer, actualizar y eliminar recursos. Pero hay un problema silencioso que se esconde en cada endpoint: los errores no tienen una forma consistente.

Observa lo que ocurre ahora cuando algo falla:

| Situación | Respuesta actual |
|---|---|
| `POST /tickets` con título duplicado | `409 Conflict` + body: `"Ya existe un ticket con el título..."` (texto plano) |
| `GET /tickets/999` (no existe) | `404 Not Found` + **sin body** |
| `PUT /tickets/999` (no existe) | `404 Not Found` + **sin body** |
| `DELETE /tickets/999` (no existe) | `404 Not Found` + **sin body** |

El problema es doble:

1. **Inconsistencia:** el `POST` devuelve algo (aunque sea texto plano), los demás no devuelven nada.
2. **Inutilidad para el cliente:** cuando un cliente recibe un `404` vacío, no sabe qué estaba buscando ni por qué falló. Tiene que adivinar.

Una API profesional tiene un contrato de errores claro. El cliente siempre sabe qué esperar cuando algo sale mal.

---

## ¿Qué vas a construir?

Al terminar esta lección, **todos** los errores de tu API tendrán la misma estructura JSON:

```json
{
  "message": "Ticket con ID 999 no encontrado"
}
```

Concretamente, vas a:

1. Crear una clase `ErrorResponse` que representa esa estructura
2. Actualizar el controlador para que todos los errores devuelvan esta estructura con cuerpo
3. Garantizar que el `Service` lanza excepciones con mensajes claros

### Lo que vas a ser capaz de explicar

Al terminar deberías poder responder:

- ¿Por qué una API devuelve `text/plain` cuando el body es un `String` y `application/json` cuando es un objeto?
- ¿Qué ventaja tiene `{"message": "..."}` sobre devolver un `String` directamente?
- ¿Por qué un `404` sin body es problemático para el cliente que consume la API?
- ¿Qué es un "contrato de errores" y por qué importa?
- ¿En qué se diferencia manejar errores localmente (try/catch por método) del manejo global (`@ControllerAdvice`)?

---

## ¿Qué requerimientos implementamos en esta lección?

> El proyecto completo está descrito en [`00_enunciado_proyecto.md`](../00_enunciado_proyecto.md).

| Requerimiento | Lo que construimos |
|---|---|
| **REQ-11** — Error con cuerpo JSON `{"message":"..."}` | La clase `ErrorResponse` + actualización de todos los endpoints para usarla |
| **REQ-12** — El creador y asignado no pueden ser el mismo usuario | Validación en `create()` y `updateById()` del Service |

---

## ¿Qué NO cubre esta lección? (y por qué)

| Tema | ¿Por qué lo dejamos después? |
|---|---|
| `@ControllerAdvice` | Requiere comprender el ciclo de errores de Spring; lo presentamos como debate hoy sin implementarlo |
| Validación de entrada (`@NotBlank`, `@Valid`) | Es una capa adicional; primero consolidamos la estructura de errores |
| Jerarquía de excepciones propias | `IllegalArgumentException` es suficiente por ahora; las excepciones de dominio llegan con más contexto |
| Códigos de error de la base de datos | Aún trabajamos en memoria |

El foco de esta lección es uno solo: **que todos los errores hablen el mismo idioma JSON**.

---

## La estructura que tienes al comenzar

```
src/main/java/cl/duoc/fullstack/tickets/
├── controller/
│   └── TicketController.java   ← CRUD completo, errores inconsistentes
├── model/
│   └── Ticket.java
├── respository/
│   └── TicketRepository.java   ← List-based, findById/update/delete con Optional
├── service/
│   └── TicketService.java      ← create() lanza IllegalArgumentException
└── TicketsApplication.java
```

Y la estructura que tendrás al terminar:

```
src/main/java/cl/duoc/fullstack/tickets/
├── controller/
│   └── TicketController.java   ← todos los errores devuelven ErrorResponse
├── model/
│   ├── Ticket.java
│   └── ErrorResponse.java      ← nueva: estructura estándar de error
├── respository/
│   └── TicketRepository.java   (sin cambios)
├── service/
│   └── TicketService.java      (sin cambios)
└── TicketsApplication.java
```






<!-- START OF FILE: docs_lessons_07-errors_02_guion_paso_a_paso.md -->
# Documento: docs lessons 07-errors 02 guion paso a paso
---
# Lección 07 - Tutorial paso a paso: validaciones y errores

Sigue esta guía en orden. Vas a agregar validaciones de negocio en `TicketService` y manejo de excepciones en `TicketController`.

---

## Paso 0: agregar la dependencia de validación

Para usar `@NotBlank`, `@Valid` y otras anotaciones de Bean Validation necesitas la dependencia en `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Paso 1: agregar campos `createdBy` y `assignedTo` al modelo

Abre `model/Ticket.java` y añade dos campos nuevos:

```java
@NotBlank(message = "El creador es requerido")
private String createdBy;

private String assignedTo;
```

El modelo completo debe quedar así:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
  @Min(5) @Max(100)
  private Long id;
  @NotBlank(message = "El titulo es requerido")
  @Size(min = 1, max = 50)
  private String title;
  @NotBlank
  private String description;
  private String status;
  private LocalDateTime createdAt;
  private LocalDate estimatedResolutionDate;
  private LocalDateTime effectiveResolutionDate;
  @NotBlank(message = "El creador es requerido")
  private String createdBy;
  private String assignedTo;
}
```

---

## Paso 2: agregar validación en `TicketService.create()`

Abre `service/TicketService.java` y actualiza el método `create()`:

```java
public Ticket create(Ticket ticket) {
    // Validación 1: Título duplicado
    boolean exists = this.repository.existsByTitle(ticket.getTitle());
    if (exists) {
        throw new IllegalArgumentException("Ya existe un ticket con el título '" + ticket.getTitle() + "'");
    }

    // Validación 2: Creador ≠ Asignado
    if (ticket.getAssignedTo() != null && 
        ticket.getAssignedTo().equals(ticket.getCreatedBy())) {
        throw new IllegalArgumentException("El creador y el asignado no pueden ser el mismo usuario");
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDate ldNow = LocalDate.now();
    LocalDate estimated = ldNow.plusDays(5L);

    ticket.setStatus("NEW");
    ticket.setCreatedAt(now);
    ticket.setEstimatedResolutionDate(estimated);
    return this.repository.save(ticket);
}
```

**¿Por qué lanzar excepción?** El Service valida reglas de negocio. Si falla, lanza `IllegalArgumentException`. El Controller la capturará y convertirá a respuesta HTTP.

---

## Paso 3: agregar validación en `TicketService.updateById()`

Actualiza el método `updateById()` para usar `Optional`:

```java
public Optional<Ticket> updateById(Long id, Ticket ticket) {
    Optional<Ticket> found = this.repository.findById(id);
    if (found.isEmpty()) {
        return Optional.empty();
    }

    Ticket toUpdate = found.get();

    // Validación: Si se intenta cambiar el asignado, verifica que ≠ creador
    if (ticket.getAssignedTo() != null && 
        ticket.getAssignedTo().equals(toUpdate.getCreatedBy())) {
        throw new IllegalArgumentException("El creador y el asignado no pueden ser el mismo usuario");
    }

    toUpdate.setTitle(ticket.getTitle());
    toUpdate.setDescription(ticket.getDescription());
    toUpdate.setStatus(ticket.getStatus());
    toUpdate.setEffectiveResolutionDate(ticket.getEffectiveResolutionDate());
    if (ticket.getAssignedTo() != null) {
        toUpdate.setAssignedTo(ticket.getAssignedTo());
    }
    this.repository.update(toUpdate);
    return Optional.of(toUpdate);
}
```

---

## Paso 4: actualizar `TicketController.create()`

Envuelve el `service.create()` en try/catch para capturar la excepción:

```java
@PostMapping
public ResponseEntity<Object> create(@Valid @RequestBody Ticket ticket) {
    try {
        this.service.create(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body("Ticket Creado");
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

> **¿Por qué `409 Conflict` y no `400 Bad Request`?**
> El estándar HTTP define `409 Conflict` para situaciones donde la petición entra en conflicto con el estado actual del recurso (por ejemplo, un título duplicado o un creador que es el mismo que el asignado). `400 Bad Request` se reserva para problemas de formato o validación del request en sí.

---

## Paso 5: actualizar `TicketController.updateTicketById()`

Envuelve el `service.updateById()` en try/catch y usa `Optional`:

```java
@PutMapping("/by-id/{id}")
public ResponseEntity<Object> updateTicketById(
        @PathVariable Long id,
        @Valid @RequestBody Ticket ticket) {
    try {
        Optional<Ticket> updated = this.service.updateById(id, ticket);
        if (updated.isPresent()) {
            return ResponseEntity.ok(updated.get());
        }
        return ResponseEntity.notFound().build();
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

---

## Paso 6: crear `ErrorResponse`

Crea el archivo `model/ErrorResponse.java`:

```java
package cl.duoc.fullstack.tickets.model;

public record ErrorResponse(String message) {}
```

Jackson convierte automáticamente a JSON: `{"message": "..."}`

---

## Paso 7: verificar que todo funciona

### Prueba 1: crear ticket sin asignar (válido)

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{
  "title": "Ticket A",
  "description": "Descripción",
  "createdBy": "juan"
}
```

**Resultado:** `201 Created` con `"Ticket Creado"`

### Prueba 2: crear ticket con creador = asignado (inválido)

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{
  "title": "Ticket B",
  "description": "Descripción",
  "createdBy": "juan",
  "assignedTo": "juan"
}
```

**Resultado:** `409 Conflict` con:

```json
{
  "message": "El creador y el asignado no pueden ser el mismo usuario"
}
```

### Prueba 3: crear ticket con creador ≠ asignado (válido)

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{
  "title": "Ticket C",
  "description": "Descripción",
  "createdBy": "juan",
  "assignedTo": "maria"
}
```

**Resultado:** `201 Created`

### Prueba 4: modificar ticket a asignado = creador (inválido)

```
PUT http://localhost:8080/ticket-app/tickets/by-id/1
Content-Type: application/json

{
  "title": "Ticket C",
  "description": "Nueva descripción",
  "status": "IN_PROGRESS",
  "assignedTo": "juan"
}
```

**Resultado:** `409 Conflict` con el mismo error.






<!-- START OF FILE: docs_lessons_07-errors_03_manejo_global_vs_local.md -->
# Documento: docs lessons 07-errors 03 manejo global vs local
---
# Lección 07 - El debate: manejo local vs. manejo global de errores

## El problema que aparece cuando creces

Después de esta lección tu controlador maneja errores así:

```java
// En create():
} catch (IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(e.getMessage()));
}

// En getById():
.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
    .body(new ErrorResponse("Ticket con ID " + id + " no encontrado")));
```

Ahora imagina que tu API crece. Tienes `TicketController`, `CategoryController`, `UserController`, `ProjectController`. Cada uno tiene los mismos bloques try/catch y `.orElse(...)` con `new ErrorResponse(...)`.

Si decides cambiar la estructura del error — agregar un campo `timestamp`, cambiar el nombre de `message` a `error`, o agregar un código numérico — tendrías que editar **cada uno** de esos bloques en **cada controlador**.

Eso viola el principio DRY (*Don't Repeat Yourself*).

---

## La solución: `@ControllerAdvice`

Spring ofrece `@ControllerAdvice`, una anotación que marca una clase como **manejador global de excepciones**. Cualquier excepción que no sea capturada en el controlador sube hacia esta clase, que decide qué respuesta devolver.

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

Con este enfoque, los controladores **no tienen try/catch**. Si el service lanza una `IllegalArgumentException`, esta clase la intercepta automáticamente.

---

## Comparación directa

| Criterio | Manejo local (try/catch) | Manejo global (`@ControllerAdvice`) |
|---|---|---|
| **Dónde vive el código de error** | En cada método del controlador | En una sola clase centralizada |
| **DRY** | ❌ Repite lógica en cada método | ✅ Un solo lugar |
| **Claridad del controlador** | El flujo feliz y los errores están mezclados | Solo el flujo feliz; errores en otro lado |
| **Cambiar la estructura del error** | Hay que tocar cada método | Solo hay que tocar el `@ControllerAdvice` |
| **Facilidad de entendimiento inicial** | ✅ Fácil de razonar paso a paso | Requiere conocer el ciclo de vida de Spring MVC |
| **Errores específicos por endpoint** | ✅ Fácil de personalizar por caso | Posible, pero más elaborado |

---

## ¿Por qué no lo implementamos ya?

Tres razones pedagógicas:

1. **El try/catch local hace visible el flujo.** Cuando ves `try { ... } catch (IllegalArgumentException e)` en el método, entiendes exactamente qué puede salir mal y qué responde el servidor. `@ControllerAdvice` esconde ese enlace.

2. **`@ControllerAdvice` requiere conocer el ciclo de vida de Spring MVC.** Cuando una excepción no es capturada localmente, sube por el stack y Spring la intercepta. Entender eso bien — incluyendo cuándo aplica, cuándo no, y cómo interactúa con `@ResponseStatus` — merece su propio espacio.

3. **El problema de DRY no duele con un solo controlador.** Cuando tengas tres o cuatro controladores con la misma lógica de error, el dolor será concreto y la solución será obvia. Aprender la solución antes de sentir el problema dificulta recordarla.

---

## El `@ExceptionHandler` local: un punto medio

Hay una opción intermedia que usarás en la lección 08: `@ExceptionHandler` dentro del propio controlador. No es global como `@ControllerAdvice`, pero tampoco repite el mismo bloque en cada método.

```java
@RestController
@RequestMapping("/tickets")
public class TicketController {

    // ... endpoints ...

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
```

Este `@ExceptionHandler` aplica **solo** a las excepciones que lanzan los métodos de ese controlador. Es útil para validaciones que son específicas de un recurso y no necesitan ser globales.

---

## El mapa de evolución

```
Lección 07: try/catch por método + orElse con body
                ↓
Lección 08: @ExceptionHandler local (por validación)
                ↓
Futuro:     @ControllerAdvice global (cuando escale)
```

Cada paso tiene su momento correcto. Lo importante ahora es que entiendas **por qué** existe cada nivel, no solo cómo implementarlo.






<!-- START OF FILE: docs_lessons_07-errors_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 07-errors 04 checklist rubrica minima
---
# Lección 07 - Checklist y rúbrica mínima

Usa esta lista para verificar que implementaste correctamente la estructura de error antes de dar la lección por terminada.

---

## Checklist de la clase `ErrorResponse`

- ☐ Existe el archivo `model/ErrorResponse.java`
- ☐ Está declarada como `record`: `public record ErrorResponse(String message) {}`
- ☐ No tiene dependencias externas ni anotaciones adicionales
- ☐ Está en el paquete `cl.duoc.fullstack.tickets.model`

---

## Checklist de endpoints

| Endpoint | Código exitoso | Error + body `{"message":"..."}` |
|---|---|---|
| `GET /tickets` | 200 + lista | — |
| `GET /tickets/{id}` existente | 200 + ticket | — |
| `GET /tickets/{id}` inexistente | — | ☐ 404 + `{"message": "Ticket con ID X no encontrado"}` |
| `POST /tickets` título nuevo | 201 + ticket | — |
| `POST /tickets` título duplicado | — | ☐ 409 + `{"message": "Ya existe un ticket..."}` |
| `PUT /tickets/{id}` existente | 200 + ticket | — |
| `PUT /tickets/{id}` inexistente | — | ☐ 404 + `{"message": "Ticket con ID X no encontrado"}` |
| `DELETE /tickets/{id}` existente | 204 sin body | — |
| `DELETE /tickets/{id}` inexistente | — | ☐ 404 + `{"message": "Ticket con ID X no encontrado"}` |

---

## Checklist de código

### `TicketController.java`

- ☐ Todos los métodos de error devuelven `ResponseEntity<?>` (no `ResponseEntity<Object>`, no `ResponseEntity<Void>` en los que pueden fallar)
- ☐ El método `create()` usa `body(new ErrorResponse(e.getMessage()))`, **no** `body(e.getMessage())`
- ☐ Los métodos `getById()`, `update()` y `delete()` usan `body(new ErrorResponse("..."))` en el caso 404, **no** `.notFound().build()`
- ☐ El tipo en `.map()` está anotado: `.<ResponseEntity<?>>map(ResponseEntity::ok)` en `getById()` y `update()`
- ☐ No hay `null` explícito en el controlador

### `ErrorResponse.java`

- ☐ Jackson puede serializarla: al incluirla en el body de una `ResponseEntity`, Spring devuelve `Content-Type: application/json`
- ☐ El campo se llama `message` (sin prefijo `get`, es un record)

### `TicketService.java`

- ☐ El método `create()` lanza `IllegalArgumentException` con un mensaje descriptivo cuando el título ya existe
- ☐ No hay `return null` en ningún método del service

---

## Checklist de pruebas

Hiciste las siguientes pruebas en Postman / Thunder Client:

- ☐ `POST /tickets` con título existente → `409 Conflict` + `Content-Type: application/json` + `{"message": "..."}`
- ☐ `GET /tickets/by-id/999` → `404 Not Found`
- ☐ `PUT /tickets/by-id/999` → `404 Not Found`
- ☐ `DELETE /tickets/by-id/999` → `404 Not Found`
- ☐ `GET /tickets/by-id/1` → `200 OK` + ticket completo (el body exitoso no se rompió)
- ☐ `POST /tickets` con título nuevo → `201 Created` + `"Ticket Creado"` (el flujo feliz no se rompió)

---

## Errores comunes a evitar

| Error | Por qué está mal | Cómo corregirlo |
|---|---|---|
| `body(e.getMessage())` | Devuelve `text/plain`, no JSON | Usar `body(new ErrorResponse(e.getMessage()))` |
| `ResponseEntity.notFound().build()` en error | Sin cuerpo; el cliente no sabe qué falló | Usar `.status(NOT_FOUND).body(new ErrorResponse("..."))` |
| `ResponseEntity<Object>` cuando el tipo varía | Funciona, pero oculta la intención | Usar `ResponseEntity<?>` para tipos mixtos |
| Olvidar `.<ResponseEntity<?>>map(...)` | Error de compilación por tipos incompatibles | Anotar el tipo antes del `.map()` |
| Mensaje de error genérico ("Error") | El cliente no puede actuar sobre él | Incluir el valor que causó el error en el mensaje |






<!-- START OF FILE: docs_lessons_07-errors_05_actividad_individual.md -->
# Documento: docs lessons 07-errors 05 actividad individual
---
# Lección 07 - Actividad individual: errores estructurados en categorías

## Contexto

En las lecciones anteriores implementaste el CRUD de `Category`. En este momento, sus errores siguen el mismo patrón deficiente que tenía `Ticket` antes de esta lección: cuerpos de texto plano o respuestas 404 vacías.

Esta actividad es aplicar exactamente lo que aprendiste hoy, pero sobre el recurso que tú construiste.

---

## ¿Qué vas a construir?

Vas a actualizar `CategoryController` y `CategoryService` para que todos los errores devuelvan una estructura JSON consistente:

```json
{
  "message": "Categoría con ID 999 no encontrada"
}
```

---

## Requerimientos

### 1. Reutiliza `ErrorResponse`

**No** crees una segunda clase de error. La clase `ErrorResponse` que creaste hoy existe en el paquete `model` y está disponible para todos los controladores. Impórtala y úsala directamente en `CategoryController`.

### 2. Actualiza `CategoryService`

Verifica que `create()` lanza `IllegalArgumentException` con un mensaje descriptivo cuando el nombre ya existe:

```java
throw new IllegalArgumentException("Ya existe una categoría con el nombre '" + request.getName() + "'");
```

Si ya lo hiciste en la lección 06, no hay nada que cambiar aquí.

### 3. Actualiza `CategoryController`

Aplica exactamente el mismo patrón que `TicketController`:

| Endpoint | Código exitoso | Error con body |
|---|---|---|
| `GET /categories` | 200 + lista | — |
| `GET /categories/{id}` | 200 + categoría | 404 + `{"message": "Categoría con ID X no encontrada"}` |
| `POST /categories` | 201 + categoría | 409 + `{"message": "Ya existe una categoría con el nombre '...'"}` |
| `PUT /categories/{id}` | 200 + categoría | 404 + `{"message": "Categoría con ID X no encontrada"}` |
| `DELETE /categories/{id}` | 204 sin body | 404 + `{"message": "Categoría con ID X no encontrada"}` |

---

## Guía de implementación

### `CategoryController.java`

```java
// Todos los métodos con posible error devuelven ResponseEntity<?>

@GetMapping("/{id}")
public ResponseEntity<?> getById(@PathVariable Long id) {
    return service.findById(id)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Categoría con ID " + id + " no encontrada")));
}

@PostMapping
public ResponseEntity<?> create(@RequestBody Category category) {
    try {
        Category saved = service.create(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

Replica el mismo patrón para `PUT` y `DELETE`.

---

## Ejemplos de prueba

### Crear categoría duplicada

```
POST http://localhost:8080/categories
Content-Type: application/json

{ "name": "Hardware", "description": "Ya existe" }
```

Resultado esperado:

```json
{
  "message": "Ya existe una categoría con el nombre 'Hardware'"
}
```

Código: `409 Conflict`, `Content-Type: application/json`.

### Buscar categoría inexistente

```
GET http://localhost:8080/categories/999
```

Resultado esperado:

```json
{
  "message": "Categoría con ID 999 no encontrada"
}
```

Código: `404 Not Found`.

---

## Extensión opcional

Si terminaste antes, agrega mensajes de error más detallados al `DELETE`:

- Si la categoría tiene tickets asociados, que el servidor responda `409 Conflict` con el mensaje: `"No se puede eliminar la categoría 'Hardware' porque tiene tickets asociados"`

Por ahora no hay una relación real entre categorías y tickets, así que puedes simularlo con un contador fijo: si el ID de la categoría a eliminar es `1`, asume que tiene tickets asociados.

---

## Criterios de evaluación

| Criterio | Puntaje |
|---|---|
| Todos los errores de `CategoryController` devuelven `Content-Type: application/json` | 30% |
| Los códigos HTTP son correctos (404 para no encontrado, 409 para conflicto) | 25% |
| Se reutiliza `ErrorResponse` del paquete `model` sin duplicarla | 25% |
| El mensaje incluye el valor que causó el error (ID o nombre) | 20% |




