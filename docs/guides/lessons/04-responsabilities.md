<!-- START OF FILE: docs_lessons_04-responsabilities_01_objetivo_y_alcance.md -->
# Documento: docs lessons 04-responsabilities 01 objetivo y alcance
---
# Lección 04 - Separación de responsabilidades: ¿qué vas a aprender?

## ¿De dónde venimos?

En la lección anterior construiste tu primer endpoint con Spring Boot: algo como `GET /greetings` que devolvía un texto plano. Funcionaba, pero todo el código estaba en un solo lugar: el controlador hacía absolutamente todo.

Eso está bien para empezar, pero en la práctica real ese enfoque genera problemas muy rápido. Imagina que mañana tu jefe te pide cambiar cómo se buscan los datos, o agregar una regla de negocio, o hacer pruebas automáticas. Con todo en un solo archivo, cualquier cambio pequeño puede romper todo lo demás.

Esta lección existe para resolver ese problema desde el principio.

---

## ¿Qué vas a construir?

Al terminar esta lección tendrás un microservicio real y ejecutable que:

- Expone el endpoint `GET /tickets` y devuelve una lista de tickets en formato JSON
- Está organizado en **cuatro capas separadas**, cada una con una responsabilidad clara
- Usa datos en memoria (sin base de datos aún) para que podamos concentrarnos en la arquitectura

### Lo que vas a ser capaz de explicar

Más importante que el código es que entiendas el **por qué** detrás de cada decisión. Al terminar deberías poder responder:

- ¿Qué hace el `Controller` y qué NO debería hacer?
- ¿Por qué existe el `Service` como capa separada?
- ¿Por qué el `Repository` no debería tener lógica de negocio?
- ¿Qué ventaja tiene retornar `ResponseEntity` en lugar de un objeto directo?
- ¿Por qué las URLs REST usan sustantivos en plural en lugar de verbos?

---

## ¿Qué requerimientos implementamos en esta lección?

> El proyecto completo está descrito en [`00_enunciado_proyecto.md`](../00_enunciado_proyecto.md).
> Ahí encontrarás el escenario, los actores y la lista completa de requerimientos numerados.

De esa lista, esta lección implementa **uno**:

| Requerimiento | Lo que construimos |
|---------------|--------------------|
| **REQ-01** — Consultar todos los tickets | El endpoint `GET /tickets` que devuelve la lista completa en JSON |

Solo uno. Pero ese uno lo construimos con una arquitectura que soportará todo lo demás sin necesidad de reescribir nada.

Los REQ-02 al REQ-10 (crear, actualizar, eliminar, validar) **necesitan** esta base bien puesta para poder agregarse lección a lección sin romper lo que ya existe. Si construyes el `GET` de cualquier forma, el costo lo pagas después. Si lo construyes con capas, los siguientes pasos son naturales.

---

## ¿Qué NO cubre esta lección? (y por qué)

Es importante que sepas lo que intencionalmente dejamos para más adelante, para que no te preocupes si no lo ves aquí:

| Tema | ¿Por qué lo dejamos después? |
|---|---|
| CRUD completo (crear, editar, eliminar) | Primero necesitas dominar la estructura antes de multiplicar endpoints |
| Validaciones (`@Valid`, `@NotNull`) | Agregamos complejidad solo cuando la base esté sólida |
| Manejo de errores global (`@ControllerAdvice`) | Es el paso siguiente natural después de tener un endpoint funcionando |
| Base de datos real (JPA, PostgreSQL) | Usamos memoria para que el foco sea la arquitectura, no la infraestructura |

El objetivo de esta lección no es hacer mucho: es hacer **una cosa bien hecha y con forma profesional**.

---

## Configuración del proyecto

El proyecto ya tiene una configuración mínima en `src/main/resources/application.properties`:

```properties
spring.application.name=Tickets
```

Esto le dice a Spring Boot cómo se llama tu aplicación. Es el único parámetro configurado hasta ahora.

En el futuro vas a aprender a personalizar más cosas desde ahí, como:

- **Cambiar el puerto** donde corre la aplicación (`server.port`)
- **Agregar un prefijo global** a todas tus rutas (`server.servlet.context-path`)
- **Personalizar el banner** que aparece en consola al iniciar (`banner.txt`)

Por ahora esos temas están pendientes. Lo importante es que entiendas que esa personalización **vive en el archivo de configuración**, no dentro del código Java de tus capas.

---

## La idea central de esta lección

