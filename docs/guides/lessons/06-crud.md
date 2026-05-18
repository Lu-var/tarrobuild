<!-- START OF FILE: docs_lessons_06-crud_01_objetivo_y_alcance.md -->
# Documento: docs lessons 06-crud 01 objetivo y alcance
---
# Lección 06 - CRUD completo: ¿qué vas a aprender?

## ¿De dónde venimos?

En la lección anterior extendiste la API de tickets para que también pudiera **crear** recursos. Ahora tienes dos endpoints funcionando:

```
GET  /tickets → devuelve todos los tickets
POST /tickets → recibe un ticket nuevo y lo guarda
```

Eso está bien. Pero una API que solo sabe leer y crear tiene una limitación evidente: no puede buscar un ticket específico, no puede actualizarlo y no puede eliminarlo.

Esta lección existe para resolver eso.

---

## ¿Qué vas a construir?

Al terminar esta lección tendrás un **CRUD completo** sobre el recurso `Ticket`. Concretamente, agregarás:

- `GET /tickets/{id}` → buscar un ticket por su ID
- `PUT /tickets/{id}` → actualizar un ticket existente
- `DELETE /tickets/{id}` → eliminar un ticket

Con esto el CRUD queda completo:

| Operación | Método HTTP | Endpoint          |
|-----------|-------------|-------------------|
| Create    | POST        | `/tickets`        |
| Read all  | GET         | `/tickets`        |
| Read one  | GET         | `/tickets/{id}`   |
| Update    | PUT         | `/tickets/{id}`   |
| Delete    | DELETE      | `/tickets/{id}`   |

### Lo que vas a ser capaz de explicar

Más que ejecutar el código, el objetivo es que entiendas cada decisión. Al terminar deberías poder responder:

- ¿Para qué sirve `@PathVariable` y en qué se diferencia de `@RequestBody`?
- ¿Por qué `GET /tickets/{id}` devuelve `404` cuando el ticket no existe?
- ¿Por qué un `DELETE` exitoso devuelve `204 No Content` y no `200 OK`?
- ¿Qué significa que una operación sea **idempotente**?
- ¿Por qué el ID correcto para una actualización viene de la URL y no del body?

---

## ¿Qué requerimientos implementamos en esta lección?

> El proyecto completo está descrito en [`00_enunciado_proyecto.md`](../00_enunciado_proyecto.md).
> Ahí encontrarás el escenario, los actores y la lista completa de requerimientos numerados.

De esa lista, esta lección implementa los **cuatro restantes**:

| Requerimiento | Lo que construimos |
|---------------|--------------------|
| **REQ-07** — Consultar un ticket por ID | El endpoint `GET /tickets/{id}` con `@PathVariable` |
| **REQ-08** — Actualizar título o descripción | El endpoint `PUT /tickets/{id}` |
| **REQ-09** — Eliminar un ticket | El endpoint `DELETE /tickets/{id}` |
| **REQ-10** — Error claro cuando el ticket no existe | `Optional<T>` en las capas internas + respuesta `404 Not Found` en el controlador |

Con esta lección el sistema cumple **todos** los requerimientos del enunciado. El proyecto Tickets tiene un CRUD completo y funcional.

---

## ¿Qué NO cubre esta lección? (y por qué)

| Tema | ¿Por qué lo dejamos después? |
|---|---|
| Manejo global de errores (`@ControllerAdvice`) | Requiere conocer las excepciones típicas de una API; lo trabajaremos con más endpoints disponibles |
| Validaciones (`@Valid`, `@NotBlank`) | Son una capa adicional; primero el flujo básico |
| `PATCH` para actualizaciones parciales | Más complejo que `PUT`; cubrirlo ahora distrae del CRUD básico |
| Base de datos real (JPA + PostgreSQL) | Aún trabajamos en memoria; el salto a persistencia real viene después |
| Paginación y filtros en el `GET /tickets` | Requiere comprender primero los endpoints individuales |

El foco de esta lección es uno solo: **completar el ciclo de vida de un recurso REST con los cuatro verbos HTTP fundamentales**.

---


## La estructura que tienes al comenzar

```
src/main/java/cl/duoc/fullstack/tickets/
├── controller/
│   └── TicketController.java   ← solo GET y POST
├── model/
│   └── Ticket.java
├── respository/
│   └── TicketRepository.java   ← solo getAll(), existsByTitle(), save()
├── service/
│   └── TicketService.java      ← solo getTickets(), create()
└── TicketsApplication.java
```

Y la estructura que tendrás al terminar:

```
src/main/java/cl/duoc/fullstack/tickets/
├── controller/
│   └── TicketController.java   ← GET, GET/{id}, POST, PUT/{id}, DELETE/{id}
├── model/
│   └── Ticket.java
├── respository/
│   └── TicketRepository.java   ← getAll(), findById(), existsByTitle(), save(), update(), delete()
├── service/
│   └── TicketService.java      ← getTickets(), findById(), create(), update(), delete()
└── TicketsApplication.java
```

Los cambios son incrementales: cada nueva operación agrega un método a cada capa, sin romper lo que ya existe.






<!-- START OF FILE: docs_lessons_06-crud_02_guion_paso_a_paso.md -->
# Documento: docs lessons 06-crud 02 guion paso a paso
---
# Lección 06 - Tutorial paso a paso: CRUD completo de tickets

