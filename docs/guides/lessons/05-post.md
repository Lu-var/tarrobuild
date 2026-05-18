<!-- START OF FILE: docs_lessons_05-post_01_objetivo_y_alcance.md -->
# Documento: docs lessons 05-post 01 objetivo y alcance
---
# Lección 05 - POST y creación de recursos: ¿qué vas a aprender?

## ¿De dónde venimos?

En la lección anterior construiste una API con arquitectura por capas: `Controller`, `Service`, `Repository` y `Model`. El endpoint que expusiste fue `GET /tickets`, que devolvía una lista de tickets almacenados en memoria.

Era una API que solo sabía leer. Funciona, y la estructura era correcta, pero en la práctica una API que solo lee sirve de muy poco: los datos tienen que entrar desde algún lugar.

Esta lección existe para resolver eso.

---

## ¿Qué vas a construir?

Al terminar esta lección habrás extendido tu API de tickets para que también sea capaz de **recibir y guardar información nueva**. Concretamente:

- Agregarás el endpoint `POST /tickets` al controlador existente
- Recibirás datos JSON desde el cliente usando `@RequestBody`
- Asignarás IDs de forma automática dentro del `Repository`
- Devolverás el ticket creado con el código de estado correcto: `201 Created`

### Lo que vas a ser capaz de explicar

Más que ejecutar el código, el objetivo es que entiendas cada decisión. Al terminar deberías poder responder:

- ¿Para qué sirve `@RequestBody` y qué problema resuelve?
- ¿Por qué el servidor asigna el ID y no el cliente?
- ¿Por qué una creación exitosa responde `201` y no `200`?
- ¿Qué diferencia hay entre devolver un objeto directamente y usar `ResponseEntity`?
- ¿Por qué el modelo necesita un constructor vacío para que `@RequestBody` funcione?

---

## ¿Qué requerimientos implementamos en esta lección?

> El proyecto completo está descrito en [`00_enunciado_proyecto.md`](../00_enunciado_proyecto.md).
> Ahí encontrarás el escenario, los actores y la lista completa de requerimientos numerados.

De esa lista, esta lección implementa **cinco**:

| Requerimiento | Lo que construimos |
|---------------|--------------------|
| **REQ-02** — Registrar un nuevo ticket con título y descripción | El endpoint `POST /tickets` con `@RequestBody` |
| **REQ-03** — Estado inicial `NEW` automático | El `Service` asigna `status = "NEW"` al crear |
| **REQ-04** — Sin títulos duplicados | El `Service` valida con `existsByTitle()` antes de guardar |
| **REQ-05** — Fecha y hora de creación automática | El `Service` asigna `createdAt = LocalDateTime.now()` |
| **REQ-06** — Fecha estimada de resolución | El `Service` calcula `estimatedResolutionDate = hoy + 5 días` |

Nota que REQ-03 a REQ-06 **no los envía el cliente** en el body del `POST`. Los calcula y asigna el servidor. Eso no es un detalle técnico: es una regla de negocio, y el lugar correcto para esa lógica es el `Service`.

---

## ¿Qué NO cubre esta lección? (y por qué)

Hay cosas que intencionalmente dejamos para más adelante:

| Tema | ¿Por qué lo dejamos después? |
|---|---|
| Validaciones (`@Valid`, `@NotNull`, `@NotBlank`) | Primero entendemos el flujo básico de creación; las validaciones son una capa adicional |
| Manejo global de errores (`@ControllerAdvice`) | Requiere conocer las excepciones que puede lanzar una API; lo trabajaremos cuando tengamos más endpoints |
| IDs auto-generados por la base de datos | Aún no usamos JPA ni PostgreSQL; la estrategia manual es suficiente para esta etapa |
| `PUT`, `PATCH` y `DELETE` | Completaremos el CRUD una vez que `POST` esté dominado |
| UUID como identificador | Agrega complejidad sin aportar valor en esta etapa del aprendizaje |

El foco de esta lección es uno solo: **entender cómo entra información a la API y cómo se responde correctamente cuando algo se crea**.

---

## El problema que resuelve `POST`

Hasta ahora, los datos de tu API estaban cargados de forma fija en el constructor del `TicketRepository`. Eso funcionaba para probar el `GET`, pero tiene un problema obvio: nadie puede agregar tickets nuevos mientras la aplicación está corriendo.

El método `POST` es la solución. Cuando un cliente quiere crear un recurso nuevo, envía los datos en el cuerpo de la petición HTTP y tu API los recibe, los procesa y los persiste (en memoria por ahora).

El flujo completo de una petición `POST` es:

```
Cliente → POST /tickets (con body JSON)
       → TicketController.create()
       → TicketService.create()
       → TicketRepository.save()
       → [ ticket con ID asignado ]
       → 201 Created (con el ticket creado en el body)
```

Cada capa sigue haciendo exactamente lo mismo que en la lección anterior, con la diferencia de que ahora el dato entra desde afuera en lugar de estar hardcodeado.

---

## La idea central de esta lección

> "El código de estado HTTP no es un detalle de implementación. Es parte del contrato de tu API."

Devolver `200 OK` cuando el usuario espera `201 Created` no es solo incorrecto semánticamente: es un error de comunicación. Tu API le está mintiendo al cliente sobre lo que acaba de ocurrir. Desde esta lección, el código de respuesta siempre será explícito y correcto.