> "No buscamos cantidad de endpoints. Buscamos escribir un endpoint pequeño, pero con forma profesional desde el inicio."

Un código bien estructurado hoy te ahorra horas de depuración mañana. La separación de responsabilidades no es un trámite burocrático: es la diferencia entre un proyecto que escala y uno que se convierte en un problema.





<!-- START OF FILE: docs_lessons_04-responsabilities_02_guion_paso_a_paso.md -->
# Documento: docs lessons 04-responsabilities 02 guion paso a paso
---
# Lección 04 - Tutorial paso a paso: construyendo tu primera API con capas

Sigue esta guía en orden. Cada sección te explica qué vas a hacer y **por qué lo hacemos así**, para que no solo copies código sino que entiendas la lógica detrás.

---

## Paso 1: el problema del "controlador que hace todo"

Antes de escribir código, piensa en esto:

Imagina que tienes un restaurante donde el mismo mesero toma el pedido, lo cocina, lo sirve y también lleva la contabilidad. Cuando el restaurante es pequeño quizás funciona, pero cuando creces ese modelo colapsa: si el mesero se enferma, todo falla; si quieres cambiar el menú, tienes que reentrenar a toda la persona.

En programación ocurre exactamente lo mismo. Si tu `Controller` valida datos, contiene la lógica de negocio, accede directamente a la colección de datos y formatea la respuesta, estás ante el mismo problema:

- Es difícil de probar: no puedes testear la lógica sin levantar el servidor HTTP completo
- Es frágil: cambiar una regla de negocio puede romper el manejo HTTP
- No escala: cuando el proyecto crece, ese archivo se vuelve imposible de mantener

La solución es **separar responsabilidades**: cada parte del sistema hace una sola cosa y la hace bien. Eso es exactamente lo que vas a construir hoy.

---

## Paso 2: crear la estructura de paquetes (CSR)

El patrón que vamos a usar se llama **CSR** (Controller - Service - Repository). Antes de escribir ninguna clase, crea los siguientes paquetes dentro de `cl.duoc.fullstack.tickets`:

```
controller/    → recibe y responde peticiones HTTP
service/       → contiene la lógica de negocio
respository/   → accede y almacena los datos (nota: el nombre conserva la errata intencionalmente)
model/         → define la forma de los datos (las "entidades")
```

> **¿Por qué paquetes separados?** En Java, los paquetes son más que carpetas: comunican intención. Cuando alguien abre tu proyecto y ve estos cuatro paquetes, inmediatamente sabe que sigues una arquitectura por capas. Es un lenguaje común entre desarrolladores.

El flujo de una petición siempre sigue este camino:

```
HTTP → Controller → Service → Repository → Service → Controller → HTTP
```

Nunca al revés, nunca saltando capas. El `Controller` nunca llama directamente al `Repository`. Si algún día ves eso en tu código, es una señal de que algo está mal ubicado.

---

## Paso 3: el Modelo (`Ticket.java`)

El modelo es la clase que representa los datos de tu dominio. En este caso, un `Ticket` tiene cuatro atributos: un identificador, un título, una descripción y un estado.

Crea la clase `Ticket` en el paquete `model`:

```java
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Ticket {
    private Long id;
    private String title;
    private String description;
    private String status;
}
```

> **¿Qué es Lombok y por qué lo usamos?**
> Lombok es una librería que genera código repetitivo automáticamente durante la compilación. Las tres anotaciones hacen lo siguiente:
> - `@Getter`: genera un método `getId()`, `getTitle()`, etc. por cada campo
> - `@Setter`: genera un método `setId()`, `setTitle()`, etc. por cada campo
> - `@AllArgsConstructor`: genera un constructor con todos los campos como parámetros
>
> Sin Lombok tendrías que escribir todo eso a mano. Con Lombok, tu clase queda limpia y legible.

> **¿Por qué los campos están en inglés?**
> Es una convención del sector. Los identificadores de código (clases, métodos, variables) se escriben en inglés para que el proyecto sea entendible por cualquier desarrollador del mundo, independientemente de su idioma. Los textos que el usuario ve sí pueden estar en el idioma local.

---

## Paso 4: el Repository (`TicketRepository.java`)

El `Repository` es la capa que se encarga de **almacenar y recuperar datos**. Hoy usamos una `List` en memoria para simular una base de datos. Cuando en lecciones futuras conectemos una base de datos real, solo tendrás que cambiar esta capa: el resto del código no sabrá la diferencia.