Sigue esta guía en orden. Vas a extender el proyecto de tickets para que pueda buscar, actualizar y eliminar un ticket por su ID.

---

## Paso 1: entender qué cambios necesitamos

Antes de tocar el código, piensa en lo que falta. Tu API actualmente tiene esto:

```
GET  /tickets        → devuelve la lista completa  (ya existe)
POST /tickets        → crea un ticket nuevo        (ya existe)
```

Y lo que necesita tener al final de esta lección:

```
GET    /tickets             → devuelve la lista completa    (ya existe)
POST   /tickets             → crea un ticket nuevo          (ya existe)
GET    /tickets/by-id/{id}  → busca un ticket por ID        (lo que vamos a construir)
PUT    /tickets/by-id/{id}  → actualiza un ticket           (lo que vamos a construir)
DELETE /tickets/by-id/{id}  → elimina un ticket             (lo que vamos a construir)
```

Para que los tres nuevos endpoints funcionen, necesitas modificar **tres capas**:

1. **`TicketRepository`:** agregar `findById()`, `update()` y `delete()`
2. **`TicketService`:** agregar `findById()`, `update()` y `delete()`
3. **`TicketController`:** agregar los tres nuevos endpoints con `@PathVariable`

El `Model` (`Ticket.java`) **no cambia**: los campos que ya tiene son suficientes para todas estas operaciones.

---

## Paso 2: `Optional<T>` — por qué no retornamos `null`

Antes de escribir el primer método nuevo, hay una decisión de diseño que atraviesa **toda** esta lección: ningún método devolverá `null` para representar "no encontré nada".

### El problema con `null`

Cuando un método devuelve `null`, el código que lo llama **puede olvidar verificarlo**. Si lo olvida, el programa explota en tiempo de ejecución con un `NullPointerException`. Tony Hoare, el inventor del `null`, lo llamó su *"error de mil millones de dólares"*.

```java
// Peligroso: el compilador NO te avisa si olvidas el null check
Ticket ticket = repository.findById(id);
System.out.println(ticket.getTitle()); // NullPointerException si ticket == null
```

### La solución: `Optional<T>`

`Optional<T>` es un contenedor que **puede o no** tener un valor adentro. Lo que lo hace valioso no es el contenedor en sí, sino que **obliga al código que lo recibe a manejar explícitamente el caso "no existe"**. El compilador y el tipo mismo te lo recuerdan.

```java
// Seguro: Optional hace visible la posibilidad de ausencia
Optional<Ticket> ticket = repository.findById(id);
ticket.map(Ticket::getTitle).ifPresent(System.out::println); // nunca explota
```

### Las operaciones clave de Optional

| Operación                | ¿Qué hace?                                                    |
|--------------------------|---------------------------------------------------------------|
| `Optional.of(valor)`     | Crea un Optional con valor (lanza excepción si es null)       |
| `Optional.empty()`       | Crea un Optional vacío                                        |
| `optional.map(fn)`       | Si tiene valor, transforma; si está vacío, devuelve vacío     |
| `optional.orElse(otro)`  | Devuelve el valor si existe, u `otro` si está vacío           |
| `optional.ifPresent(fn)` | Ejecuta la función solo si hay valor                          |
| `optional.isPresent()`   | `true` si tiene valor (evitar: es casi igual a un null check) |

> **¿Cuándo usar `Optional` y cuándo no?**
> `Optional` está diseñado para **valores de retorno** de métodos que pueden no encontrar algo. No debe usarse como parámetro de método ni como campo de una clase: para esos casos hay mejores alternativas. En esta lección lo usarás exactamente donde corresponde: en los retornos de `findById()` y `update()`.

---

## Paso 3: agregar `findById()` al Repository

El `Repository` es quien sabe dónde están guardados los tickets. Abre `TicketRepository` y agrega el método `findById()`:

```java
public Optional<Ticket> findById(Long id) {
    return tickets.stream()
        .filter(t -> t.getId().equals(id))
        .findFirst();
}
```

El stream de `findFirst()` ya devuelve un `Optional<T>` de forma nativa: si encuentra un elemento que pasa el filtro, devuelve `Optional.of(ese elemento)`; si no, devuelve `Optional.empty()`. No hay que hacer nada más.

**Código equivalente sin expresiones lambda:**

```java
public Optional<Ticket> findById(Long id) {
    for (Ticket ticket : tickets) {
        if (ticket.getId().equals(id)) {
            return Optional.of(ticket);
        }
    }
    return Optional.empty();
}
```

Ambas versiones son correctas y producen exactamente el mismo resultado. La versión con stream es más concisa; la versión con `for` es más explícita paso a paso. Cuando trabajes con JPA, `findById()` ya vendrá implementado por el framework y no tendrás que escribir ninguna de las dos.

> **¿Por qué usamos stream aquí y no un `for`?**
> Porque `findFirst()` devuelve `Optional<T>` de forma natural. Escribir el mismo comportamiento con un `for` obligaría a retornar `Optional.of(ticket)` o `Optional.empty()` manualmente al final, que es más verboso sin ningún beneficio. Cada herramienta en su lugar.

> **¿Qué pasa si hay dos tickets con el mismo ID?**
> En nuestro almacenamiento en memoria eso no puede ocurrir porque el ID se asigna con un contador incremental. Pero si ocurriera, `findFirst()` devolvería el primero que encuentre. Cuando migremos a JPA, el motor de base de datos garantiza unicidad con una restricción `PRIMARY KEY`.