<!-- START OF FILE: docs_lessons_05-post_02_guion_paso_a_paso.md -->
# Documento: docs lessons 05-post 02 guion paso a paso
---
# Lección 05 - Tutorial paso a paso: agregando POST a tu API

Sigue esta guía en orden. Vas a extender el proyecto de tickets que construiste en la lección anterior, agregando la capacidad de crear nuevos tickets a través de una petición `POST`.

---

## Paso 1: entender qué cambios necesitamos

Antes de tocar el código, piensa en lo que falta. Tu API actualmente tiene esto:

```
GET /tickets → devuelve la lista completa
```

Y lo que necesita tener:

```
GET  /tickets → devuelve la lista completa         (ya existe)
POST /tickets → recibe un ticket nuevo y lo guarda (lo que vamos a construir)
```

Para que el `POST` funcione, necesitas modificar **cuatro capas**:

1. **`Ticket` (Model):** agregar un constructor vacío y tres nuevos campos de fecha
2. **`TicketRepository`:** agregar `existsByTitle()` para validar duplicados y el método `save()` con ID incremental
3. **`TicketService`:** agregar `create()` con toda la lógica de negocio (validación, estado, fechas)
4. **`TicketController`:** agregar el endpoint `@PostMapping` con `@RequestBody` y `ResponseEntity`

La separación de capas hace que los cambios estén bien localizados: cada capa se modifica por su propia razón, no por razones de otra capa.

---

## Paso 2: preparar el Modelo (`Ticket.java`)

Abre la clase `Ticket` en el paquete `model`. Necesita dos cambios: un constructor vacío para que Jackson pueda deserializar el JSON entrante, y tres nuevos campos para representar el ciclo de vida del ticket en el tiempo.

```java
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
```

> **¿Por qué `@NoArgsConstructor`?**
> Spring usa Jackson para convertir el JSON del cliente en un objeto Java. El proceso es: Jackson crea una instancia vacía (`new Ticket()`) y luego llama a los setters campo por campo. Sin `@NoArgsConstructor`, ese primer paso falla y la petición devuelve un error `400 Bad Request` confuso sobre deserialización.

> **¿Por qué conservamos `@AllArgsConstructor`?**
> Porque el `TicketRepository` lo sigue usando para construir los tickets semilla con todos sus campos. Ambas anotaciones coexisten sin problema: Java permite múltiples constructores con diferentes firmas.

> **¿Por qué `LocalDate` para la estimada y `LocalDateTime` para las otras?**
> La fecha de creación y de resolución efectiva necesitan precisión de hora y minuto: importa saber a qué hora exacta ocurrió cada evento. La fecha estimada, en cambio, es una fecha de vencimiento: no importa la hora, solo el día. `LocalDate` comunica esa intención con más precisión que un `LocalDateTime` donde la hora sería siempre `00:00`.

> **¿El cliente manda estos campos en el POST?**
> No. El cliente solo manda `title` y `description`. Los campos `status`, `createdAt`, `estimatedResolutionDate` y `effectiveResolutionDate` los asigna exclusivamente el servidor. Si el cliente los incluye en el JSON, el servidor los ignora y los sobreescribe. Esa es la lógica de negocio que vive en el `Service`.

---

## Paso 3: agregar `existsByTitle()` y `save()` al Repository (`TicketRepository.java`)

El `Repository` cumple dos responsabilidades nuevas: verificar si un título ya existe, y persistir un ticket nuevo con un ID generado automáticamente.

Reemplaza el contenido de `TicketRepository` con lo siguiente:

```java
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TicketRepository {

    private List<Ticket> tickets;
    private Long currentId = 3L;

    public TicketRepository() {
        tickets = new ArrayList<>();
        tickets.add(new Ticket(
            1L, "Ticket 1", "Descripción del ticket 1", "NEW",
            LocalDateTime.of(2026, 3, 15, 9, 0),
            LocalDate.of(2026, 3, 22),
            null
        ));
        tickets.add(new Ticket(
            2L, "Ticket 2", "Descripción del ticket 2", "RESOLVED",
            LocalDateTime.of(2026, 3, 10, 14, 30),
            LocalDate.of(2026, 3, 17),
            LocalDateTime.of(2026, 3, 16, 11, 0)
        ));
    }

    public List<Ticket> getAll() {
        return tickets;
    }

    public boolean existsByTitle(String title) {
        return tickets.stream()
            .anyMatch(t -> t.getTitle().equalsIgnoreCase(title));
    }

    public Ticket save(Ticket ticket) {
        ticket.setId(currentId++);
        tickets.add(ticket);
        return ticket;
    }
}
```

> **¿Por qué `currentId` empieza en `3L`?**
> Los tickets semilla ya ocupan los IDs `1` y `2`. Empezar en `3` garantiza que no haya colisión de IDs.

> **¿Por qué `existsByTitle()` usa `equalsIgnoreCase()`?**
> Para que `"login falla"`, `"Login Falla"` y `"LOGIN FALLA"` sean considerados el mismo título. Un usuario que comete un error de capitalización no debería poder crear un ticket duplicado. La comparación sin distinción de mayúsculas es más robusta y más amigable.