Crea la clase `TicketRepository` en el paquete `respository`:

```java
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TicketRepository {

    List<Ticket> tickets;

    public TicketRepository() {
        tickets = new ArrayList<>();
        tickets.add(new Ticket(1L, "Ticket 1", "Ticket 1", "NEW"));
        tickets.add(new Ticket(2L, "Ticket 2", "Ticket 2", "NEW"));
    }

    public List<Ticket> getAll() {
        return tickets;
    }
}
```

> **¿Qué hace `@Repository`?**
> Le dice a Spring que esta clase es un componente de acceso a datos. Spring la registra automáticamente en su contenedor y la tiene disponible para inyectarla donde sea necesaria. Sin esta anotación, Spring no sabría que esta clase existe.

> **¿Por qué los datos iniciales se cargan en el constructor?**
> El constructor se ejecuta una sola vez cuando Spring crea el objeto. Esos datos iniciales (llamados "seed data" o "datos semilla") nos permiten probar el endpoint de inmediato sin tener que crear datos manualmente. Son datos de prueba, no datos reales.

> **Importante:** como los datos viven en memoria, si reinicias la aplicación vuelven al estado inicial. Eso está bien por ahora: todavía no necesitamos persistencia real.

---

## Paso 5: el Service (`TicketService.java`)

El `Service` es la capa que contiene la **lógica de negocio**. Hoy nuestra lógica es simple (solo devolver la lista), pero esta capa existe porque en el mundo real aquí es donde irían las reglas: filtrar tickets por estado, calcular prioridades, validar que el usuario tenga permisos, etc.

Crea la clase `TicketService` en el paquete `service`:

```java
import org.springframework.stereotype.Service;
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
}
```

> **¿Qué hace `@Service`?**
> Similar a `@Repository`, le dice a Spring que este es un componente de lógica de negocio. Spring lo registra y lo tiene disponible para inyectarlo.

> **¿Por qué el `Service` recibe el `Repository` por constructor?**
> Esto se llama **inyección de dependencias por constructor**. En lugar de que `TicketService` cree él mismo su `TicketRepository` (con `new`), se lo pedimos a Spring a través del constructor. Las ventajas son claras:
> - Spring gestiona el ciclo de vida de los objetos, no tú
> - En pruebas unitarias puedes pasar un `Repository` falso (mock) sin levantar todo el sistema
> - Las dependencias son explícitas y visibles: cualquiera que lea el constructor sabe exactamente qué necesita esta clase para funcionar

---

## Paso 6: el Controller (`TicketController.java`)

El `Controller` es la única capa que "habla HTTP". Su único trabajo es:
1. Recibir la petición HTTP
2. Llamar al `Service` para que haga el trabajo
3. Devolver la respuesta HTTP

Nada más. Si ves lógica de negocio en el `Controller`, es una señal de que algo está en el lugar equivocado.

Crea la clase `TicketController` en el paquete `controller`:

```java
import org.springframework.web.bind.annotation.GetMapping;
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
}
```

> **¿Qué hace cada anotación?**
> - `@RestController`: combina `@Controller` y `@ResponseBody`. Le dice a Spring que esta clase maneja peticiones HTTP y que los objetos que retorne deben convertirse automáticamente a JSON.
> - `@RequestMapping("/tickets")`: define la URL base para todos los endpoints de este controlador. Todos los métodos de esta clase responderán bajo `/tickets`.
> - `@GetMapping`: mapea el método `getAllTickets()` a las peticiones `GET /tickets`. Si el cliente hace `GET /tickets`, Spring ejecuta este método.

> **¿Por qué el método se llama `getAllTickets()` y no solo `get()`?**
> Los nombres de los métodos en el `Controller` deben ser descriptivos. `getAllTickets` deja claro a cualquier desarrollador que ese método obtiene todos los tickets. Recuerda: el nombre del método no aparece en la URL; la URL la define `@GetMapping`.

> **Mejora pendiente:** actualmente el método devuelve `List<Ticket>` directamente. El siguiente paso (próximas lecciones) es envolverlo en `ResponseEntity<List<Ticket>>`, lo que nos dará control explícito sobre el código HTTP de la respuesta. Por ahora, Spring retorna automáticamente `200 OK` cuando el método termina sin errores.

---

## Paso 7: verificar que todo funciona

Levanta la aplicación con el botón de play en IntelliJ (o con `./mvnw spring-boot:run` en la terminal) y abre Postman o Insomnia.