---

## Paso 4: agregar `getById()` al Service

El `Service` delega al `Repository` y propaga el `Optional` hacia arriba, sin desnudarlo. No hay reglas de negocio que aplicar en una simple búsqueda por ID.

Abre `TicketService` y agrega:

```java
public Optional<Ticket> getById(Long id) {
    return this.repository.findById(id);
}
```

> **¿Por qué el `Service` no "abre" el Optional aquí?**
> Porque "abrir" el Optional (llamar a `.get()` o `.orElse(null)`) en el `Service` descargaría la responsabilidad de manejar el caso vacío en el `Controller`. Propagar el `Optional` hacia arriba preserva la información de "puede no existir" hasta la capa que sabe qué respuesta HTTP dar. Cada capa hace lo que le corresponde.

---

## Paso 5: agregar `GET /tickets/by-id/{id}` al Controller

Abre `TicketController` y agrega el endpoint:

```java
@GetMapping("/by-id/{id}")
public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
    return service.getById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

**Código equivalente sin expresiones lambda:**

```java
@GetMapping("/by-id/{id}")
public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
    Optional<Ticket> found = service.getById(id);
    if (found.isPresent()) {
        return ResponseEntity.ok(found.get());
    }
    return ResponseEntity.notFound().build();
}
```

Ambas versiones hacen exactamente lo mismo. El `.get()` es seguro aquí porque está protegido por `isPresent()` en la línea anterior.

> **¿Qué hace `@PathVariable`?**
> Captura el valor dinámico que viene en la URL. Si el cliente llama a `GET /tickets/by-id/3`, Spring extrae el `3` de la URL y lo asigna a la variable `id`. Sin `@PathVariable`, el controlador no sabría qué ID está buscando el cliente.

> **¿En qué se diferencia `@PathVariable` de `@RequestParam`?**
> `@PathVariable` extrae valores que forman parte de la estructura de la URL: `/tickets/3`. `@RequestParam` extrae parámetros del query string: `/tickets?id=3`. En REST, los identificadores de recursos van en la URL, no en el query string. Por eso usamos `@PathVariable`.

> **¿Qué hace `.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build())`?**
> Si el `Optional` tiene un ticket adentro, `.map()` lo transforma en `ResponseEntity.ok(ticket)` → `200 OK`. Si está vacío, `.orElse()` devuelve `ResponseEntity.notFound().build()` → `404 Not Found`. Todo sin un solo `if` ni riesgo de `NullPointerException`.

> **¿Por qué `ResponseEntity.notFound().build()` y no `ResponseEntity.notFound().body(...)`?**
> Porque un 404 no lleva cuerpo en esta API: solo comunica que el recurso no existe. El `.build()` construye la respuesta sin body.

---

## Paso 6: agregar `update()` al Repository

El `Repository` necesita saber cómo actualizar un ticket existente. Reutilizamos `findById()` para no duplicar la lógica de búsqueda, y usamos `ifPresent()` para modificar el ticket solo si existe.

Abre `TicketRepository` y agrega:

```java
public Optional<Ticket> update(Long id, Ticket updatedTicket) {
    Optional<Ticket> found = findById(id);
    found.ifPresent(ticket -> {
        ticket.setTitle(updatedTicket.getTitle());
        ticket.setDescription(updatedTicket.getDescription());
        ticket.setStatus(updatedTicket.getStatus());
    });
    return found;
}
```

**Código equivalente sin expresiones lambda:**

```java
public Optional<Ticket> update(Long id, Ticket updatedTicket) {
    Optional<Ticket> found = findById(id);
    if (found.isPresent()) {
        Ticket ticket = found.get();
        ticket.setTitle(updatedTicket.getTitle());
        ticket.setDescription(updatedTicket.getDescription());
        ticket.setStatus(updatedTicket.getStatus());
    }
    return found;
}
```

Nuevamente el `.get()` es seguro porque está dentro del bloque `if (found.isPresent())`.

> **¿Por qué reutilizamos `findById()` en lugar de iterar de nuevo con un `for`?**
> Porque `findById()` ya resuelve el problema de búsqueda y devuelve un `Optional`. Duplicar la lógica de iteración sería una violación del principio DRY (*Don't Repeat Yourself*). Si mañana cambia cómo se busca un ticket (por ejemplo, en una base de datos), solo hay que cambiar `findById()`.

> **¿Qué hace `ifPresent()`?**
> Ejecuta el bloque de código solo si el `Optional` tiene un valor adentro. Si está vacío, no hace nada. Es el equivalente seguro de `if (found != null) { ... }`, pero sin null.

> **¿Por qué no actualizamos el ID?**
> El ID es el identificador único e inmutable del recurso. En REST, el recurso se identifica por su URL: `PUT /tickets/by-id/1` siempre modifica el ticket con ID `1`, independientemente de lo que el body diga sobre el ID.

---

## Paso 7: agregar `updateById()` al Service

```java
public Optional<Ticket> updateById(Long id, Ticket updatedTicket) {
    return this.repository.update(id, updatedTicket);
}
```

En esta lección el `Service` delega directamente al `Repository`. El desafío opcional al final de esta guía propone agregar validaciones aquí.

---

## Paso 8: agregar `PUT /tickets/by-id/{id}` al Controller

```java
@PutMapping("/by-id/{id}")
public ResponseEntity<Ticket> updateTicketById(@PathVariable Long id, @RequestBody Ticket ticket) {
    return service.updateById(id, ticket)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

**Código equivalente sin expresiones lambda:**

```java
@PutMapping("/by-id/{id}")
public ResponseEntity<Ticket> updateTicketById(@PathVariable Long id, @RequestBody Ticket ticket) {
    Optional<Ticket> updated = service.updateById(id, ticket);
    if (updated.isPresent()) {
        return ResponseEntity.ok(updated.get());
    }
    return ResponseEntity.notFound().build();
}
```

> **¿Por qué usamos el `id` de la URL y no el que pudiera venir en el body?**
> Porque la URL identifica el recurso de forma autoritativa. Si el cliente manda `PUT /tickets/by-id/1` con un body que tiene `"id": 99`, eso es una inconsistencia. La URL dice claramente cuál recurso se está modificando. El `id` del body se ignora: el `Repository` actualiza el ticket cuyo `id` coincide con el de la URL.

> **¿Por qué `PUT` devuelve `200 OK` con el ticket actualizado y no `204 No Content`?**
> Porque devolver el recurso actualizado le permite al cliente confirmar que los cambios se aplicaron correctamente, sin necesidad de hacer un `GET` adicional. Aunque la especificación HTTP permite `204` en un `PUT`, devolver `200` con el cuerpo actualizado es más útil en la práctica.

---

## Paso 9: agregar `delete()` al Repository

Para el borrado, el resultado es binario: o se eliminó o no existía. `boolean` es el tipo correcto aquí — no `Optional`, porque no hay ningún valor de retorno significativo si la operación fue exitosa.

Abre `TicketRepository` y agrega:

```java
public boolean delete(Long id) {
    return tickets.removeIf(t -> t.getId().equals(id));
}
```

**Código equivalente sin expresiones lambda:**

```java
public boolean delete(Long id) {
    for (Ticket ticket : tickets) {
        if (ticket.getId().equals(id)) {
            tickets.remove(ticket);
            return true;
        }
    }
    return false;
}
```

`removeIf()` elimina todos los elementos que satisfacen la condición y devuelve `true` si eliminó al menos uno, `false` si la colección no cambió (el ticket no existía).

> **¿Por qué devolvemos `boolean` y no `Optional<Ticket>`?**
> Porque `Optional` está diseñado para "puede haber un valor útil que necesitas". Después de un borrado, el ticket ya no existe: no hay nada que envolver en un `Optional`. `boolean` comunica exactamente lo que importa: ¿se eliminó algo? Usar `Optional` aquí sería forzar el patrón donde no corresponde.

> **¿Por qué `removeIf()` y no el `for` + `remove()` del paso de update?**
> Porque aquí no necesitamos el objeto después de borrarlo. `removeIf()` es la herramienta correcta cuando solo nos interesa la eliminación, no el valor eliminado. Elegir la herramienta correcta para cada caso hace el código más claro e intencional.

---

## Paso 10: agregar `deleteById()` al Service

```java
public boolean deleteById(Long id) {
    return this.repository.delete(id);
}
```

---

## Paso 11: agregar `DELETE /tickets/by-id/{id}` al Controller

```java
@DeleteMapping("/by-id/{id}")
public ResponseEntity<Void> deleteTicketById(@PathVariable Long id) {
    if (!service.deleteById(id)) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
}
```

> **¿Por qué el tipo de retorno es `ResponseEntity<Void>`?**
> Porque una eliminación exitosa no devuelve contenido: solo el código `204 No Content`. `Void` expresa esa intención con claridad: este endpoint nunca tendrá un cuerpo en la respuesta exitosa.

> **¿Por qué `204 No Content` y no `200 OK`?**
> `200 OK` implica que hay un cuerpo con información útil. `204 No Content` dice exactamente lo contrario: la operación fue exitosa, pero no hay nada que devolver. En una eliminación, el recurso ya no existe, por lo que devolver su estado anterior sería incoherente.

---

## Paso 12: el controlador completo

Este es el estado final de `TicketController` al terminar la lección:

```java
package cl.duoc.fullstack.tickets.controller;

import cl.duoc.fullstack.tickets.model.Ticket;
import cl.duoc.fullstack.tickets.service.TicketService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(this.service.getTickets());
    }

    @GetMapping("/by-id/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return service.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody Ticket ticket) {
        try {
            service.create(ticket);
            return ResponseEntity.status(HttpStatus.CREATED).body("Ticket Creado");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/by-id/{id}")
    public ResponseEntity<Ticket> updateTicketById(@PathVariable Long id, @RequestBody Ticket ticket) {
        return service.updateById(id, ticket)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/by-id/{id}")
    public ResponseEntity<Void> deleteTicketById(@PathVariable Long id) {
        if (!service.deleteById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
```

Observa el patrón: donde el recurso puede no existir, el `Optional` del `Service` se convierte directamente en la `ResponseEntity` correcta con `.map().orElse()`. No hay un solo `null` ni un solo `if (x == null)` en el controlador.

---

## Paso 13: verificar que todo funciona

Levanta la aplicación y abre Postman, Insomnia o Thunder Client.

### Prueba 1: obtener todos los tickets

```
GET http://localhost:8080/tickets
```

Resultado esperado: `200 OK` con la lista de 2 tickets semilla.

---

### Prueba 2: obtener un ticket existente

```
GET http://localhost:8080/tickets/by-id/1
```

Resultado esperado (`200 OK`):

```json
{
  "id": 1,
  "title": "Ticket 1",
  "description": "Descripción del ticket 1",
  "status": "NEW",
  "createdAt": "2026-03-15T09:00:00",
  "estimatedResolutionDate": "2026-03-22",
  "effectiveResolutionDate": null
}
```

---

### Prueba 3: obtener un ticket inexistente

```
GET http://localhost:8080/tickets/by-id/999
```

Resultado esperado: `404 Not Found` (sin cuerpo).

---

### Prueba 4: crear un ticket

```
POST http://localhost:8080/tickets
Content-Type: application/json
```

Body:

```json
{
  "title": "Error en dashboard",
  "description": "El gráfico de ventas no carga al filtrar por semana"
}
```

Resultado esperado: `201 Created` con el ticket creado (ID 3, status NEW, fechas asignadas por el servidor).

---

### Prueba 5: actualizar un ticket existente

```
PUT http://localhost:8080/tickets/by-id/1
Content-Type: application/json
```

Body:

```json
{
  "title": "Ticket 1 - Revisado",
  "description": "Descripción actualizada después de la revisión",
  "status": "IN_PROGRESS"
}
```

Resultado esperado: `200 OK` con el ticket actualizado. Observa que `createdAt` y `estimatedResolutionDate` **no cambiaron**.

---

### Prueba 6: actualizar un ticket inexistente

```
PUT http://localhost:8080/tickets/by-id/999
Content-Type: application/json
```

Body:

```json
{
  "title": "Ticket fantasma",
  "description": "Este ticket no existe",
  "status": "NEW"
}
```

Resultado esperado: `404 Not Found`.

---

### Prueba 7: eliminar un ticket existente

```
DELETE http://localhost:8080/tickets/by-id/2
```

Resultado esperado: `204 No Content` (sin cuerpo).

Verifica con un `GET /tickets` que el ticket 2 ya no aparece en la lista.

---

### Prueba 8: eliminar un ticket inexistente

```
DELETE http://localhost:8080/tickets/by-id/999
```

Resultado esperado: `404 Not Found`.

---

## Paso 14: reflexiona antes de cerrar

Antes de pasar a la actividad, respóndete estas preguntas:

1. Si un cliente manda `PUT /tickets/by-id/1` con el body `{"id": 99, "title": "Nuevo título", ...}`, ¿qué ID usa el servidor para buscar el ticket a actualizar? ¿Por qué?
2. Si ejecutas `DELETE /tickets/by-id/1` cinco veces seguidas, ¿qué responde el servidor la segunda, tercera, cuarta y quinta vez? ¿Eso lo hace idempotente?
3. ¿Por qué `findById()` y `update()` devuelven `Optional<Ticket>` mientras que `delete()` devuelve `boolean`? ¿Qué comunica cada tipo de retorno al código que lo llama?
4. ¿Qué pasaría si el `Service` llamara a `repository.findById(id).get()` sin verificar si el `Optional` está vacío? ¿Cuándo y cómo fallaría?

---

## Extensión opcional

Si terminaste todo lo anterior y quieres ir un paso más, agrega validaciones en el `Service` para el `update()`:

- Si el título está vacío o en blanco, que el controlador responda `400 Bad Request`
- Si el estado no es `NEW`, `IN_PROGRESS` o `RESOLVED`, que el controlador responda `400 Bad Request`

Por ahora sin `@Valid` ni Bean Validation. Solo con `if` simples en el `Service` y una excepción propia (o `IllegalArgumentException`) que el controlador capture.






<!-- START OF FILE: docs_lessons_06-crud_03_reglas_rest_e_idempotencia.md -->
# Documento: docs lessons 06-crud 03 reglas rest e idempotencia
---
# Lección 06 - Reglas REST e idempotencia

## Reglas REST que estamos aplicando

Esta lección consolida las reglas REST que hemos venido aplicando desde la lección 03. Ahora que tienes el CRUD completo es un buen momento para revisarlas en conjunto.

---

### Regla 1: URLs en plural y sin verbos

El recurso se llama `Ticket` (singular), pero la URL usa el plural:

```
/tickets
```

Esto se hace porque la URL representa una **colección** de recursos. Usar el singular `/ticket` puede generar ambigüedad: ¿es la colección o un elemento?

**Correcto:**

```
GET    /tickets
GET    /tickets/1
POST   /tickets
PUT    /tickets/1
DELETE /tickets/1
```

**Incorrecto:**

```
GET  /getTickets
POST /createTicket
PUT  /updateTicket/1
GET  /obtenerTicketPorId/1
```

La **acción** la indica el verbo HTTP, no la URL. La URL solo identifica el recurso.

---

### Regla 2: la acción la define el método HTTP

El mismo endpoint `/tickets/1` puede hacer cosas diferentes dependiendo del método:

| Método | Endpoint        | Acción                       |
|--------|-----------------|------------------------------|
| GET    | `/tickets/1`    | Obtener el ticket con ID 1   |
| PUT    | `/tickets/1`    | Actualizar el ticket con ID 1|
| DELETE | `/tickets/1`    | Eliminar el ticket con ID 1  |

Si pusieras la acción en la URL, tendrías que crear una URL diferente por cada operación. Eso rompe el diseño REST.

---

### Regla 3: el código de estado debe reflejar lo que ocurrió

Una API no debe responder siempre `200 OK`. El código de estado le dice al cliente exactamente qué pasó.

| Situación                            | Código HTTP         |
|--------------------------------------|---------------------|
| Consulta o actualización exitosa     | `200 OK`            |
| Ticket creado correctamente          | `201 Created`       |
| Eliminación exitosa                  | `204 No Content`    |
| Ticket no encontrado                 | `404 Not Found`     |
| Título duplicado al crear            | `409 Conflict`      |

Si tu API devuelve `200 OK` para todo, el cliente no puede saber si algo falló. Un cliente bien diseñado (o un desarrollador que usa Postman) depende de estos códigos para saber cómo actuar.

---

### Regla 4: el ID del recurso va en la URL, no en el body

Cuando haces `PUT /tickets/1`, el ID `1` identifica **qué recurso estás modificando**. Es parte de la URL.

Si el body también trae un `id`, el servidor debe ignorarlo (o asumir que el de la URL es el correcto). No debes confiar en el `id` del body para decidir qué ticket actualizar.

```java
// Correcto: usas el id de la URL
@PutMapping("/{id}")
public ResponseEntity<Ticket> update(@PathVariable Long id, @RequestBody Ticket ticket) {
    Ticket updatedTicket = service.update(id, ticket); // el id viene de la URL
    ...
}
```

---

## Idempotencia explicada con tickets

### ¿Qué significa idempotente?

Una operación es **idempotente** si ejecutarla una vez produce el mismo resultado que ejecutarla múltiples veces.

En otras palabras: no importa cuántas veces repitas la operación, el **estado final** del sistema siempre es el mismo.

---

### `PUT` es idempotente

Supón que haces esta petición:

```
PUT /tickets/1
Content-Type: application/json

{
  "title": "Ticket 1 - Revisado",
  "description": "Descripción actualizada",
  "status": "IN_PROGRESS"
}
```

Si la ejecutas una vez, el ticket 1 queda con ese título, descripción y estado.

Si la ejecutas cinco veces más, el ticket 1 **sigue quedando exactamente igual**: mismo título, misma descripción, mismo estado.

El resultado final no cambia sin importar cuántas veces repitas la petición. Por eso `PUT` es idempotente.

**Idea clave:** `PUT` reemplaza el estado del recurso hacia un valor definido. No acumula cambios: reemplaza.

---

### `DELETE` también es idempotente

```
DELETE /tickets/2
```

La primera vez: el ticket 2 se elimina → `204 No Content`.

La segunda vez: el ticket 2 ya no existe → `404 Not Found`.

Aunque el código de respuesta cambia, el **estado del sistema** es el mismo en ambos casos: el ticket 2 no existe. Por eso `DELETE` se considera idempotente.

---

### `POST` **no** es idempotente

```
POST /tickets
{
  "title": "Error en login",
  "description": "..."
}
```

Cada vez que ejecutas esta petición, se crea un ticket nuevo (si el título no existía) o se rechaza con `409` (si ya existe). El resultado puede variar, y si el título es distinto cada vez, crearás múltiples tickets nuevos.

Por eso `POST` normalmente no es idempotente.

---

### Resumen de idempotencia

| Método | ¿Idempotente? | Razón                                                  |
|--------|---------------|--------------------------------------------------------|
| GET    | ✅ Sí          | Solo lee, nunca modifica el estado                     |
| PUT    | ✅ Sí          | Reemplaza el recurso hacia un estado definido          |
| DELETE | ✅ Sí          | El resultado final es siempre "el recurso no existe"   |
| POST   | ❌ No          | Puede crear múltiples recursos si se repite            |

---

## Códigos HTTP de esta lección: resumen visual

```
GET /tickets          → 200 OK         (lista de tickets)
GET /tickets/1        → 200 OK         (ticket encontrado)
GET /tickets/999      → 404 Not Found  (ticket no existe)

POST /tickets         → 201 Created    (ticket creado)
POST /tickets         → 409 Conflict   (título duplicado)

PUT /tickets/1        → 200 OK         (ticket actualizado)
PUT /tickets/999      → 404 Not Found  (ticket no existe)

DELETE /tickets/2     → 204 No Content (ticket eliminado)
DELETE /tickets/999   → 404 Not Found  (ticket no existe)
```






<!-- START OF FILE: docs_lessons_06-crud_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 06-crud 04 checklist rubrica minima
---
# Lección 06 - Checklist y rúbrica mínima

Usa esta lista para verificar que implementaste correctamente el CRUD completo de tickets antes de dar la lección por terminada.

---

## Checklist de endpoints

| Endpoint             | Método | ¿Implementado? | Código exitoso | Código de error |
|----------------------|--------|----------------|----------------|-----------------|
| `/tickets`           | GET    | ☐              | 200 OK         | —               |
| `/tickets/{id}`      | GET    | ☐              | 200 OK         | 404 Not Found   |
| `/tickets`           | POST   | ☐              | 201 Created    | 409 Conflict    |
| `/tickets/{id}`      | PUT    | ☐              | 200 OK         | 404 Not Found   |
| `/tickets/{id}`      | DELETE | ☐              | 204 No Content | 404 Not Found   |

---

## Checklist de código

### Model (`Ticket.java`)

- ☐ Tiene `@NoArgsConstructor` para que Jackson pueda deserializar el body del POST y PUT
- ☐ Tiene los campos: `id`, `title`, `description`, `status`, `createdAt`, `estimatedResolutionDate`, `effectiveResolutionDate`

### Repository (`TicketRepository.java`)

- ☐ Tiene `findById(Long id)` que devuelve `Optional<Ticket>` (usa stream + `findFirst()`, no retorna `null`)
- ☐ Tiene `update(Long id, Ticket updatedTicket)` que devuelve `Optional<Ticket>` (reutiliza `findById()` + `ifPresent()`)
- ☐ Tiene `delete(Long id)` que devuelve `boolean` (usa `removeIf()`)
- ☐ El contador de IDs (`currentId`) empieza en `3L` para no colisionar con los tickets semilla

### Service (`TicketService.java`)

- ☐ Tiene `findById(Long id)` que devuelve `Optional<Ticket>` y propaga el Optional del Repository hacia arriba
- ☐ Tiene `update(Long id, Ticket updatedTicket)` que devuelve `Optional<Ticket>`
- ☐ Tiene `delete(Long id)` que devuelve `boolean`
- ☐ Ningún método llama a `.get()` ni a `.orElse(null)` en el Optional (eso se hace en el Controller)
- ☐ El método `create()` sigue asignando `status`, `createdAt`, `estimatedResolutionDate` y `effectiveResolutionDate` en el servidor

### Controller (`TicketController.java`)

- ☐ `GET /tickets/{id}` usa `@GetMapping("/{id}")` y `@PathVariable Long id`
- ☐ `PUT /tickets/{id}` usa `@PutMapping("/{id}")` con `@PathVariable` y `@RequestBody`
- ☐ `DELETE /tickets/{id}` usa `@DeleteMapping("/{id}")` y devuelve `ResponseEntity<Void>`
- ☐ Todos los endpoints usan `ResponseEntity` con el código correcto
- ☐ Los endpoints de GET y PUT usan `.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build())` (sin `if (x == null)`)
- ☐ El DELETE exitoso devuelve `ResponseEntity.noContent().build()`
- ☐ No hay ningún `null` explícito en el controlador

---

## Checklist de reglas REST

- ☐ La URL del recurso es `/tickets` (plural, sin verbos)
- ☐ No hay rutas con verbos como `/getTicket`, `/deleteTicket`, `/updateTicket`
- ☐ El ID del recurso en PUT viene de la URL (`@PathVariable`), no del body
- ☐ Los códigos de estado son correctos para cada situación

---

## Checklist de pruebas

Hiciste las siguientes pruebas en Postman/Thunder Client:

- ☐ `GET /tickets` devuelve los 2 tickets semilla → `200 OK`
- ☐ `GET /tickets/by-id/1` devuelve el primer ticket → `200 OK`
- ☐ `GET /tickets/by-id/999` → `404 Not Found`
- ☐ `POST /tickets` con título nuevo → `201 Created` (el servidor asignó ID, status y fechas)
- ☐ `POST /tickets` con el mismo título → `409 Conflict`
- ☐ `PUT /tickets/by-id/1` con datos nuevos → `200 OK` (fechas no cambiaron)
- ☐ `PUT /tickets/by-id/999` → `404 Not Found`
- ☐ `DELETE /tickets/by-id/2` → `204 No Content`
- ☐ `DELETE /tickets/by-id/999` → `404 Not Found`
- ☐ `GET /tickets` después del DELETE → el ticket eliminado ya no aparece

---

## Errores comunes a evitar

| Error | Por qué está mal | Cómo corregirlo |
|---|---|---|
| Devolver `null` desde Repository o Service | Puede causar `NullPointerException` si el llamador olvida verificar | Usa `Optional.empty()` o `Optional.of(valor)` |
| Llamar a `optional.get()` sin verificar | Si el Optional está vacío, lanza `NoSuchElementException` | Usa `.map()`, `.orElse()` o `.ifPresent()` |
| Usar el `id` del body en el PUT | El ID autoritativo es el de la URL | Usa solo `@PathVariable Long id` para buscar el ticket |
| Devolver `200` en el DELETE | Una eliminación exitosa no devuelve contenido | Usa `ResponseEntity.noContent().build()` (`204`) |
| Devolver `200` cuando no existe el recurso | `200` indica éxito; un recurso inexistente es un error | Usa `ResponseEntity.notFound().build()` (`404`) |
| Poner verbos en la URL | El verbo HTTP ya indica la acción | Usa `DELETE /tickets/1` en vez de `GET /deleteTicket/1` |
| No probar casos negativos | La API debe responder correctamente a errores también | Prueba siempre con IDs que no existen |






<!-- START OF FILE: docs_lessons_06-crud_05_actividad_individual.md -->
# Documento: docs lessons 06-crud 05 actividad individual
---
# Lección 06 - Actividad individual: CRUD de categorías

## Contexto

En la lección anterior (POST) ya tenías una actividad sobre `Category`. En esta lección vas a completar ese trabajo implementando el **CRUD completo** para ese mismo recurso.

Si en la lección anterior implementaste `Category` con sus propios atributos, adapta la actividad a lo que ya tienes. Si no la implementaste, esta es tu oportunidad.

---

## ¿Qué vas a construir?

Una API REST para gestionar categorías de tickets. Cada categoría agrupa tickets por tipo de problema: `"Hardware"`, `"Software"`, `"Acceso"`, etc.

### Atributos mínimos del recurso `Category`

| Campo         | Tipo     | Descripción                                         |
|---------------|----------|-----------------------------------------------------|
| `id`          | `Long`   | Identificador único asignado por el servidor        |
| `name`        | `String` | Nombre de la categoría (ej: `"Hardware"`)           |
| `description` | `String` | Descripción breve del tipo de problemas que agrupa  |

---

## Endpoints requeridos

| Método | Endpoint           | Descripción                              | Código exitoso | Código de error |
|--------|--------------------|------------------------------------------|----------------|-----------------|
| GET    | `/categories`      | Devuelve todas las categorías            | 200 OK         | —               |
| GET    | `/categories/{id}` | Devuelve una categoría por ID            | 200 OK         | 404 Not Found   |
| POST   | `/categories`      | Crea una nueva categoría                 | 201 Created    | 409 Conflict    |
| PUT    | `/categories/{id}` | Actualiza una categoría existente        | 200 OK         | 404 Not Found   |
| DELETE | `/categories/{id}` | Elimina una categoría existente          | 204 No Content | 404 Not Found   |

---

## Estructura de archivos esperada

```
src/main/java/cl/duoc/fullstack/tickets/
├── controller/
│   ├── TicketController.java      (ya existe)
│   └── CategoryController.java    (nuevo)
├── model/
│   ├── Ticket.java                (ya existe)
│   └── Category.java              (nuevo)
├── respository/
│   ├── TicketRepository.java      (ya existe)
│   └── CategoryRepository.java    (nuevo)
├── service/
│   ├── TicketService.java         (ya existe)
│   └── CategoryService.java       (nuevo)
└── TicketsApplication.java
```

---

## Reglas de negocio mínimas

Implementa estas reglas **en el `Service`**, igual que hiciste con los tickets:

1. **No se pueden crear dos categorías con el mismo nombre** (comparación sin distinguir mayúsculas/minúsculas) → `409 Conflict`
2. **El ID lo asigna el servidor**, nunca el cliente
3. **Los datos semilla:** crea al menos 2 categorías de ejemplo en el constructor del `Repository`

---

## Guía de implementación

Sigue exactamente el mismo patrón que usaste para `Ticket`:

### 1. `Category.java`
- Usa Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Campos: `id`, `name`, `description`

### 2. `CategoryRepository.java`
- Lista en memoria como almacenamiento
- Contador incremental para los IDs
- Métodos:
  - `getAll()` → `List<Category>`
  - `findById(Long id)` → `Optional<Category>` (usa stream + `findFirst()`, sin `null`)
  - `existsByName(String name)` → `boolean`
  - `save(Category category)` → `Category`
  - `update(Long id, Category category)` → `Optional<Category>` (reutiliza `findById()` + `ifPresent()`)
  - `delete(Long id)` → `boolean` (usa `removeIf()`)

### 3. `CategoryService.java`
- Validación de nombre duplicado en `create()`
- `findById()` devuelve `Optional<Category>` sin llamar a `.get()` ni `.orElse(null)`
- `update()` devuelve `Optional<Category>` y delega al Repository
- `delete()` devuelve `boolean` y delega al Repository

### 4. `CategoryController.java`
- `@RestController` + `@RequestMapping("/categories")`
- Un método por endpoint
- `ResponseEntity` en todos los métodos con los códigos correctos
- GET y PUT usan `.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build())`
- No hay ningún `null` explícito en el controlador

---

## Ejemplos de prueba

### Crear una categoría

```
POST http://localhost:8080/categories
Content-Type: application/json

{
  "name": "Hardware",
  "description": "Problemas relacionados con componentes físicos"
}
```

Resultado esperado: `201 Created`

```json
{
  "id": 3,
  "name": "Hardware",
  "description": "Problemas relacionados con componentes físicos"
}
```

### Actualizar una categoría

```
PUT http://localhost:8080/categories/1
Content-Type: application/json

{
  "name": "Hardware y periféricos",
  "description": "Problemas con hardware, teclados, monitores y otros periféricos"
}
```

Resultado esperado: `200 OK` con la categoría actualizada.

### Eliminar una categoría

```
DELETE http://localhost:8080/categories/2
```

Resultado esperado: `204 No Content`

---

## Desafío opcional

Si terminaste antes, agrega una validación para impedir:

- Nombres vacíos o en blanco → `400 Bad Request`
- Descripciones de menos de 10 caracteres → `400 Bad Request`

Impleméntalas como `if` simples en el `Service`, lanzando `IllegalArgumentException`. El `Controller` las captura y devuelve `ResponseEntity.badRequest().build()`.

---

## Criterios de evaluación

| Criterio                                                         | Puntaje |
|------------------------------------------------------------------|---------|
| Los 5 endpoints están implementados y responden correctamente    | 40%     |
| Los códigos HTTP son correctos para éxito y error               | 20%     |
| La estructura de capas es correcta (no hay lógica de negocio en el Controller) | 20%     |
| Las URLs siguen las reglas REST (plural, sin verbos)             | 10%     |
| Se probaron los casos negativos (IDs inexistentes, nombre duplicado) | 10%  |