**Código equivalente sin expresiones lambda:**

```java
public boolean existsByTitle(String title) {
    for (Ticket ticket : tickets) {
        if (ticket.getTitle().equalsIgnoreCase(title)) {
            return true;
        }
    }
    return false;
}
```

El `for` recorre cada ticket y retorna `true` en cuanto encuentra un título coincidente. Si termina el recorrido sin encontrar ninguno, retorna `false`. El stream con `anyMatch` hace exactamente lo mismo con menos líneas.

> **¿Por qué esta validación vive en el `Repository` y no en el `Service`?**
> La consulta de si algo existe en el almacenamiento es responsabilidad del `Repository`: es quien sabe dónde y cómo están guardados los datos. Pero la *decisión* de qué hacer si existe un duplicado (lanzar una excepción, ignorar, etc.) es responsabilidad del `Service`. El `Repository` solo responde la pregunta; el `Service` toma la acción.

> **Los datos semilla ahora tienen fechas realistas:** el Ticket 1 está abierto (`effectiveResolutionDate: null`), el Ticket 2 ya fue resuelto antes de su fecha estimada. Esto permite probar el `GET` con datos que reflejan ambos estados posibles de un ticket.

---

## Paso 4: agregar `create()` al Service (`TicketService.java`)

El `Service` es donde vive toda la lógica de negocio de la creación. Esta es la capa más importante de este paso: aquí se concentra todo lo que el servidor decide de forma autónoma, sin depender de lo que el cliente mande.

Abre `TicketService` y agrega el método `create()`:

```java
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    public List<Ticket> getTickets() {
        return this.repository.getAll();
    }

    public Ticket create(Ticket ticket) {
        if (repository.existsByTitle(ticket.getTitle())) {
            throw new IllegalArgumentException(
                "Ya existe un ticket con el título: \"" + ticket.getTitle() + "\""
            );
        }

        ticket.setStatus("NEW");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setEstimatedResolutionDate(LocalDate.now().plusDays(5));
        ticket.setEffectiveResolutionDate(null);

        return this.repository.save(ticket);
    }
}
```

> **¿Por qué el `Service` lanza una excepción en lugar de devolver `null` o `false`?**
> Porque una excepción comunica explícitamente que ocurrió algo inesperado e impide que el flujo continúe. Si devolviéramos `null`, el controlador tendría que verificar si el resultado es nulo y tomar la decisión, lo que mezcla lógica de negocio con lógica de presentación HTTP. La excepción fuerza al controlador a manejar el error de forma explícita.

> **¿Por qué el `Service` asigna el `status` en lugar de recibirlo del cliente?**
> Porque "un ticket recién creado siempre empieza como `NEW`" es una **regla de negocio**. Si el cliente pudiera mandar `"status": "RESOLVED"` y el servidor lo aceptara, cualquier usuario podría resolver un ticket sin haberlo trabajado. El servidor tiene la autoridad sobre su propio estado interno.

> **¿Por qué el `Service` calcula la fecha estimada (y no el cliente)?**
> Por el mismo principio: la regla "la resolución estimada es 5 días después de la creación" es lógica de negocio. Si el cliente calculara esa fecha, cada cliente podría mandar una fecha diferente. Centralizar el cálculo en el `Service` garantiza que la regla se aplique de forma consistente sin importar desde dónde se cree el ticket.

> **¿Por qué `effectiveResolutionDate` se asigna como `null`?**
> Porque en el momento de la creación el ticket aún no está resuelto. Esta fecha se asignará en el futuro, cuando se implemente el endpoint de actualización de estado (`PUT /tickets/{id}`). Por ahora, dejarla como `null` es el estado correcto para un ticket nuevo.

---

## Paso 5: agregar el endpoint `POST` al Controller (`TicketController.java`)

El controlador recibe la petición, llama al `Service` y devuelve la respuesta apropiada. Incluye manejo de la excepción de duplicado porque, mientras no tengamos `@ControllerAdvice`, esta es la única forma de interceptarla antes de que Spring devuelva un `500 Internal Server Error`.