Haz una petición `GET` a:

```
http://localhost:8080/tickets
```

Deberías recibir una respuesta `200 OK` con este cuerpo JSON:

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

Si ves eso, ¡felicitaciones! Acabas de construir tu primera API con arquitectura por capas.

---

## Paso 8: configuración del proyecto (pendiente)

Por ahora la aplicación corre con la configuración predeterminada de Spring Boot (puerto `8080`, sin prefijo de ruta). El único parámetro configurado explícitamente es el nombre de la aplicación, en `src/main/resources/application.properties`:

```properties
spring.application.name=Tickets
```

En una próxima iteración aprenderás a personalizar la aplicación migrando a `application.yaml` y agregando:

### Cambiar el puerto

Por defecto Spring Boot usa el puerto `8080`. Si necesitas cambiar eso (por ejemplo, porque tienes otro servicio corriendo en ese puerto):

```yaml
server:
  port: 8081
```

Con esto, tu endpoint quedaría en `http://localhost:8081/tickets`.

### Agregar un prefijo global (context path)

En un entorno real, es común que todas las rutas de tu API tengan un prefijo que la identifique. Por ejemplo, si tu API se llama "tickets-app":

```yaml
server:
  servlet:
    context-path: /tickets-app
```

Con esto, el endpoint quedaría en `http://localhost:8080/tickets-app/tickets`.

> **¿Por qué esto se configura en el archivo YAML y no en el código Java?**
> Porque el puerto y el prefijo son **configuración de entorno**, no lógica de negocio. En desarrollo puedes usar el puerto 8080; en producción, el 443. Si eso estuviera hardcodeado en el `Controller`, tendrías que cambiar el código fuente cada vez que cambia el entorno. El archivo de configuración separa esas decisiones del código.

### Personalizar el banner de inicio

Cuando Spring Boot arranca, muestra un banner en la consola. Puedes personalizarlo creando el archivo `src/main/resources/banner.txt`:

```text
=== TICKETS API - CSR CLASS ===
```

Es un detalle pequeño, pero útil para identificar rápidamente qué aplicación está corriendo cuando tienes varias en tu máquina.

---

## Paso 9: reflexiona antes de cerrar

Antes de pasar a la actividad, respóndete estas preguntas mentalmente (o en voz alta):

1. Si mañana necesitas conectar una base de datos real en lugar de la `List`, ¿qué archivo modificarías? ¿Por qué solo ese?
2. Si el cliente pide que un ticket solo sea visible si está en estado `"NEW"`, ¿en qué capa agregarías ese filtro? ¿Por qué no en el `Controller`?
3. Si otro equipo quiere consumir los mismos datos de tickets pero desde una interfaz gráfica diferente, ¿tendrías que cambiar algo del `Service` o del `Repository`? ¿Por qué?

Si puedes responder estas tres preguntas con seguridad, entendiste el objetivo de esta lección.

---

## Extensión opcional

Si terminaste todo lo anterior y quieres ir un paso más, agrega un segundo endpoint que filtre por estado:

```
GET /tickets/status/{status}
```

Por ejemplo, `GET /tickets/status/NEW` debería devolver solo los tickets con `status = "NEW"`. Piensa en qué capa va la lógica de filtrado antes de escribir el código.





<!-- START OF FILE: docs_lessons_04-responsabilities_03_decisiones_rest_y_csr.md -->
# Documento: docs lessons 04-responsabilities 03 decisiones rest y csr
---
# Lección 04 - Por qué hacemos las cosas así: decisiones de diseño explicadas

Esta sección no es un listado de reglas para memorizar. Es una explicación de las decisiones que tomamos al construir la API, para que entiendas el razonamiento detrás de cada una. En el mundo real, un buen desarrollador no solo sabe *qué* hacer, sino *por qué* lo hace.

---

## Decisión 1: la URL es un sustantivo, no un verbo

Cuando defines una URL en una API REST, la URL debe representar un **recurso** (una "cosa"), no una acción. Por eso usamos:

```
GET /tickets
```

Y no:

```
GET /getTickets        ← MAL: el verbo ya está en el método HTTP (GET)
GET /getAllTickets      ← MAL: la URL no es un nombre de función Java
GET /ticket-list       ← MAL: no describe un recurso, describe una estructura
```

El método HTTP (`GET`, `POST`, `PUT`, `DELETE`) es quien expresa la acción. La URL expresa *sobre qué recurso* se realiza esa acción. Separar ambas responsabilidades hace que tu API sea predecible: cualquier desarrollador que la consuma puede intuir qué hace cada endpoint sin leer documentación.

