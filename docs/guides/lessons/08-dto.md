<!-- START OF FILE: docs_lessons_08-dto_01_objetivo_y_alcance.md -->
# Documento: docs lessons 08-dto 01 objetivo y alcance
---
# Lección 08 - Validaciones y DTO: ¿qué vas a aprender?

## ¿De dónde venimos?

En la lección anterior conseguiste que todos los errores de tu API devuelvan el mismo formato JSON: `{"message": "..."}`. Ahora el contrato de errores es claro y consistente.

Pero hay un problema diferente que aún existe: el cliente puede enviarte **campos que no debería poder enviar**.

Observa el body que acepta tu `POST /tickets` ahora mismo:

```json
{
  "id": 99,
  "title": "Bug en login",
  "description": "No puedo iniciar sesión",
  "status": "RESOLVED",
  "createdAt": "2020-01-01T00:00:00",
  "estimatedResolutionDate": "2020-01-06"
}
```

Tu API acepta este body sin quejarse. El campo `id` se ignora porque el `Repository` lo sobreescribe, y `createdAt` se ignora porque el `Service` asigna su propio valor. Pero `status` **sí podría ser tomado** si no tienes cuidado.

El problema no es solo este caso concreto. El problema de fondo es que estás usando `Ticket` — tu modelo de dominio — como la entrada directa de la API. Eso mezcla dos responsabilidades que deben estar separadas:

- **Lo que el cliente puede enviar** (campos de entrada)
- **Lo que el sistema almacena y procesa** (modelo de dominio)

Esta lección existe para resolver eso.

---

## ¿Qué vas a construir?

Al terminar esta lección tendrás:

1. Un **DTO de entrada** (`TicketRequest`) implementado como un **Java `record`** — una característica de Java 21 que genera automáticamente constructor, getters, `equals()`, `hashCode()` y `toString()`
2. **Validación automática** del título con `@NotBlank`: si viene vacío, la API responde `400 Bad Request` con `{"message": "título: El titulo es requerido"}`
3. Un `@ExceptionHandler` en el controlador que convierte los errores de validación al formato `ErrorResponse`

### Lo que vas a ser capaz de explicar

Al terminar deberías poder responder:

- ¿Qué es un DTO y para qué sirve?
- ¿Por qué el modelo de dominio no debería ser la forma de entrada de la API?
- ¿Qué es un `record` en Java y por qué es ideal para DTOs?
- ¿Qué hace `@NotBlank` y en qué se diferencia de `@NotNull` y `@NotEmpty`?
- ¿Qué hace `@Valid` en el parámetro del controlador?
- ¿Qué excepción lanza Spring cuando la validación falla y cómo se captura?
- ¿Por qué el `@ExceptionHandler` local es preferible al try/catch por cada método para errores de validación?

---

## ¿Qué requerimientos implementamos en esta lección?

> El proyecto completo está descrito en [`00_enunciado_proyecto.md`](../00_enunciado_proyecto.md).

| Requerimiento | Lo que construimos |
|---|---|
| **REQ-12** — Título no puede estar vacío | La anotación `@NotBlank` en `TicketRequest.title` + respuesta `400` |
| **REQ-13** — DTO separado del modelo | El `record TicketRequest` en el paquete `dto` |

---

## ¿Qué NO cubre esta lección? (y por qué)

| Tema | ¿Por qué lo dejamos después? |
|---|---|
| `@ControllerAdvice` global para validaciones | Requiere entender bien la jerarquía de excepciones; el `@ExceptionHandler` local es suficiente por ahora |
| Validaciones complejas (`@Min`, `@Max`, `@Pattern`, `@Size`) | Primero entiende el flujo básico; agregarlas es trivial una vez que entiendes `@NotBlank` |
| MapStruct u otras librerías de mapeo DTO → modelo | El mapeo manual hace visible la transformación; una librería lo oculta |
| DTO de respuesta (Response DTO) | Ahora solo controlamos la **entrada**; los DTOs de salida son un tema separado |
| Validaciones de negocio en el DTO | Las reglas de negocio (como "no duplicados") pertenecen al Service, no al DTO |