Reemplaza el contenido de `TicketController` con lo siguiente:

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<Ticket> getAllTickets() {
        return this.service.getTickets();
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
}
```

> **¿Por qué `ResponseEntity<Object>` y no `ResponseEntity<Ticket>`?**
> Porque el método puede retornar dos tipos distintos: un `Ticket` (cuando todo sale bien) o un `String` con el mensaje de error (cuando hay duplicado). Java no permite que un método genérico retorne dos tipos diferentes, así que usamos `Object` como tipo común. Esta es una limitación temporal: cuando implementemos `@ControllerAdvice` en lecciones futuras, el controlador volverá a tener `ResponseEntity<Ticket>` y el manejo de errores vivirá en una clase dedicada.

> **¿Por qué `409 Conflict` para el duplicado?**
> El estándar HTTP define `409 Conflict` para situaciones donde la petición no puede completarse por un conflicto con el estado actual del recurso. Crear un ticket con un título que ya existe es exactamente eso: la petición entra en conflicto con un dato que ya existe. Es más preciso que `400 Bad Request` (que indica que el formato del request está mal) o `422 Unprocessable Entity` (que indica que la entidad no puede procesarse).

> **¿Por qué el `try/catch` está en el `Controller` y no en el `Service`?**
> Porque la decisión de qué código HTTP devolver es responsabilidad del controlador. El `Service` solo sabe que algo salió mal (por eso lanza la excepción). El `Controller` es quien sabe cómo traducir ese error a un código HTTP. Cada capa hace lo que le corresponde.

---

## Paso 6: verificar que todo funciona

Levanta la aplicación y abre Postman, Insomnia o Thunder Client.

### Prueba 1: crear un ticket nuevo

Haz una petición `POST` a:

```
POST http://localhost:8080/tickets
Content-Type: application/json
```

Con el siguiente body. **Nota:** solo mandas `title` y `description`. El servidor se encarga de todo lo demás.

```json
{
  "title": "Login falla con usuario especial",
  "description": "El sistema no permite el acceso con caracteres especiales en el nombre de usuario"
}
```

Resultado esperado (`201 Created`):

```
Ticket Creado
```

Observa que:
- La respuesta es un texto plano confirmando la creación, no el objeto completo
- Internamente, el servidor asignó `id`, `status = "NEW"`, `createdAt` y `estimatedResolutionDate` (5 días después)
- Puedes verificar el ticket creado con `GET /tickets`

### Prueba 2: intentar crear un ticket con el mismo título

Vuelve a mandar el mismo POST con el mismo título:

```json
{
  "title": "Login falla con usuario especial",
  "description": "Otro intento con el mismo título"
}
```

Resultado esperado (`409 Conflict`):

```
Ya existe un ticket con el título: "Login falla con usuario especial"
```

El servidor rechaza la creación porque ya existe un ticket con ese título. El mensaje viene directamente de la excepción lanzada en el `Service`.

### Prueba 3: verificar que el GET refleja el estado correcto

```
GET http://localhost:8080/tickets
```

Deberías ver los 3 tickets: los 2 semilla más el que acabas de crear. Los semilla tienen `status = "NEW"` y el nuevo también.

---

## Paso 7: reflexiona antes de cerrar

Antes de pasar a la actividad, respóndete estas preguntas:

1. El cliente mandó un JSON sin el campo `status`. ¿Qué valor tiene `status` en el objeto `Ticket` cuando llega al `Service`? ¿Qué pasa si el cliente sí lo manda con `"status": "RESOLVED"`?
2. Si mañana la regla de negocio cambia y la fecha estimada pasa de 5 días a 10 días hábiles, ¿qué archivo modificarías? ¿Tendrías que tocar el `Controller` o el `Repository`?
3. ¿Por qué el `try/catch` está en el `Controller` y no en el `Service`? ¿Qué pasaría si lo pusieras en el `Service`?

---

## Extensión opcional

Si terminaste todo lo anterior y quieres ir un paso más, implementa el endpoint de resolución de un ticket:

```
PUT /tickets/by-id/{id}/resolve
```

- Busca el ticket por `id` en el `Repository`
- Si no existe, devuelve `404 Not Found`
- Si ya está `"RESOLVED"`, devuelve `409 Conflict` con un mensaje claro
- Si existe y está `"NEW"`, cambia el `status` a `"RESOLVED"` y asigna `effectiveResolutionDate = LocalDateTime.now()`
- Devuelve `200 OK` con el ticket actualizado

Este es el momento en que `effectiveResolutionDate` deja de ser `null`. Toda la lógica de ese cambio de estado vive en el `Service`.





<!-- START OF FILE: docs_lessons_05-post_03_decisiones_post_y_http.md -->
# Documento: docs lessons 05-post 03 decisiones post y http
---
# Lección 05 - Por qué hacemos las cosas así: decisiones de diseño explicadas

Esta sección no es un listado de reglas. Es la explicación del razonamiento detrás de cada decisión que tomamos al agregar el `POST` a nuestra API. Un buen desarrollador no solo sabe *qué* hacer, sino *por qué* lo hace así y no de otra manera.

---

## Decisión 1: `201 Created` en lugar de `200 OK`

Esta es la decisión más visible de la lección y la que más errores comete la gente al principio.

El protocolo HTTP define los códigos de estado con precisión. No son sugerencias: son un contrato entre el servidor y el cliente. La diferencia entre `200` y `201` no es cosmética:

| Código | Nombre | Significado |
|---|---|---|
| `200 OK` | OK | La petición fue exitosa. Se usa para consultas (`GET`) o actualizaciones genéricas. |
| `201 Created` | Created | La petición fue exitosa **y** como resultado se creó un nuevo recurso. |

Cuando tu `POST /tickets` devuelve `200 OK`, le estás diciendo al cliente: "todo salió bien, pero no sé bien qué pasó". Cuando devuelves `201 Created`, le estás diciendo: "todo salió bien y se creó exactamente un recurso nuevo".

Los clientes automatizados (otras APIs, aplicaciones frontend, scripts) toman decisiones basadas en el código de estado. Un frontend que espera `201` para mostrar un mensaje de "recurso creado" no funcionará correctamente si recibe `200`.

> **La regla práctica:**
> - Operación que solo consulta → `200 OK`
> - Operación que crea un recurso nuevo → `201 Created`
> - Operación que actualiza un recurso existente → `200 OK`
> - Operación que elimina → `204 No Content`

---

## Decisión 2: el servidor asigna el ID, no el cliente

En el endpoint `POST /tickets`, el cliente manda esto:

```json
{
  "title": "Bug en login",
  "description": "...",
  "status": "NEW"
}
```

Y el servidor responde con:

```json
{
  "id": 3,
  "title": "Bug en login",
  "description": "...",
  "status": "NEW"
}
```

El cliente no mandó el `id`. El servidor lo asignó. Esto no es accidental: es una decisión de diseño deliberada.

**¿Por qué no dejar que el cliente elija su propio ID?**

Imagina que dos clientes (dos usuarios distintos usando la aplicación al mismo tiempo) envían simultáneamente un ticket con `"id": 5`. ¿Cuál de los dos tiene razón? ¿Quién gana? El sistema quedaría en un estado inconsistente.

El servidor tiene una visión centralizada del estado: sabe qué IDs ya existen. Por eso la autoridad para generar IDs siempre recae en el servidor, nunca en el cliente.

**¿Qué pasa si el cliente manda un `id` en el JSON de todas formas?**

Jackson lo leerá y lo asignará al campo `id` del objeto `Ticket`. Pero inmediatamente después, el `Repository` lo sobreescribirá con `ticket.setId(currentId++)`. El valor que mandó el cliente se descarta. El servidor siempre tiene la última palabra sobre el ID.

---

## Decisión 3: ID incremental manual en lugar de UUID

El raw material de esta lección menciona que no usamos UUID aún. Aquí está el razonamiento completo.

Un UUID se ve así: `550e8400-e29b-41d4-a716-446655440000`. Es un identificador globalmente único, generado de forma aleatoria, que prácticamente nunca colisiona con otro UUID aunque lo generes en otra máquina.

¿Por qué no usarlo desde el principio?

| Criterio | ID incremental (`Long`) | UUID (`String`) |
|---|---|---|
| Legibilidad en pruebas | Fácil: `1`, `2`, `3` | Difícil: `550e8400-...` |
| Complejidad de implementación | Mínima | Requiere `UUID.randomUUID()` y tipo `String` |
| URLs amigables | `GET /tickets/3` | `GET /tickets/550e8400-...` |
| Valor pedagógico en esta etapa | Alto: foco en el flujo | Bajo: distrae con detalles |
| Cuándo tiene sentido | APIs internas con BD relacional | APIs públicas, microservicios distribuidos |

La regla es sencilla: no agregues complejidad antes de necesitarla. En esta etapa, el contador incremental es la herramienta correcta. Cuando conectemos una base de datos real, JPA o PostgreSQL manejarán la generación de IDs automáticamente, y el contador manual desaparecerá.

---

## Decisión 4: `@NoArgsConstructor` en el modelo

Antes de esta lección, `Ticket` solo tenía `@AllArgsConstructor`. Ahora agregamos `@NoArgsConstructor`. ¿Por qué?

Cuando Spring recibe una petición con `@RequestBody`, le pide a Jackson que convierta el JSON en un objeto Java. Jackson hace esto en dos pasos:

1. Crea una instancia vacía del objeto: `new Ticket()` → necesita constructor sin argumentos
2. Llama a cada setter para asignar los valores del JSON: `ticket.setTitle("Bug en login")` → necesita setters (`@Setter`)

Sin `@NoArgsConstructor`, el paso 1 falla y Spring devuelve un error `400 Bad Request` con un mensaje confuso sobre deserialización. El error real no es que el JSON sea inválido: es que Jackson no puede construir el objeto.

> **¿No rompe esto algo?**
> No. `@NoArgsConstructor` y `@AllArgsConstructor` pueden coexistir sin problema. Java permite múltiples constructores con diferentes firmas. El código existente (los tickets semilla con `new Ticket(1L, "Ticket 1", ...)`) sigue usando `@AllArgsConstructor`. El nuevo flujo de `@RequestBody` usa `@NoArgsConstructor`.

---

## Decisión 5: `ResponseEntity` como estándar en el Controller

En la lección anterior, el `TicketController` devolvía `List<Ticket>` directamente:

```java
@GetMapping
public List<Ticket> getAllTickets() {
    return this.service.getTickets();
}
```

En esta lección, el nuevo endpoint devuelve `ResponseEntity<Ticket>`:

```java
@PostMapping
public ResponseEntity<Ticket> create(@RequestBody Ticket ticket) {
    Ticket saved = service.create(ticket);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

¿Por qué el cambio? Porque `ResponseEntity` nos da control completo sobre tres aspectos de la respuesta HTTP:

1. **El código de estado**: `200`, `201`, `404`, `400`, etc.
2. **Los headers**: `Content-Type`, `Location`, cabeceras personalizadas
3. **El body**: el objeto serializado como JSON

Devolver el objeto directamente le delega ese control a Spring, que simplemente asume `200 OK` siempre que no haya excepción. Eso es conveniente, pero nos quita expresividad.

A partir de esta lección, todos los endpoints nuevos usarán `ResponseEntity`. El `GET /tickets` existente se migra en la próxima iteración.

> **Criterio de calidad que te acompaña en el curso:**
> Poco alcance, buena forma. Una API con dos endpoints perfectamente estructurados es mejor que cinco endpoints que no comunican correctamente su estado HTTP.

---

## Decisión 6: el body de la respuesta incluye el objeto creado completo

Cuando el `POST` es exitoso, la respuesta incluye el ticket tal como quedó guardado:

```json
{
  "id": 3,
  "title": "Bug en login",
  "description": "...",
  "status": "NEW"
}
```

Hay APIs que responden al `POST` con el body vacío y solo el código `201`. Técnicamente es válido. Pero incluir el objeto creado en la respuesta tiene una ventaja concreta para el cliente: **no necesita hacer un GET adicional** para obtener el ID que le asignó el servidor.

Si el cliente necesita saber el ID del ticket que acaba de crear (por ejemplo, para redirigir al usuario a la pantalla de detalle), la respuesta ya lo tiene. Sin una segunda petición. Sin estado compartido. Sin condiciones de carrera.

Esta es la práctica recomendada en APIs REST modernas y es la que usaremos a lo largo del curso.






<!-- START OF FILE: docs_lessons_05-post_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 05-post 04 checklist rubrica minima
---
# Lección 05 - Lista de verificación: ¿llegué al mínimo requerido?

Usa esta lista para revisar tu propio trabajo antes de presentarlo. Cada ítem tiene una breve explicación de qué significa y cómo verificarlo.

---

## ¿Qué es un indicador de evaluación (IE)?

Los indicadores de evaluación son los criterios concretos con los que se mide tu aprendizaje. Esta lección construye directamente sobre la anterior: los mismos indicadores de la lección 04 siguen vigentes, y ahora se agrega uno nuevo relacionado con la creación de recursos.

---

## IE 1.2.3 - Creación de recursos con POST

Este indicador mide si eres capaz de extender una API existente para que pueda recibir datos del cliente y persistirlos correctamente.

Checklist:

- [ ] El endpoint `POST /tickets` existe en `TicketController`
- [ ] El método del controlador usa `@PostMapping` sin argumentos adicionales
- [ ] El parámetro recibe el body con `@RequestBody Ticket ticket`
- [ ] El método retorna `ResponseEntity<Ticket>`, no `Ticket` directamente
- [ ] El código de respuesta es `201 Created`, no `200 OK`
- [ ] El body de la respuesta incluye el ticket con el `id` asignado por el servidor
- [ ] El `id` es asignado en el `Repository`, no en el `Controller` ni en el `Service`

**Cómo verificarlo:** haz una petición `POST http://localhost:8080/tickets` en Postman con un body JSON. Observa el código de estado en la esquina superior derecha: debe decir `201 Created`. El body de la respuesta debe incluir el objeto con un campo `id` con valor `3` (o el siguiente en la secuencia).

**Flujo correcto:**

```
POST /tickets (body JSON) → TicketController.create(@RequestBody)
                          → TicketService.create(ticket)
                          → TicketRepository.save(ticket)
                          → ticket.setId(currentId++)
                          → ResponseEntity 201 Created + body
```

---

## IE 1.1.3 - Respuestas REST y códigos HTTP

Este indicador ahora incluye el manejo explícito de `ResponseEntity`, que en la lección anterior estaba marcado como pendiente.

Checklist:

- [ ] `POST /tickets` responde con `201 Created`
- [ ] `GET /tickets` responde con `200 OK`
- [ ] El body de la respuesta en ambos casos es JSON válido
- [ ] `ResponseEntity` se usa en el método `create()` del controlador
- [ ] El método `getAllTickets()` existente no fue modificado ni roto por los cambios

**Cómo verificarlo:**
- Postman `POST /tickets` → `201 Created` con body
- Postman `GET /tickets` → `200 OK` con arreglo de tickets (incluyendo el recién creado)

> **¿Por qué el GET sigue sin `ResponseEntity`?**
> Porque aún no lo hemos migrado. Está planificado para la próxima iteración. Lo importante es que el nuevo endpoint `POST` ya lo usa correctamente desde el inicio.

---

## IE 1.2.1 - Estructura CSR preservada

Este indicador viene de la lección anterior y sigue vigente. Agregar un nuevo endpoint no debería romper la separación de responsabilidades que ya tenías.

Checklist:

- [ ] `TicketController` no accede directamente a la lista de tickets
- [ ] `TicketController` no tiene `setId()` ni lógica de generación de IDs
- [ ] `TicketService` tiene el método `create()` que llama a `repository.save()`
- [ ] `TicketRepository` tiene el método `save()` con el contador `currentId`
- [ ] El contador `currentId` arranca en `3L` para no colisionar con los tickets semilla

**Cómo verificarlo:** abre cada clase y pregúntate si tiene código que no le corresponde. El `Controller` solo debería tener anotaciones HTTP y llamadas al `Service`. El `Service` solo debería contener lógica de negocio. El `Repository` solo debería manejar la lista.

---

## IE 1.2.2 - Modelo actualizado correctamente

El modelo `Ticket` necesita una modificación específica para que `@RequestBody` funcione.

Checklist:

- [ ] La clase `Ticket` tiene `@NoArgsConstructor`
- [ ] La clase `Ticket` conserva `@AllArgsConstructor` (los tickets semilla lo necesitan)
- [ ] La clase `Ticket` tiene `@Getter` y `@Setter` (Jackson los necesita para la deserialización)
- [ ] Los tickets semilla en el constructor de `TicketRepository` siguen funcionando

**Cómo verificarlo:** si el JSON llega correctamente al servidor (el ticket se crea con los datos que mandaste), significa que `@NoArgsConstructor` está en su lugar. Si recibes un error `400 Bad Request` con mención a "deserialization" o "no suitable constructor", falta `@NoArgsConstructor`.

---

## IE 1.1.2 - Diseño de endpoints REST

Este indicador también viene de la lección anterior. El nuevo endpoint debe seguir las mismas convenciones.

Checklist:

- [ ] El recurso sigue en plural: `/tickets`
- [ ] El método HTTP es el correcto para crear: `POST`
- [ ] La URL no contiene verbos: no hay `/createTicket` ni `/nuevo`
- [ ] Un solo `@RequestMapping("/tickets")` a nivel de clase cubre ambos métodos

**Cómo verificarlo:** el `@PostMapping` del método no necesita argumentos porque hereda el path `/tickets` de `@RequestMapping`. Si ves `@PostMapping("/tickets")` en el método, hay duplicación innecesaria.

---

## Indicadores que se trabajan en lecciones siguientes

| Indicador | Qué cubre |
|---|---|
| IE 1.3.1 | Validaciones de entrada: `@Valid`, `@NotNull`, `@NotBlank` para evitar nombres vacíos en `POST` |
| IE 1.3.2 | Manejo global de excepciones con `@ControllerAdvice` |
| IE 1.3.3 | Pruebas automáticas de los endpoints REST |
| IE 1.2.3 (extensión) | `PUT` para actualizar y `DELETE` para eliminar: CRUD completo |

---

## ¿Completé el mínimo de esta lección?

Completaste el mínimo si:

- ✅ `POST http://localhost:8080/tickets` con un body JSON devuelve `201 Created` con el ticket creado (incluyendo un `id` asignado por el servidor)
- ✅ `GET http://localhost:8080/tickets` después del POST incluye el ticket recién creado en la lista
- ✅ El `id` del nuevo ticket es `3` (o mayor), nunca `null` ni el valor que el cliente intentara mandar
- ✅ Puedes explicar en tus propias palabras por qué `201` y no `200`, y por qué el servidor asigna el ID






<!-- START OF FILE: docs_lessons_05-post_05_actividad_individual_categories.md -->
# Documento: docs lessons 05-post 05 actividad individual categories
---
# Lección 05 - Actividad individual: recurso `categories`

Ahora es tu turno. Esta actividad replica lo que hiciste con `Ticket` en clase, pero esta vez para un recurso `Category` que crearás desde cero. El objetivo es que apliques el patrón CSR con `POST` de forma autónoma, tomando las mismas decisiones de diseño que aprendiste.

> Si no estuviste en clase, lee primero el tutorial paso a paso (`02_guion_paso_a_paso.md`) y el documento de decisiones de diseño (`03_decisiones_post_y_http.md`) antes de comenzar esta actividad.

---

## ¿Qué vas a construir?

Un recurso `Category` completamente nuevo dentro del mismo proyecto `Tickets`, con la arquitectura por capas que ya conoces. El entregable incluye dos endpoints:

```
GET  /api/categories       → devuelve la lista de categorías (con datos semilla)
POST /api/categories       → recibe una categoría nueva y la guarda
```

Nota el prefijo `/api` en la ruta. A partir de esta actividad empezamos a incorporarlo como práctica profesional para separar semánticamente la API del resto del servidor.

---

## Restricciones de la actividad

| Restricción | Por qué |
|---|---|
| Usar el patrón CSR con paquetes separados | Es el núcleo de la arquitectura que se evalúa |
| Usar `List` para persistencia temporal | No usamos BD todavía |
| El servidor asigna el ID, no el cliente | Regla de diseño REST explicada en clase |
| `POST` debe responder `201 Created` | Semántica correcta de HTTP |
| `GET` debe responder `200 OK` | Semántica correcta de HTTP |
| Usar `ResponseEntity` en ambos endpoints | Estándar que adoptamos a partir de esta lección |
| La URL debe usar el prefijo `/api` | Práctica profesional para identificar la API |

---

## Modelo sugerido

Crea la clase `Category` en el paquete `model`. Una categoría de ticket tiene un identificador, un nombre y una descripción:

```java
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private Long id;
    private String name;
    private String description;
}
```

> **¿Por qué necesitas `@NoArgsConstructor` desde el inicio?**
> Porque este recurso tendrá un endpoint `POST` con `@RequestBody`. Jackson necesita el constructor vacío para deserializar el JSON entrante. Si no lo pones desde el principio, tendrás un `400 Bad Request` confuso cuando pruebes el endpoint.

> **¿Qué significa cada campo?**
> - `id`: identificador único asignado por el servidor
> - `name`: nombre corto de la categoría (por ejemplo, `"Bug"`, `"Feature"`, `"Mejora"`)
> - `description`: explicación más detallada de qué tickets entran en esta categoría

---

## Guía de implementación

Sigue este orden. Cada paso construye sobre el anterior.

### 1. Crea el paquete y la clase `Category`

La clase va en el paquete `model`, junto a `Ticket.java`. Usa las cuatro anotaciones Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`.

### 2. Crea `CategoryRepository`

- Anótala con `@Repository`
- Declara `private List<Category> categories` y `private Long currentId = 3L`
- En el constructor, inicializa la lista con al menos 2 categorías de prueba:
  - `Bug` / `"Problema o error que afecta el funcionamiento esperado"`
  - `Feature` / `"Nueva funcionalidad solicitada por el usuario"`
- Crea el método `getAll()` que retorne la lista completa
- Crea el método `save(Category category)` que asigne el ID, agregue a la lista y retorne la categoría

### 3. Crea `CategoryService`

- Anótala con `@Service`
- Recibe `CategoryRepository` por constructor (inyección de dependencias)
- Crea el método `getCategories()` que llame a `repository.getAll()`
- Crea el método `create(Category category)` que llame a `repository.save(category)`

### 4. Crea `CategoryController`

- Anótalo con `@RestController` y `@RequestMapping("/api/categories")`
- Recibe `CategoryService` por constructor
- Crea el método `getAllCategories()` con `@GetMapping` que retorne `ResponseEntity.ok(service.getCategories())`
- Crea el método `create()` con `@PostMapping` y `@RequestBody Category category` que retorne `ResponseEntity.status(HttpStatus.CREATED).body(service.create(category))`

### 5. Prueba ambos endpoints

**Prueba GET:**

```
GET http://localhost:8080/api/categories
```

Resultado esperado (`200 OK`):

```json
[
  { "id": 1, "name": "Bug", "description": "Problema o error que afecta el funcionamiento esperado" },
  { "id": 2, "name": "Feature", "description": "Nueva funcionalidad solicitada por el usuario" }
]
```

**Prueba POST:**

```
POST http://localhost:8080/api/categories
Content-Type: application/json

{
  "name": "Mejora",
  "description": "Cambio menor que optimiza una funcionalidad existente"
}
```

Resultado esperado (`201 Created`):

```json
{
  "id": 3,
  "name": "Mejora",
  "description": "Cambio menor que optimiza una funcionalidad existente"
}
```

**Prueba de integridad (GET después del POST):**

Después del POST, vuelve a hacer `GET /api/categories`. Deberías ver las 3 categorías: las 2 semilla más la que acabas de crear.

---

## ¿Cómo sé si lo hice bien?

### Logro alto

- Los cuatro paquetes existen con sus clases: `Category`, `CategoryRepository`, `CategoryService`, `CategoryController`
- `GET /api/categories` responde `200 OK` con un arreglo JSON de categorías
- `POST /api/categories` responde `201 Created` con la categoría creada (incluyendo `id`)
- El `id` es asignado por el servidor, nunca viene `null` en la respuesta
- `CategoryController` usa `ResponseEntity` en ambos métodos
- Puedes explicar en voz alta por qué cada clase está en su paquete y por qué `201` en el POST

### Logro medio

- La estructura CSR existe pero algún método está en la capa equivocada (por ejemplo, la asignación de ID en el `Service` o en el `Controller`)
- El POST funciona pero devuelve `200` en lugar de `201`
- El GET funciona pero no usa `ResponseEntity`
- El endpoint responde correctamente pero no puedes justificar las decisiones

### Logro inicial

- El endpoint funciona, pero todo está en el `Controller` sin separación de capas
- La URL contiene verbos (`/crearCategoria`, `/nuevaCategoria`)
- El campo `id` llega `null` en la respuesta (el servidor no lo está asignando)
- No hay datos semilla y el GET devuelve un arreglo vacío

---

## Extensión opcional: si terminas antes

### Opción A: validación manual de campo vacío

Antes de guardar la categoría, verifica que el campo `name` no sea `null` ni una cadena vacía. Si el nombre está vacío, devuelve `400 Bad Request` con un mensaje claro:

```json
{
  "error": "El nombre de la categoría no puede estar vacío"
}
```

Piensa en qué capa va esa validación. ¿En el `Controller`? ¿En el `Service`? ¿En el `Repository`? Justifica tu decisión antes de escribir el código.

### Opción B: buscar categoría por ID

Agrega el endpoint:

```
GET /api/categories/{id}
```

- Si la categoría existe: `200 OK` con el objeto
- Si no existe: `404 Not Found`

Usa `Optional<Category>` en el `Repository` para manejar el caso donde el ID no se encuentra.

### Opción C: prefijo `/api` en Tickets también

Ahora que `CategoryController` usa `/api/categories`, es consistente migrar `TicketController` para que también use `/api/tickets`. Hazlo y verifica que ambos endpoints siguen funcionando.

---

## Antes de entregar: pregúntate esto

1. Si alguien hace `POST /api/categories` con `{ "id": 99, "name": "Test" }`, ¿qué `id` aparece en la respuesta? ¿Por qué?
2. ¿Qué código de estado devuelve tu `POST` cuando todo sale bien? ¿Y tu `GET`?
3. Si mañana necesitas que las categorías se guarden en una base de datos, ¿qué archivo modificarías? ¿Qué archivos **no** necesitarías tocar?

Si las tres respuestas son claras para ti, completaste el objetivo de esta actividad.