> **¿Por qué el recurso va en plural (`/tickets` y no `/ticket`)?**
> Porque el endpoint devuelve una colección. Cuando dices `/tickets`, estás describiendo "el conjunto de tickets del sistema". Es una convención ampliamente adoptada en APIs REST del mundo real.

> **Diseño objetivo (pendiente):** en una API de producción, además agregaríamos un prefijo `/api` para separar la API del resto de rutas, y `/v1` para indicar la versión, quedando `GET /api/v1/tickets`. Eso permite que en el futuro exista una `v2` sin romper a los clientes que ya consumen la `v1`. Lo incorporaremos en lecciones futuras.

---

## Decisión 2: devolver `ResponseEntity` en lugar del objeto directo

Actualmente el controlador devuelve `List<Ticket>` directamente:

```java
public List<Ticket> getAllTickets() {
    return this.service.getTickets();
}
```

Spring Boot detecta que el método retornó sin error y envía automáticamente un `200 OK`. Eso funciona, pero oculta algo importante: **el código de estado HTTP es parte de la respuesta** y debería ser explícito en tu código.

La forma profesional es usar `ResponseEntity`:

```java
public ResponseEntity<List<Ticket>> getAllTickets() {
    return ResponseEntity.ok(this.service.getTickets());
}
```

¿Por qué es mejor? Porque cuando más adelante necesites devolver un `404 Not Found` (ticket no existe) o un `201 Created` (ticket creado exitosamente), ya tendrás la estructura lista. Si empiezas devolviendo el objeto directo, después tendrás que refactorizar todos tus endpoints.

> **Estado actual:** aún retornamos `List<Ticket>` directamente. Incorporar `ResponseEntity` es el siguiente paso planificado.

---

## Decisión 3: inyección de dependencias por constructor

En el proyecto usamos este patrón en todas las capas:

```java
public class TicketController {

    private TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }
}
```

La pregunta natural es: ¿por qué no hacemos simplemente `new TicketService()` dentro del constructor? La respuesta es que estaríamos violando un principio clave: **la clase que necesita una dependencia no debería ser responsable de crearla**.

Cuando inyectas por constructor:

- **Spring gestiona los objetos por ti.** No tienes que preocuparte de cuándo crear o destruir instancias.
- **Las dependencias son visibles.** Cualquiera que lea el constructor sabe exactamente qué necesita esa clase para funcionar. No hay dependencias ocultas.
- **Las pruebas unitarias se simplifican.** Puedes pasar un objeto falso (`mock`) en lugar del real sin modificar el código de producción.

Esto contrasta con la inyección por campo (usando `@Autowired` directamente sobre el atributo), que aunque es más corta, esconde las dependencias y hace las pruebas más difíciles.

---

## Decisión 4: cada capa tiene una sola pregunta que responder

Una forma práctica de recordar para qué sirve cada capa es asociarla con una pregunta:

| Capa | Su única pregunta |
|---|---|
| `Controller` | ¿Cómo entra y sale la petición HTTP? |
| `Service` | ¿Qué regla de negocio aplica aquí? |
| `Repository` | ¿Dónde y cómo se almacenan o recuperan los datos? |
| `Model` | ¿Cómo se ve la estructura del dato? |

Cuando revisas tu código, puedes hacer la siguiente prueba de cordura:

- ¿Hay `ResponseEntity` fuera del `Controller`? → Probablemente algo del HTTP se está filtrando hacia capas que no deberían saber de HTTP.
- ¿Hay lógica de negocio (`if`, cálculos, reglas) en el `Controller`? → Moverla al `Service`.
- ¿Hay reglas de negocio en el `Repository`? → Moverlas al `Service`. El `Repository` solo debe saber cómo guardar y recuperar datos, no qué hacer con ellos.

Seguir estas reglas hace que tu código sea predecible: siempre sabes dónde buscar cuando algo falla.

---

## Decisión 5: datos en memoria en lugar de base de datos real

Para esta lección usamos una `List<Ticket>` en el `TicketRepository` como almacenamiento:

```java
List<Ticket> tickets = new ArrayList<>();
tickets.add(new Ticket(1L, "Ticket 1", "Ticket 1", "NEW"));
```

Esto no es una limitación técnica: es una decisión pedagógica intencional.