El foco de esta lección es uno: **separar la entrada del modelo y validarla antes de que llegue al Service**.

---

## La estructura que tienes al comenzar

```
src/main/java/cl/duoc/fullstack/tickets/
├── controller/
│   └── TicketController.java   ← acepta @RequestBody Ticket (modelo directo)
├── model/
│   ├── Ticket.java
│   └── ErrorResponse.java
├── respository/
│   └── TicketRepository.java
├── service/
│   └── TicketService.java      ← create(Ticket ticket)
└── TicketsApplication.java
```

Y la estructura que tendrás al terminar:

```
src/main/java/cl/duoc/fullstack/tickets/
├── controller/
│   └── TicketController.java   ← acepta @Valid @RequestBody TicketRequest + @ExceptionHandler
├── dto/
│   └── TicketRequest.java      ← nueva: record de entrada con @NotBlank
├── model/
│   ├── Ticket.java             ← sin anotaciones de validación (modelo puro)
│   └── ErrorResponse.java
├── respository/
│   └── TicketRepository.java
├── service/
│   └── TicketService.java      ← create(TicketRequest request), updateById(Long id, TicketRequest request)
└── TicketsApplication.java
```





<!-- START OF FILE: docs_lessons_08-dto_02_guion_paso_a_paso.md -->
# Documento: docs lessons 08-dto 02 guion paso a paso
---
# Lección 08 - Tutorial paso a paso: DTO y validaciones

Sigue esta guía en orden. Vas a separar la entrada de la API del modelo de dominio e introducir validación automática.

---

## Paso 1: verificar la dependencia de validación

En la lección anterior ya agregaste la dependencia de Bean Validation en `pom.xml`. Verifica que esté presente:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Esta dependencia trae Hibernate Validator (la implementación de referencia de Bean Validation / JSR-380), que provee anotaciones como `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max` y `@Pattern`.

---

## Paso 2: entender por qué necesitamos un DTO

Antes de crear `TicketRequest`, entiende el problema que resuelve.

Tu endpoint actual acepta esto:

```java
@PostMapping
public ResponseEntity<?> create(@RequestBody Ticket ticket) { ... }
```

El cliente puede enviar un JSON con **cualquier campo** de `Ticket`:

```json
{
  "id": 999,
  "title": "Bug crítico",
  "status": "RESOLVED",
  "createdAt": "2020-01-01T00:00:00",
  "estimatedResolutionDate": "2020-01-06"
}
```

Algunos de esos campos se ignoran en el `Service` (el `id`, `createdAt`, `estimatedResolutionDate`), pero `status` **podría ser leído** en una versión futura del código. El modelo expuesto como entrada es una bomba de tiempo.

La solución: un DTO que declara **solo** lo que el cliente puede enviar.

---

## Paso 3: crear el paquete `dto` y el `record TicketRequest`

Crea el directorio `dto` dentro de `cl/duoc/fullstack/tickets/` y luego el archivo:

```
src/main/java/cl/duoc/fullstack/tickets/dto/TicketRequest.java
```

```java
package cl.duoc.fullstack.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record TicketRequest(
    @NotBlank(message = "El titulo es requerido")
    @Size(min = 1, max = 50)
    String title,
    @NotBlank(message = "La descripción es requerida")
    String description,
    @NotBlank(message = "El creador es requerido")
    String createdBy,
    String assignedTo,
    String status,
    LocalDateTime effectiveResolutionDate
) {}
```

> **¿Qué es un `record` en Java?**
> Un `record` es una clase especial introducida en Java 16 que genera automáticamente:
> - Un constructor con todos los campos como parámetros
> - Métodos de acceso por nombre de campo (ej: `title()`, `description()`)
> - `equals()`, `hashCode()` y `toString()` basados en todos los campos
>
> Los records son **inmutables**: una vez creados, sus valores no pueden cambiar. Esto los hace ideales para DTOs, donde solo necesitas transportar datos de un lugar a otro sin modificarlos.