Si conectáramos una base de datos desde el primer día, el 80% del tiempo lo pasaríamos configurando drivers, credenciales, esquemas y conexiones, en lugar de aprender la arquitectura en sí. Al usar memoria, el foco es completamente la separación de responsabilidades.

Además, la decisión tiene una ventaja arquitectónica: cuando más adelante conectes JPA y PostgreSQL, **solo modificarás el `TicketRepository`**. El `TicketService` y el `TicketController` no necesitarán cambiar, porque no saben (ni deben saber) dónde viven los datos.

> **Importante:** los datos en memoria se pierden al reiniciar la aplicación. Eso es esperado por ahora.

---

## Decisión 6: la configuración vive en archivos de configuración, no en el código

Actualmente el único parámetro configurado es:

```properties
spring.application.name=Tickets
```

La regla es simple: cualquier valor que pueda cambiar entre entornos (desarrollo, staging, producción) debe vivir en el archivo de configuración, **nunca hardcodeado en el código Java**.

Por ejemplo, el puerto de la aplicación puede ser diferente en cada ambiente. La forma correcta de cambiarlo es en `application.yaml`:

```yaml
server:
  port: 8081
  servlet:
    context-path: /tickets-app
```

Si ese valor estuviera escrito directamente en el `Controller`, tendrías que modificar y recompilar el código fuente cada vez que cambias de ambiente. Eso es un error grave en cualquier proyecto profesional.

> **Pendiente:** migrar a `application.yaml` y agregar configuraciones de puerto, context path y banner personalizado.

---

## Criterio de calidad que te acompaña en el curso

> "Poco alcance, buena forma."

En este curso preferimos que construyas un único endpoint perfectamente estructurado antes que cinco endpoints desorganizados. La forma profesional se aprende desde el primer día, no "cuando el proyecto crezca".





<!-- START OF FILE: docs_lessons_04-responsabilities_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 04-responsabilities 04 checklist rubrica minima
---
# Lección 04 - Lista de verificación: ¿llegué al mínimo requerido?

Usa esta lista para revisar tu propio trabajo antes de presentarlo. Cada ítem tiene una breve explicación de qué significa y cómo verificarlo, para que no sea solo un tick en una casilla.

---

## ¿Qué es un indicador de evaluación (IE)?

Los indicadores de evaluación son los criterios concretos con los que se mide tu aprendizaje. Esta lección no busca cubrir todos los indicadores de la unidad, sino construir una base sólida sobre la que se apoyarán las siguientes.

---

## IE 1.2.1 - Estructura CSR con separación real

Este indicador mide si tu código está organizado por responsabilidades o si todo está mezclado en un mismo lugar.

Checklist:

- [x] Existen los paquetes `controller`, `service`, `repository`, `model`
- [x] `TicketController` no accede directamente a la lista de tickets
- [x] `TicketService` llama a `TicketRepository.getAll()` a través de `getTickets()`
- [x] `TicketRepository` es el único lugar donde vive y se accede a la lista

**Cómo verificarlo:** abre cada clase y pregúntate "¿esta clase hace algo que no le corresponde?". Si `TicketController` tiene un `ArrayList` o un `new TicketRepository()`, algo está mal. El flujo correcto es:

```
GET /tickets → TicketController.getAllTickets()
             → TicketService.getTickets()
             → TicketRepository.getAll()
             → [ lista de tickets ]
```

---

## IE 1.1.2 - Diseño de endpoints REST

Este indicador mide si tus URLs siguen las convenciones de REST. Una API bien diseñada es predecible: alguien que nunca la vio puede intuir cómo usarla.

Checklist:

- [x] El recurso está en plural: `/tickets`
- [x] El método HTTP es el correcto para listar: `GET`
- [x] La URL no contiene verbos: no hay `/getTickets` ni `/listar`
- [ ] Base path versionado: `/api/v1` *(pendiente para próximas lecciones)*

**Cómo verificarlo:** haz la petición `GET http://localhost:8080/tickets` en Postman o Insomnia y confirma que recibes `200 OK` con un arreglo JSON.

---

## IE 1.1.3 - Respuestas REST y códigos HTTP

Este indicador mide si tu API comunica correctamente el resultado de cada operación a través de los códigos de estado HTTP. Los códigos no son un detalle menor: le dicen al cliente si su petición fue exitosa, si el recurso no existe, o si cometió un error.

Checklist:

- [x] La petición `GET /tickets` responde con `200 OK`
- [x] El cuerpo de la respuesta es JSON válido (arreglo de objetos `Ticket`)
- [ ] Uso de `ResponseEntity` para control explícito del código HTTP *(pendiente)*

> **¿Por qué está pendiente `ResponseEntity`?** Actualmente Spring retorna `200 OK` automáticamente porque el método no lanza ninguna excepción. Eso funciona para el caso feliz, pero no nos da control cuando las cosas salen mal. En las próximas lecciones usaremos `ResponseEntity` para poder retornar `404`, `400`, `201`, etc. según el caso.

**Cómo verificarlo:** en Postman, observa el código de estado en la esquina superior derecha de la respuesta. Debe decir `200 OK`.

---

## IE 1.2.2 - Modelo y persistencia en memoria

Este indicador mide si tu entidad de dominio está bien definida y si el mecanismo de almacenamiento temporal funciona correctamente.

Checklist:

- [x] La clase `Ticket` tiene campos coherentes: `id` (`Long`), `title`, `description`, `status`
- [x] `TicketRepository` usa `List<Ticket>` para almacenamiento temporal
- [x] El campo `id` está definido y se ve en la respuesta JSON

**Cómo verificarlo:** en la respuesta JSON del endpoint, cada objeto ticket debe tener los cuatro campos. Si falta alguno, revisa que Lombok esté instalado y que `@Getter` esté en la clase `Ticket`.

> **Pista sobre Lombok:** si los campos no aparecen en el JSON, probablemente Lombok no está generando los getters. Verifica que la dependencia esté en el `pom.xml` y que el plugin de Lombok esté habilitado en IntelliJ (Preferences → Plugins → Lombok).

---

## Configuración mínima Spring Boot

Este ítem no es un indicador de evaluación formal, pero es parte de las buenas prácticas que debes incorporar desde el inicio.

Checklist:

- [x] Existe `src/main/resources/application.properties` con `spring.application.name=Tickets`
- [ ] Migrar configuración a `application.yaml` *(pendiente)*
- [ ] Configurar `server.port` con un puerto personalizado *(pendiente)*
- [ ] Configurar `server.servlet.context-path` con un prefijo global *(pendiente)*
- [ ] Crear `src/main/resources/banner.txt` con un texto personalizado *(pendiente)*

**¿Por qué esto importa?** Un proyecto real siempre tiene configuración externa. Aprender a usarla desde el principio te evita el mal hábito de hardcodear valores en el código.

---

## Indicadores que se trabajan en lecciones siguientes

Los siguientes indicadores están en el horizonte del curso. No se evalúan en esta lección, pero es útil que sepas hacia dónde vamos:

| Indicador | Qué cubre |
|---|---|
| IE 1.2.3 | CRUD completo: crear, leer, actualizar y eliminar tickets |
| IE 1.3.1 | Validaciones de entrada con `@Valid`, `@NotNull`, `@NotBlank` |
| IE 1.3.2 | Manejo global de excepciones con `@ControllerAdvice` |
| IE 1.3.3 | Pruebas automáticas de los endpoints REST |

---

## ¿Completé el mínimo de esta lección?

Marcaste todo lo que corresponde si:

- ✅ Tu proyecto tiene los cuatro paquetes con sus clases (`TicketController`, `TicketService`, `TicketRepository`, `Ticket`)
- ✅ Puedes hacer `GET http://localhost:8080/tickets` y recibir un arreglo JSON con los dos tickets semilla
- ✅ Puedes explicar en tus propias palabras qué hace cada clase y por qué está en su paquete correspondiente





<!-- START OF FILE: docs_lessons_04-responsabilities_05_actividad_individual_users.md -->
# Documento: docs lessons 04-responsabilities 05 actividad individual users
---
# Lección 04 - Actividad individual: recurso `users`

Ahora es tu turno. Esta actividad replica lo que construiste con `Ticket`, pero esta vez para un recurso `User`. El objetivo es que apliques el patrón CSR de forma autónoma, tomando las mismas decisiones de diseño que aprendiste.

> Si no estuviste en clase, lee primero el tutorial paso a paso (`02_guion_paso_a_paso.md`) y el documento de decisiones de diseño (`03_decisiones_rest_y_csr.md`) antes de comenzar esta actividad.

---

## ¿Qué vas a construir?

Un microservicio independiente para gestionar usuarios, con la misma estructura por capas que el proyecto `Tickets`. El entregable mínimo es un único endpoint:

```
GET /users
```

Que devuelve una lista JSON de usuarios cargados en memoria.