> **¿Por qué un `record` y no una clase con Lombok?**
> Para el modelo `Ticket` usamos Lombok porque necesitamos setters (mutabilidad) — el `Service` modifica campos como `status`, `createdAt`, etc. Pero un DTO de entrada no necesita setters: Jackson lo crea una vez a partir del JSON y nadie lo modifica después. El `record` expresa esa intención con menos código y sin dependencias externas.
>
> Jackson (la librería de serialización que usa Spring) soporta records de forma nativa desde la versión 2.12 — no necesitas `@JsonCreator` ni configuración adicional.

> **¿Por qué `@NotBlank` y no `@NotNull`?**
> `@NotNull` solo verifica que el campo no sea `null`. `@NotBlank` es más estricto: verifica que no sea null, no sea una cadena vacía `""`, y no sea solo espacios en blanco `"   "`. Para un título de ticket, `"   "` es tan inválido como `null`, por eso usamos `@NotBlank`.
>
> **Diferencias resumidas:**
> - `@NotNull` → `null` falla; `""` y `"   "` pasan
> - `@NotEmpty` → `null` y `""` fallan; `"   "` pasa
> - `@NotBlank` → `null`, `""` y `"   "` fallan

> **¿Por qué incluir `status` y `effectiveResolutionDate` en el DTO si el servidor los controla en el `create`?**
> Porque el mismo `TicketRequest` se reutiliza para el `PUT /tickets/by-id/{id}`, donde el cliente sí puede actualizar el estado y la fecha de resolución. Un campo opcional en el DTO es válido: simplemente se ignora si no viene.

---

## Paso 4: quitar las anotaciones de validación del modelo `Ticket`

Ahora que la validación vive en el DTO, el modelo `Ticket` queda como un POJO puro de Lombok — sin anotaciones de Jakarta Validation:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
  private Long id;
  private String title;
  private String description;
  private String status;
  private LocalDateTime createdAt;
  private LocalDate estimatedResolutionDate;
  private LocalDateTime effectiveResolutionDate;
  private String createdBy;
  private String assignedTo;
}
```

> **¿Por qué quitamos las anotaciones del modelo?**
> Porque la validación de entrada es responsabilidad del DTO, no del modelo de dominio. El `Ticket` representa lo que el sistema almacena internamente — el `TicketRequest` representa lo que el cliente puede enviar. Mezclar ambas responsabilidades en la misma clase fue exactamente el problema que identificamos al principio.

---

## Paso 5: actualizar `TicketService.create()` para aceptar `TicketRequest`

El `Service` recibe el DTO y construye el `Ticket` internamente. Esta transformación es responsabilidad del `Service`: es el puente entre la capa de entrada y el modelo de dominio.

**Antes:**

```java
public Ticket create(Ticket ticket) {
    // validaciones...
    ticket.setStatus("NEW");
    ticket.setCreatedAt(LocalDateTime.now());
    ticket.setEstimatedResolutionDate(LocalDate.now().plusDays(5));
    return this.repository.save(ticket);
}
```

**Después:**

```java
public Ticket create(TicketRequest request) {
    if (this.repository.existsByTitle(request.title())) {
        throw new IllegalArgumentException(
            "Ya existe un ticket con el título: \"" + request.title() + "\"");
    }

    if (request.assignedTo() != null
        && request.assignedTo().equals(request.createdBy())) {
        throw new IllegalArgumentException("El creador y el asignado no pueden ser el mismo usuario");
    }

    Ticket ticket = new Ticket();
    ticket.setTitle(request.title());
    ticket.setDescription(request.description());
    ticket.setCreatedBy(request.createdBy());
    ticket.setAssignedTo(request.assignedTo());
    ticket.setStatus("NEW");
    ticket.setCreatedAt(LocalDateTime.now());
    ticket.setEstimatedResolutionDate(LocalDate.now().plusDays(5));

    return this.repository.save(ticket);
}
```

La diferencia clave: el `Service` **construye** el `Ticket` a partir del DTO. El cliente nunca puede inyectar un `Ticket` preformado con campos no permitidos.

> **¿Por qué se accede a los campos con `request.title()` y no `request.getTitle()`?**
> Los records en Java generan métodos de acceso con el mismo nombre que el campo, sin el prefijo `get`. Es una convención del lenguaje para records.

> **¿Por qué crear `new Ticket()` en el Service y no en el Repository?**
> El `Service` tiene la responsabilidad de aplicar las reglas de negocio: qué campos asigna el servidor, cuáles vienen del cliente, cuáles se calculan. El `Repository` solo guarda. Si el `Repository` construyera el `Ticket`, mezclaría lógica de negocio con lógica de persistencia.

---

## Paso 6: actualizar `TicketService.updateById()` para aceptar `TicketRequest`

```java
public Optional<Ticket> updateById(Long id, TicketRequest request) {
    Optional<Ticket> found = this.repository.findById(id);
    if (found.isEmpty()) {
        return Optional.empty();
    }

    Ticket toUpdate = found.get();

    if (request.assignedTo() != null
        && request.assignedTo().equals(toUpdate.getCreatedBy())) {
        throw new IllegalArgumentException("El creador y el asignado no pueden ser el mismo usuario");
    }

    toUpdate.setTitle(request.title());
    toUpdate.setDescription(request.description());
    if (request.status() != null && !request.status().isBlank()) {
        toUpdate.setStatus(request.status());
    }
    toUpdate.setEffectiveResolutionDate(request.effectiveResolutionDate());
    if (request.assignedTo() != null) {
        toUpdate.setAssignedTo(request.assignedTo());
    }
    this.repository.update(toUpdate);
    return Optional.of(toUpdate);
}
```

El `status` en el `PUT` es opcional: si el cliente no lo envía (llega como `null`), el status del ticket no cambia. Si lo envía, sí se actualiza.

---

## Paso 7: actualizar el controlador para usar `@Valid` y `TicketRequest`

**POST — antes y después:**

```java
// Antes:
public ResponseEntity<Object> create(@RequestBody Ticket ticket) { ... }