---

## Restricciones de la actividad

Estas restricciones no son caprichosas: están pensadas para que practiques exactamente lo que se evaluará.

| Restricción | Por qué |
|---|---|
| Usar el patrón CSR con paquetes separados | Es el núcleo de esta lección |
| Usar `List` para persistencia temporal | No usamos BD todavía; el foco es la arquitectura |
| No implementar CRUD completo | Primero estructura, después alcance |
| No hardcodear el puerto en el código Java | La configuración vive en archivos de configuración, nunca en el código |

---

## Modelo sugerido

Crea la clase `User` en el paquete `model`. Usa Lombok para no escribir getters, setters ni constructores manualmente:

```java
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
}
```

> **¿Por qué `name` y `email` en inglés?** Seguimos la misma convención que en `Ticket`: los identificadores de código en inglés. Si los datos que el usuario ve en pantalla deben estar en español, eso se maneja en la capa de presentación, no en el modelo.

---

## Guía de implementación

Sigue exactamente el mismo orden que en el tutorial de tickets:

### 1. Crea el paquete y la clase `User`

Campos: `id` (`Long`), `name` (`String`), `email` (`String`) con las anotaciones Lombok.

### 2. Crea `UserRepository`

- Anótala con `@Repository`
- Inicializa una `List<User>` en el constructor con al menos 2 usuarios de prueba
- Crea el método `getAll()` que retorne la lista

### 3. Crea `UserService`

- Anótala con `@Service`
- Recibe `UserRepository` por constructor (inyección de dependencias)
- Crea el método `getUsers()` que llame a `repository.getAll()`

### 4. Crea `UserController`

- Anótalo con `@RestController` y `@RequestMapping("/users")`
- Recibe `UserService` por constructor
- Crea el método `getAllUsers()` con `@GetMapping` que retorne `this.service.getUsers()`

### 5. Prueba el endpoint

Levanta la aplicación y haz una petición `GET http://localhost:8080/users` en Postman o Insomnia. Deberías recibir:

```json
[
  { "id": 1, "name": "...", "email": "..." },
  { "id": 2, "name": "...", "email": "..." }
]
```

---

## ¿Cómo sé si lo hice bien?

### Logro alto

- Los cuatro paquetes existen: `controller`, `service`, `repository`, `model`
- `UserController` no tiene ninguna lista ni lógica de negocio: solo llama al `service`
- El endpoint `GET /users` responde `200 OK` con JSON válido
- El método del controlador se llama `getAllUsers()` y la URL es `/users` (sin verbos)
- Puedes explicar en voz alta por qué cada clase está en su paquete

### Logro medio

- La estructura CSR existe pero hay alguna mezcla menor (por ejemplo, lógica simple en el `Controller`)
- El endpoint funciona pero el nombre de la URL o del método no sigue las convenciones
- La respuesta JSON es correcta, pero no puedes justificar las decisiones tomadas

### Logro inicial

- El endpoint funciona, pero todo (o casi todo) está en una sola clase
- La URL contiene verbos o no sigue convenciones REST
- No hay separación clara entre lo que hace cada capa

---

## Extensión opcional: si terminas antes

Si completaste todo lo anterior y quieres un desafío adicional:

### Opción A: buscar por ID

Agrega un endpoint que devuelva un usuario por su `id`:

```
GET /users/{id}
```

- Si el usuario existe, devuelve `200 OK` con el objeto JSON
- Si el usuario **no** existe, devuelve `404 Not Found`

Piensa en qué capa va la lógica de búsqueda antes de escribir el código. Pista: `Optional<User>` puede ser útil aquí.

### Opción B: configurar la aplicación

Aunque es una tarea pendiente, puedes practicar creando un `application.yaml` con:

```yaml
server:
  port: 8082
  servlet:
    context-path: /users-app
```

Y creando `src/main/resources/banner.txt` con el nombre de tu proyecto. Recuerda que si agregas el `context-path`, tu endpoint quedaría en `GET /users-app/users`.

---

## Antes de entregar: pregúntate esto

1. ¿Puedo tocar solo `UserRepository` para cambiar la fuente de datos (de `List` a base de datos) sin modificar `UserService` ni `UserController`?
2. Si un compañero abre mi proyecto, ¿entiende a simple vista dónde vive cada responsabilidad?
3. ¿Mi URL (`/users`) describe un recurso o una acción?

Si las tres respuestas son "sí", completaste el objetivo de esta actividad.