// Después:
public ResponseEntity<Object> create(@Valid @RequestBody TicketRequest request) { ... }
```

**PUT — antes y después:**

```java
// Antes:
public ResponseEntity<Object> updateTicketById(@PathVariable Long id, @RequestBody Ticket ticket) { ... }

// Después:
public ResponseEntity<Object> updateTicketById(@PathVariable Long id, @Valid @RequestBody TicketRequest request) { ... }
```

> **¿Qué hace `@Valid`?**
> Le indica a Spring que debe validar el objeto anotado con las restricciones de Bean Validation (`@NotBlank`, etc.) antes de ejecutar el método. Si la validación falla, Spring lanza `MethodArgumentNotValidException` **antes** de entrar al cuerpo del método. El método nunca se ejecuta.
>
> Si olvidaras `@Valid`, las anotaciones `@NotBlank` estarían en el DTO pero nunca se evaluarían. El `@RequestBody` deserializa el JSON independientemente de si `@Valid` está presente o no.

---

## Paso 8: agregar el `@ExceptionHandler` para errores de validación

Cuando `@Valid` falla, Spring lanza `MethodArgumentNotValidException`. Si no capturas esa excepción, Spring devuelve su propio formato de error por defecto — que no coincide con tu `ErrorResponse`.

Agrega este método en `TicketController`:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse(message));
}
```

**Código equivalente sin expresiones lambda:**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
    List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fieldErrors.size(); i++) {
        FieldError err = fieldErrors.get(i);
        sb.append(err.getField()).append(": ").append(err.getDefaultMessage());
        if (i < fieldErrors.size() - 1) {
            sb.append(", ");
        }
    }
    return ResponseEntity.badRequest().body(new ErrorResponse(sb.toString()));
}
```

> **¿Por qué `@ExceptionHandler` y no try/catch dentro del método?**
> Con `@Valid`, la excepción se lanza **antes** de entrar al método — no puedes capturarla con try/catch porque el método nunca empieza. `@ExceptionHandler` es el mecanismo correcto: intercepta la excepción en la capa del controlador y devuelve la respuesta adecuada. Es el primer nivel de centralización de errores, antes de `@ControllerAdvice`.

> **¿Este `@ExceptionHandler` aplica a todos los controladores?**
> No. Un `@ExceptionHandler` dentro de un `@RestController` aplica **solo** a las excepciones que lanza ese controlador. Para que aplique globalmente, necesitarías moverlo a una clase con `@ControllerAdvice` — exactamente lo que discutimos en la lección 07.

---

## Paso 9: probar la validación

### Prueba 1: título vacío

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{ "title": "", "description": "Descripción", "createdBy": "juan" }
```

Resultado esperado (`400 Bad Request`):

```json
{
  "message": "title: El titulo es requerido"
}
```

### Prueba 2: título con solo espacios

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{ "title": "   ", "description": "Descripción", "createdBy": "juan" }
```

Resultado esperado: `400 Bad Request` con el mismo mensaje de error.

### Prueba 3: título ausente (null)

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{ "description": "Sin título", "createdBy": "juan" }
```

Resultado esperado: `400 Bad Request` con mensaje de validación.

### Prueba 4: creación válida (el flujo feliz no se rompió)

```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{ "title": "Bug en facturación", "description": "El sistema falla al generar PDF", "createdBy": "juan" }
```

Resultado esperado: `201 Created` con `"Ticket Creado"`.

### Prueba 5: validación en PUT también funciona

```
PUT http://localhost:8080/ticket-app/tickets/by-id/1
Content-Type: application/json

{ "title": "", "description": "Descripción", "createdBy": "juan" }
```

Resultado esperado: `400 Bad Request` con mensaje de validación.

---

## Paso 10: reflexiona antes de cerrar

1. Si el cliente envía `{"id": 99, "title": "Test", "status": "RESOLVED", "createdBy": "juan"}` al `POST /tickets`, ¿qué ocurre con el `id` y el `status` que envió? ¿El servidor los usa o los descarta?
2. ¿Por qué el `Service` construye el `Ticket` a partir del `TicketRequest` en lugar de recibirlo directamente? ¿Qué pasaría si un futuro desarrollador agrega `ticket.setStatus(request.status())` al service?
3. Si agregas `@NotBlank` en `description` también, ¿qué cambiaría en la respuesta de error si ambos campos son inválidos?
4. ¿Qué diferencia hay entre un `record` de Java y una clase con Lombok? ¿Cuándo preferirías uno sobre otro?





<!-- START OF FILE: docs_lessons_08-dto_03_por_que_dto.md -->
# Documento: docs lessons 08-dto 03 por que dto
---
# Lección 08 - Por qué necesitas un DTO

## El problema con exponer el modelo directamente

Una API que usa su modelo de dominio como entrada directa tiene un problema fundamental: el **modelo está diseñado para el sistema**, no para el cliente.

```java
// Mal: el modelo de dominio es la entrada
@PostMapping
public ResponseEntity<?> create(@RequestBody Ticket ticket) { ... }
```

Esto tiene tres consecuencias:

---

## Razón 1: Seguridad — el cliente no debería poder controlar todo

El modelo `Ticket` tiene campos que el servidor asigna: `id`, `status`, `createdAt`, `estimatedResolutionDate`. Si expones el modelo directamente, el cliente puede intentar enviar esos campos.

**Ejemplo de ataque (mass assignment):**

```json
{
  "title": "Bug legítimo",
  "description": "...",
  "status": "RESOLVED",
  "createdAt": "2020-01-01T00:00:00"
}
```

Si el código del `Service` no tiene cuidado (o alguien lo modifica inadvertidamente), esos campos podrían ser tomados tal cual. Con un DTO, ese peligro desaparece: el objeto de entrada solo tiene los campos que declaraste explícitamente.

```java
// Seguro: TicketRequest no tiene id, status, createdAt, estimatedResolutionDate
public class TicketRequest {
    @NotBlank
    private String title;
    private String description;
}
```

---

## Razón 2: Control — el servidor decide qué calcula

Algunos campos del `Ticket` son **calculados por el servidor** según reglas de negocio:

- `id` → asignado automáticamente
- `status` → siempre empieza en `NEW`
- `createdAt` → momento exacto del servidor
- `estimatedResolutionDate` → 5 días después de la creación

Si el modelo fuera la entrada, el `Service` tendría que **ignorar activamente** lo que el cliente mandó. Con un DTO, estos campos simplemente no existen en la entrada — no hay nada que ignorar.

```java
// El Service construye el Ticket con sus propias reglas
Ticket ticket = new Ticket();
ticket.setTitle(request.getTitle());     // del cliente
ticket.setDescription(request.getDescription()); // del cliente
ticket.setStatus("NEW");                 // servidor
ticket.setCreatedAt(LocalDateTime.now()); // servidor
ticket.setEstimatedResolutionDate(LocalDate.now().plusDays(5)); // servidor
```

---

## Razón 3: Desacoplamiento — el modelo puede evolucionar sin romper la API

Si en el futuro agregas un campo `priority` al modelo `Ticket` (con base de datos real), el contrato de entrada de tu API **no necesita cambiar**. El DTO sigue siendo el mismo. Solo el `Service` sabe cómo mapear `request → ticket` con el campo nuevo.

Si expusieras el modelo directamente, cualquier cambio en el modelo rompería el contrato con los clientes que ya consumen tu API.

---

## Razón 4: Validación — dónde poner las restricciones

Las anotaciones `@NotBlank`, `@Size`, `@Min` pertenecen a la entrada, no al modelo de dominio.

¿Por qué? Porque la misma regla puede ser diferente según de dónde viene el dato:

- El `title` puede ser obligatorio cuando lo envía el cliente por HTTP
- Pero puede ser `null` legítimamente si se genera internamente (por ej., un sistema de importación)

Si pones `@NotBlank` en el modelo, aplica en **todos los contextos**. Si lo pones en el DTO, aplica solo cuando el cliente envía datos por la API.

```java
// DTO: @NotBlank aplica a la entrada de la API
public class TicketRequest {
    @NotBlank(message = "El título no puede estar vacío")
    private String title;
}

// Modelo: sin @NotBlank, libre para otros contextos
public class Ticket {
    private String title; // puede ser generado internamente sin restricción
}
```

---

## Los tres tipos de DTO que existen

Esta lección introduce solo uno de ellos, pero es útil conocer el panorama:

| Tipo | Nombre convencional | ¿Cuándo se usa? |
|---|---|---|
| **DTO de entrada** (lo que implementamos hoy) | `XxxRequest`, `XxxDto`, `CreateXxxCommand` | Para recibir datos del cliente: `@RequestBody` |
| **DTO de salida** | `XxxResponse`, `XxxView` | Para controlar qué campos devuelve la API al cliente |
| **DTO de capa** | `XxxDto` (interno) | Para pasar datos entre capas sin exponer el modelo |

Por ahora usamos el modelo `Ticket` directamente como respuesta (`200 OK` devuelve el `Ticket` completo). En una API más madura, tendrías un `TicketResponse` que oculta campos internos y puede transformar formatos.

---

## El patrón completo con DTO

```
Cliente
    ↓ envía JSON
    ↓
TicketRequest (DTO de entrada)
    ↓ @Valid lo valida
    ↓
TicketController.create(TicketRequest)
    ↓ llama al service
    ↓
TicketService.create(TicketRequest)
    ↓ valida reglas de negocio
    ↓ construye Ticket
    ↓
TicketRepository.save(Ticket)
    ↓
Ticket (modelo de dominio, persiste en memoria)
    ↓ devuelve al controller
    ↓
ResponseEntity con Ticket como body
    ↓ Jackson serializa a JSON
    ↓
Cliente recibe { id, title, description, status, createdAt, ... }
```

Cada capa trabaja con lo que necesita. El cliente nunca ve el código interno ni puede manipular campos que no le corresponden.






<!-- START OF FILE: docs_lessons_08-dto_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 08-dto 04 checklist rubrica minima
---
# Lección 08 - Checklist y rúbrica mínima

Usa esta lista para verificar que implementaste correctamente el DTO y las validaciones antes de dar la lección por terminada.

---

## Checklist de dependencia

- ☐ `pom.xml` incluye `spring-boot-starter-validation` como dependencia
- ☐ La aplicación compila y levanta correctamente después de agregar la dependencia

---

## Checklist del DTO `TicketRequest`

- ☐ Existe el archivo `dto/TicketRequest.java`
- ☐ Está en el paquete `cl.duoc.fullstack.tickets.dto`
- ☐ Tiene `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor` de Lombok
- ☐ El campo `title` tiene `@NotBlank(message = "El título no puede estar vacío")`
- ☐ **No tiene** los campos `id`, `createdAt`, `estimatedResolutionDate` (esos los asigna el servidor)
- ☐ Tiene `description` (String, sin validación obligatoria)
- ☐ Tiene `status` (String, opcional, para el caso del PUT)

---

## Checklist del Service

- ☐ `create(TicketRequest request)` recibe el DTO, **no** `Ticket` directamente
- ☐ Dentro de `create()` se construye un `new Ticket()` y se asignan sus campos manualmente
- ☐ Los campos `status`, `createdAt`, `estimatedResolutionDate` son asignados por el Service, **no** tomados del request
- ☐ `update(Long id, TicketRequest request)` también recibe el DTO
- ☐ No hay `return null` en ningún método del Service

---

## Checklist del Repository

- ☐ `update(Long id, TicketRequest request)` acepta el DTO
- ☐ El `status` del update solo se aplica si `request.getStatus()` no es `null` ni blank

---

## Checklist del Controller

### Endpoints con `@Valid`

- ☐ `POST /tickets` usa `@Valid @RequestBody TicketRequest request`
- ☐ `PUT /tickets/{id}` usa `@Valid @RequestBody TicketRequest request`
- ☐ Los endpoints `GET` y `DELETE` **no** necesitan `@Valid` (no tienen body de entrada)

### `@ExceptionHandler`

- ☐ Existe un método con `@ExceptionHandler(MethodArgumentNotValidException.class)` en el controlador
- ☐ Extrae el mensaje de los `FieldErrors` con `.getBindingResult().getFieldErrors()`
- ☐ Devuelve `ResponseEntity.badRequest().body(new ErrorResponse(message))` → `400 Bad Request`
- ☐ El formato del mensaje incluye el campo y su error: `"title: El título no puede estar vacío"`

---

## Checklist de pruebas

- ☐ `POST /tickets` con `"title": ""` → `400 Bad Request` + `{"message": "title: El titulo es requerido"}`
- ☐ `POST /tickets` con `"title": "   "` → `400 Bad Request` (blanco)
- ☐ `POST /tickets` sin campo `title` → `400 Bad Request`
- ☐ `PUT /tickets/by-id/1` con `"title": ""` → `400 Bad Request`
- ☐ `POST /tickets` válido → `201 Created` + `"Ticket Creado"` (flujo feliz no se rompió)
- ☐ `GET /tickets`, `GET /tickets/by-id/1`, `DELETE /tickets/by-id/1` siguen funcionando correctamente (no se rompió nada)

---

## Errores comunes a evitar

| Error | Por qué está mal | Cómo corregirlo |
|---|---|---|
| Olvidar `@Valid` en el parámetro del controller | Las anotaciones del DTO nunca se evalúan | Agregar `@Valid` antes de `@RequestBody TicketRequest` |
| Olvidar la dependencia `spring-boot-starter-validation` | `@NotBlank` compila pero no hace nada | Agregar la dependencia al `pom.xml` |
| Poner `@NotBlank` en el modelo `Ticket` | Aplica en todos los contextos, no solo en la entrada de la API | Poner las validaciones en el DTO |
| No agregar `@ExceptionHandler` | Spring devuelve su propio error en un formato diferente al `ErrorResponse` | Agregar el handler en el controller |
| Usar `ticket.setStatus(request.getStatus())` en `create()` | El cliente puede establecer el status inicial del ticket | El Service siempre asigna `"NEW"` independientemente del request |
| No tener `@NoArgsConstructor` en el DTO | Jackson no puede deserializar el JSON | Agregar `@NoArgsConstructor` con Lombok |






<!-- START OF FILE: docs_lessons_08-dto_05_actividad_individual.md -->
# Documento: docs lessons 08-dto 05 actividad individual
---
# Lección 08 - Actividad individual: DTO y validación para categorías

## Contexto

Tu `CategoryController` actualmente recibe `@RequestBody Category` directamente. Esto tiene los mismos problemas que tuvo `TicketController` antes de esta lección: el cliente puede enviar campos que no le corresponden, y no hay validación automática.

---

## ¿Qué vas a construir?

Vas a crear un DTO de entrada para categorías y agregar validación, siguiendo el mismo patrón que usaste con `TicketRequest`.

---

## Paso 1: crear `CategoryRequest`

Crea el archivo `dto/CategoryRequest.java`:

| Campo | Tipo | Validación |
|---|---|---|
| `name` | `String` | `@NotBlank(message = "El nombre no puede estar vacío")` |
| `description` | `String` | Sin validación obligatoria |

```java
package cl.duoc.fullstack.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String name;

    private String description;
}
```

---

## Paso 2: actualizar `CategoryService`

Modifica los métodos `create()` y `update()` para que acepten `CategoryRequest` en lugar de `Category`:

```java
public Category create(CategoryRequest request) {
    if (this.repository.existsByName(request.getName())) {
        throw new IllegalArgumentException(
            "Ya existe una categoría con el nombre '" + request.getName() + "'");
    }
    Category category = new Category();
    category.setName(request.getName());
    category.setDescription(request.getDescription());
    return this.repository.save(category);
}

public Optional<Category> update(Long id, CategoryRequest request) {
    return this.repository.update(id, request);
}
```

---

## Paso 3: actualizar `CategoryRepository.update()`

```java
public Optional<Category> update(Long id, CategoryRequest request) {
    Optional<Category> found = findById(id);
    found.ifPresent(category -> {
        category.setName(request.getName());
        category.setDescription(request.getDescription());
    });
    return found;
}
```

---

## Paso 4: actualizar `CategoryController`

```java
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody CategoryRequest request) {
    try {
        Category saved = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
}

@PutMapping("/{id}")
public ResponseEntity<?> update(@PathVariable Long id,
                                @Valid @RequestBody CategoryRequest request) {
    return service.update(id, request)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Categoría con ID " + id + " no encontrada")));
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(java.util.stream.Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse(message));
}
```

---

## Pruebas requeridas

| Prueba | Resultado esperado |
|---|---|
| `POST /categories` con `"name": ""` | `400 Bad Request` + `{"message": "name: El nombre no puede estar vacío"}` |
| `POST /categories` con `"name": "   "` | `400 Bad Request` |
| `POST /categories` con nombre válido | `201 Created` con categoría |
| `POST /categories` con nombre duplicado | `409 Conflict` + `{"message": "..."}` |
| `PUT /categories/1` con `"name": ""` | `400 Bad Request` |
| `PUT /categories/999` | `404 Not Found` + `{"message": "..."}` |
| `GET /categories` | `200 OK` (no se rompió) |

---

## Desafío opcional

Agrega validación de longitud mínima en el nombre:

```java
@NotBlank(message = "El nombre no puede estar vacío")
@Size(min = 3, message = "El nombre debe tener al menos 3 caracteres")
private String name;
```

Prueba con `"name": "AB"` y verifica que el mensaje de error refleja la nueva validación.

---

## Criterios de evaluación

| Criterio | Puntaje |
|---|---|
| `CategoryRequest` en paquete `dto` con `@NotBlank` en `name` | 25% |
| `CategoryService.create()` y `update()` aceptan `CategoryRequest` y construyen `Category` internamente | 25% |
| `@Valid` en los endpoints `POST` y `PUT` del controller | 20% |
| `@ExceptionHandler` en `CategoryController` devuelve `ErrorResponse` con `400` | 20% |
| El cliente no puede fijar campos como `id` desde el body | 10% |




