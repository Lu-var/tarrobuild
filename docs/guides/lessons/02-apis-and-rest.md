<!-- START OF FILE: docs_lessons_02-apis-and-rest_01_objetivo_y_alcance.md -->
# Documento: docs lessons 02-apis-and-rest 01 objetivo y alcance
---
# Lección 02 - Arquitecturas, APIs y REST: ¿qué vas a aprender?

## ¿De dónde venimos?

En la lección anterior aprendiste los fundamentos de la comunicación web: qué es HTTP, cómo funciona el intercambio de peticiones y respuestas, qué significan los métodos y los códigos de estado. Tienes el vocabulario del protocolo.

Ahora vamos a subir un nivel de abstracción. Vamos a hablar de **cómo se organizan los sistemas** que usan ese protocolo: quién hace qué, cómo se dividen las responsabilidades, cómo los sistemas se comunican entre sí, y qué convenciones seguimos para que esa comunicación sea predecible y mantenible.

Esta lección te lleva desde "entiendo HTTP" hasta "entiendo cómo se diseña un sistema web moderno". Y eso es exactamente lo que necesitas para que la lección 03 —donde construirás tu primera API— tenga sentido completo.

---

## ¿Qué vas a aprender?

Al terminar esta lección serás capaz de explicar:

- Qué es el **frontend** y qué es el **backend**, y cómo se comunican
- Qué es una arquitectura **monolítica** y cuáles son sus ventajas y desventajas
- Qué son los **microservicios** y en qué contextos tiene sentido usarlos
- Qué es una **API** y para qué sirve
- Qué es **REST** y qué lo diferencia de otros estilos de arquitectura
- Cuáles son las **restricciones de REST** (los 6 principios de Fielding)
- Cuáles son las **buenas prácticas de diseño de APIs REST**: nombres de recursos, uso de métodos, versionado, respuestas consistentes

Esta lección es 100% teórica. No escribirás código, pero al terminarla tendrás los criterios para evaluar si una API está bien o mal diseñada, incluso antes de haber construido la tuya.

---

## ¿Qué NO cubre esta lección? (y por qué)

| Tema | ¿Por qué lo dejamos después? |
|---|---|
| Implementación en Spring Boot | La lección 03 se encarga de eso |
| Autenticación y autorización (JWT, OAuth) | Requiere entender primero qué es una API y cómo funciona |
| GraphQL o gRPC | Son alternativas a REST; primero dominas REST, luego comparas |
| Documentación de APIs (OpenAPI/Swagger) | Se aborda cuando ya tienes endpoints reales que documentar |
| CORS (Cross-Origin Resource Sharing) | Es un problema que aparece cuando el frontend y el backend están en dominios distintos; lo resolvemos cuando lo encontremos |

---

## La idea central de esta lección

> "Una API REST no es solo un servidor que responde JSON. Es un contrato de comunicación con reglas claras. Aprende las reglas antes de romperlas."

Muchos desarrolladores crean APIs que "funcionan" pero que no siguen las convenciones REST. Eso no importa mientras trabajan solos, pero se convierte en un problema cuando el equipo crece, cuando hay que mantener el sistema o cuando otro servicio necesita consumir esa API sin documentación.

Aprender las convenciones desde el principio te ahorra deuda técnica en el futuro.

---

## Estructura de esta lección

| Archivo | Contenido |
|---|---|
| `01_objetivo_y_alcance.md` | Este archivo: qué aprenderás y por qué |
| `02_arquitecturas_y_roles.md` | Frontend vs Backend, monolito vs microservicios |
| `03_apis_rest_y_buenas_practicas.md` | Qué es una API, qué es REST, principios y buenas prácticas |
| `04_checklist_rubrica_minima.md` | Criterios mínimos de evaluación |
| `05_actividad_individual.md` | Actividades investigativas y reflexivas |






<!-- START OF FILE: docs_lessons_02-apis-and-rest_02_arquitecturas_y_roles.md -->
# Documento: docs lessons 02-apis-and-rest 02 arquitecturas y roles
---
# Lección 02 - Arquitecturas y roles: Frontend, Backend, Monolito y Microservicios

Esta sección explora cómo se organizan los sistemas modernos. Antes de construir una API, necesitas entender el contexto en el que vivirá.

---

## Frontend y Backend: dos mundos que se comunican

Cuando usas una aplicación web, hay dos mundos trabajando juntos aunque nunca los ves al mismo tiempo.

### ¿Qué es el Frontend?

El **frontend** es todo lo que el usuario ve e interactúa directamente. Se ejecuta en el dispositivo del usuario (el navegador, el teléfono), no en el servidor.

Tecnologías típicas de frontend:
- **HTML** → la estructura del contenido
- **CSS** → el estilo visual
- **JavaScript** → la interactividad y lógica del lado del cliente
- **Frameworks:** React, Angular, Vue.js, Svelte

El frontend tiene una limitación fundamental: **no puede guardar datos de forma permanente ni segura**. Puede guardar cosas temporalmente en el navegador (localStorage, cookies), pero esos datos son accesibles y manipulables por el usuario. Para cualquier operación que requiera datos persistentes, lógica de negocio protegida o comunicación con una base de datos, el frontend necesita hablar con el backend.

### ¿Qué es el Backend?

El **backend** es todo lo que corre en el servidor: la lógica de negocio, el acceso a la base de datos, la autenticación, el procesamiento de datos. El usuario nunca lo ve directamente.

Tecnologías típicas de backend:
- **Java + Spring Boot** (lo que usarás en este curso)
- Node.js + Express / NestJS
- Python + Django / FastAPI
- C# + ASP.NET Core
- Go, Rust, Ruby on Rails, PHP Laravel, etc.

El backend tiene acceso a recursos que el frontend no puede tener:
- La base de datos (con credenciales que nunca deben llegar al cliente)
- Servicios externos (APIs de pago, correo, etc.)
- El sistema de archivos del servidor
- Claves y secretos de configuración

### ¿Cómo se comunican?

A través de HTTP, usando el patrón que aprendiste en la lección anterior:

```mermaid
sequenceDiagram
    participant FE as Navegador / App móvil<br/>(Frontend)
    participant BE as Servidor Backend<br/>(Spring Boot)
    participant DB as Base de datos
    
    FE->>BE: GET /productos
    BE->>DB: SELECT * FROM...
    DB-->>BE: [{id:1,...}, ...]
    BE-->>FE: 200 OK [{...}, ...]
```

El frontend envía peticiones HTTP al backend. El backend consulta lo que necesita (base de datos, otros servicios) y devuelve una respuesta, generalmente en formato JSON. El frontend usa esa respuesta para mostrar información al usuario.

### El contrato: la API

La "interfaz" entre el frontend y el backend se llama **API**. Es el conjunto de URLs, métodos y formatos de datos que el backend expone para que el frontend (u otros clientes) los consuman. Lo veremos en detalle en la siguiente sección.

---

## Separación de responsabilidades: ¿por qué no todo en uno?

En los primeros días de la Web, existía el concepto de **aplicaciones "full page"**: el servidor generaba el HTML completo y lo enviaba al navegador. No había separación: el backend generaba la presentación.

Hoy ese modelo todavía existe (se llama **Server-Side Rendering** o SSR), pero la tendencia dominante en aplicaciones modernas es la separación clara entre frontend y backend, con un API como contrato entre ellos.

**Ventajas de separar frontend y backend:**

| Ventaja | Descripción |
|---|---|
| **Un backend, múltiples clientes** | El mismo API puede ser consumido por una app web, una app móvil iOS, una app Android y un bot, todos al mismo tiempo |
| **Equipos independientes** | El equipo de frontend y el de backend pueden trabajar en paralelo mientras respeten el contrato del API |
| **Escalabilidad diferenciada** | Puedes escalar el backend sin tocar el frontend y viceversa |
| **Tecnología independiente** | El frontend puede ser React mientras el backend es Spring Boot; no importa |

---

## Arquitecturas de sistema: Monolito vs Microservicios

Una vez que tienes claro que el backend existe, surge la pregunta: ¿cómo se organiza internamente?

### Arquitectura monolítica

En una arquitectura **monolítica**, toda la lógica del backend vive en **una sola aplicación** que se despliega como una unidad.

```mermaid
flowchart TB
    subgraph Monolito[Monolito]
        direction LR
        M1[Módulo<br/>Usuarios]
        M2[Módulo<br/>Productos]
        M3[Módulo<br/>Pedidos]
    end
    DB[(Base de datos<br/>únida)]
    M1 --- DB
    M2 --- DB
    M3 --- DB
```

**Ventajas del monolito:**

- Más simple de desarrollar al principio
- Más fácil de depurar (todo está en un lugar)
- Una sola base de código para mantener
- No hay latencia de red entre módulos
- Despliegue simple: una sola aplicación

**Desventajas del monolito:**

- A medida que crece, se vuelve difícil de entender y modificar
- Un fallo en un módulo puede tumbar toda la aplicación
- Escalar implica escalar todo, aunque solo un módulo lo necesite
- Los equipos grandes se pisan entre sí en la misma base de código
- El ciclo de despliegue se ralentiza cuando el código crece

> **Para este curso:** construirás monolitos. Es el punto de partida correcto. Entender un monolito bien estructurado es prerequisito para entender por qué y cuándo se migra a microservicios.

### Arquitectura de microservicios

En una arquitectura de **microservicios**, el sistema se divide en **muchos servicios pequeños e independientes**, cada uno responsable de una capacidad de negocio específica.

```mermaid
flowchart TB
    S1[Servicio<br/>Usuarios]
    S2[Servicio<br/>Productos]
    S3[Servicio<br/>Pedidos]
    DB1[(DB propia)]
    DB2[(DB propia)]
    DB3[(DB propia)]
    S1 --- DB1
    S2 --- DB2
    S3 --- DB3
    
    S1 -.-> S2
    S2 -.-> S3
    S3 -.-> S1
    note right: se comunican por HTTP
```

**Ventajas de los microservicios:**

- Cada servicio puede desplegarse de forma independiente
- Un fallo en un servicio no necesariamente afecta a los otros
- Escalabilidad granular: escala solo lo que necesita más recursos
- Equipos autónomos: cada equipo es dueño de su servicio
- Libertad tecnológica: cada servicio puede usar el lenguaje que mejor le convenga

**Desventajas de los microservicios:**

- Mucho más complejo de desarrollar, depurar y operar
- Latencia de red entre servicios (lo que antes era una llamada a función ahora es una petición HTTP)
- Necesitas infraestructura adicional: service discovery, API gateway, monitoreo distribuido, etc.
- Los datos distribuidos hacen que las transacciones sean difíciles
- Requiere un equipo maduro con experiencia en operaciones

### ¿Cuándo usar cada uno?

No existe una respuesta única. La decisión depende del contexto:

| Criterio | Monolito | Microservicios |
|---|---|---|
| Tamaño del equipo | Pequeño (1-10 personas) | Grande (muchos equipos) |
| Madurez del producto | Producto nuevo / explorando | Producto establecido con dominio claro |
| Velocidad de desarrollo inicial | Alta | Baja (overhead de infraestructura) |
| Necesidad de escalabilidad | Moderada | Alta y diferenciada por módulo |
| Complejidad operacional tolerable | Baja | Alta |

> **Una opinión respaldada por la industria:** Martin Fowler (autor de "Refactoring" y "Patterns of Enterprise Application Architecture") recomienda empezar con un monolito bien diseñado y migrar a microservicios solo cuando el monolito se convierte en un problema real. Esto se llama el enfoque **"Monolith First"**.

---

## Los tres niveles de un sistema web moderno

Ahora tienes el cuadro completo. Un sistema web moderno típico tiene tres niveles o "capas":

```mermaid
flowchart TB
    subgraph "Nivel 1: Presentación (Frontend)"
        FE[Navegador / App móvil<br/>React, Angular, Vue]
    end
    subgraph "Nivel 2: Aplicación (Backend)"
        BE[Servidor de aplicación<br/>Spring Boot, Node.js<br/>Lógica de negocio, API]
    end
    subgraph "Nivel 3: Datos (Persistencia)"
        DB[(Base de datos<br/>PostgreSQL, MySQL, MongoDB)]
    end
    
    FE -->|HTTP / JSON| BE
    BE -->|SQL| DB
```
Nivel 1: Presentación (Frontend)
         ┌────────────────────────────┐
         │  Navegador / App móvil     │
         │  React, Angular, Vue       │
         └────────────┬───────────────┘
                      │ HTTP / JSON
Nivel 2: Aplicación (Backend)
         ┌────────────▼───────────────┐
         │   Servidor de aplicación   │
         │   Spring Boot, Node.js...  │
         │   Lógica de negocio, API   │
         └────────────┬───────────────┘
                      │ SQL / protocolo propio
Nivel 3: Datos (Persistencia)
         ┌────────────▼───────────────┐
         │        Base de datos       │
         │  PostgreSQL, MySQL, MongoDB│
         └────────────────────────────┘
```

Este curso se enfoca en el **nivel 2**: el backend. Aprenderás a construir el servidor que recibe peticiones HTTP del frontend (nivel 1), aplica lógica de negocio, y consulta o modifica datos en la base de datos (nivel 3).






<!-- START OF FILE: docs_lessons_02-apis-and-rest_03_apis_rest_y_buenas_practicas.md -->
# Documento: docs lessons 02-apis-and-rest 03 apis rest y buenas practicas
---
# Lección 02 - APIs, REST y buenas prácticas de diseño

Esta sección te enseña qué es una API, qué es REST, y cómo diseñar una API que sea intuitiva, predecible y mantenible.

---

## ¿Qué es una API?

**API** significa *Application Programming Interface* (Interfaz de Programación de Aplicaciones). Es un **contrato** que define cómo dos programas pueden comunicarse entre sí.

Una API especifica:
- Qué operaciones están disponibles
- Cómo pedirlas (formato de la petición)
- Qué esperar como resultado (formato de la respuesta)
- Qué errores pueden ocurrir

### La analogía del menú de restaurante

Imagina un restaurante:

- El **cliente** (tú) no entra a la cocina a preparar su comida directamente
- El **menú** define qué puedes pedir, cómo pedirlo (nombre del plato) y qué recibirás
- El **mozo** actúa como intermediario entre el cliente y la cocina
- La **cocina** (el servidor) procesa el pedido y devuelve el resultado

La API es el menú: define las opciones disponibles sin exponer cómo se prepara cada plato internamente. Puedes cambiar completamente la receta (la implementación) sin que el cliente note la diferencia, siempre que el resultado en el plato (la respuesta) sea el mismo.

### Tipos de APIs

| Tipo | Descripción | Ejemplo |
|---|---|---|
| **API Web / HTTP** | Se accede por HTTP. Es la más común en sistemas modernos. | La API de GitHub, Twitter, Google Maps |
| **API de biblioteca** | Funciones que ofrece una librería para ser usadas en el mismo proceso | `java.util.List`, el SDK de AWS |
| **API del sistema operativo** | Interfaz para acceder a recursos del SO | Llamadas del sistema en Linux |

> **En este curso**, cuando decimos "API", nos referimos siempre a una **API Web HTTP**: un servidor que expone URLs y responde peticiones HTTP.

---

## ¿Qué es REST?

**REST** (Representational State Transfer) es un **estilo arquitectónico** para diseñar APIs web. No es un protocolo ni un estándar; es un conjunto de principios y restricciones propuesto por Roy Fielding en su tesis doctoral del año 2000.

Una API que sigue los principios de REST se llama **API RESTful** o simplemente **API REST**.

> **Importante:** REST usa HTTP, pero no toda API que usa HTTP es REST. Una API es REST cuando sigue los principios de Fielding, no simplemente porque use el protocolo.

---

## Los 6 principios (restricciones) de REST

Fielding definió seis restricciones que una arquitectura debe cumplir para ser considerada REST. Vamos a verlas con ejemplos prácticos.

### 1. Interfaz uniforme (Uniform Interface)

Este es el principio más importante. La interfaz entre cliente y servidor debe ser uniforme y consistente. Se descompone en cuatro sub-restricciones:

**a) Identificación de recursos:** cada recurso tiene una URL única que lo identifica.
```
/usuarios/42        → el usuario con ID 42
/productos/15       → el producto con ID 15
/pedidos/7/items    → los ítems del pedido 7
```

**b) Manipulación a través de representaciones:** el cliente interactúa con los recursos a través de representaciones (JSON, XML), no con los datos internos del servidor.

**c) Mensajes auto-descriptivos:** cada mensaje contiene toda la información necesaria para procesarlo (método HTTP, cabeceras, cuerpo con tipo de contenido declarado).

**d) HATEOAS:** Hypermedia As The Engine Of Application State. Las respuestas incluyen enlaces a acciones relacionadas. Es el principio más avanzado y el menos implementado en la práctica.

### 2. Sin estado (Stateless)

Ya lo viste en la lección anterior: cada petición debe contener toda la información necesaria para ser procesada. El servidor no guarda estado de sesión entre peticiones.

```http
GET /pedidos/123 HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

El token de autorización viaja en CADA petición. El servidor no "recuerda" que ya te identificaste en la petición anterior.

**Consecuencia:** la escalabilidad horizontal es trivial. Puedes agregar más servidores porque ninguno guarda estado; cualquier servidor puede atender cualquier petición.

### 3. Caché (Cacheable)

Las respuestas deben indicar si pueden ser cacheadas o no. Si una respuesta puede cachearse, el cliente puede reutilizarla sin hacer una nueva petición al servidor.

```http
HTTP/1.1 200 OK
Cache-Control: max-age=3600
```

Esto le dice al cliente que puede guardar esta respuesta y reutilizarla durante 1 hora sin volver a pedirla.

**Beneficio:** reduce la carga del servidor y mejora el rendimiento del cliente.

### 4. Sistema de capas (Layered System)

El cliente no necesita saber si está hablando directamente con el servidor final o con un intermediario (proxy, load balancer, CDN, gateway). Cada capa solo conoce la capa inmediatamente adyacente.

```
[Cliente] → [CDN / Cache] → [Load Balancer] → [Servidor A]
                                             → [Servidor B]
```

El cliente envía sus peticiones al mismo endpoint sin importar cuántas capas hay en el medio. Esto permite agregar infraestructura sin cambiar el contrato.

### 5. Code on Demand (opcional)

El servidor puede enviar código ejecutable al cliente (ej: JavaScript). Esta es la única restricción opcional de REST. En la práctica, cuando accedes a un sitio web, el servidor envía HTML, CSS y JavaScript que el navegador ejecuta.

### 6. Cliente-Servidor (Client-Server)

El cliente y el servidor deben estar separados y comunicarse solo a través de la interfaz. El cliente no conoce la implementación interna del servidor; el servidor no conoce la implementación interna del cliente.

**Consecuencia:** pueden evolucionar de forma independiente.

---

## REST en la práctica: diseñando una API

Los principios de Fielding son abstractos. En la práctica diaria, "diseñar una API REST" significa tomar decisiones concretas sobre URLs, métodos y respuestas. Estas son las convenciones más importantes.

---

### Regla 1: Los recursos son sustantivos, no verbos

La URL identifica **qué** se está manipulando, no **qué acción** se realiza. La acción la indica el método HTTP.

```
❌ Mal diseño (verbos en la URL)
GET  /getUsuarios
POST /crearUsuario
POST /eliminarUsuario/42
GET  /buscarProductosPorCategoria?cat=ropa

✅ Buen diseño (sustantivos en la URL)
GET    /usuarios
POST   /usuarios
DELETE /usuarios/42
GET    /productos?categoria=ropa
```

> Si sientes la necesidad de poner un verbo en la URL, casi siempre es señal de que el método HTTP no está siendo usado correctamente.

### Regla 2: Usa sustantivos en plural para las colecciones

Las URLs de colecciones (todos los elementos de un tipo) usan el plural. Las URLs de recursos individuales identifican el elemento con un ID.

```
/usuarios          → colección de todos los usuarios
/usuarios/42       → el usuario con ID 42

/productos         → colección de todos los productos
/productos/15      → el producto con ID 15
```

### Regla 3: Jerarquía para recursos relacionados

Cuando un recurso pertenece a otro, la URL refleja esa jerarquía:

```
/pedidos/7/items         → todos los ítems del pedido 7
/pedidos/7/items/3       → el ítem 3 del pedido 7
/usuarios/42/direcciones → las direcciones del usuario 42
```

**Cuidado:** no anides más de 2-3 niveles. Las URLs demasiado largas se vuelven difíciles de manejar.

```
❌ Demasiada jerarquía
/paises/1/regiones/5/comunas/22/direcciones/8/usuarios
```

Si necesitas más contexto, usa query parameters o rediseña el modelo de datos.

### Regla 4: Los métodos HTTP definen la operación

Aplica consistentemente la semántica de los métodos:

| Operación | Método | URL | Respuesta |
|---|---|---|---|
| Listar todos | `GET` | `/usuarios` | `200 OK` + array |
| Ver uno | `GET` | `/usuarios/42` | `200 OK` + objeto |
| Crear | `POST` | `/usuarios` | `201 Created` + objeto creado |
| Reemplazar completo | `PUT` | `/usuarios/42` | `200 OK` + objeto actualizado |
| Actualizar parcial | `PATCH` | `/usuarios/42` | `200 OK` + objeto actualizado |
| Eliminar | `DELETE` | `/usuarios/42` | `204 No Content` |

### Regla 5: Usa los códigos de estado correctamente

No devuelvas siempre `200 OK`. El código de estado es parte del contrato.

```
❌ Mal: devolver 200 con mensaje de error en el cuerpo
HTTP/1.1 200 OK
{ "status": "error", "message": "Usuario no encontrado" }

✅ Bien: usar el código correcto
HTTP/1.1 404 Not Found
{ "error": "Usuario no encontrado" }
```

El cliente (y las herramientas de monitoreo) toman decisiones basadas en el código de estado. Si siempre devuelves `200`, esas herramientas no pueden distinguir éxito de error.

### Regla 6: Consistencia en los nombres

Elige una convención y aplícala en toda la API:

| Aspecto | Opciones | Recomendación |
|---|---|---|
| Caso de URLs | `camelCase`, `kebab-case`, `snake_case` | `kebab-case` (ej: `/mis-pedidos`) |
| Caso de campos JSON | `camelCase`, `snake_case` | `camelCase` en Java/JS (ej: `fechaCreacion`) |
| Idioma | Español, inglés | Elige uno y no lo mezcles |

```
❌ Inconsistente
/usuarios/{userId}/mis_pedidos
{ "fecha_Creacion": "2026-03-19", "Total": 15000 }

✅ Consistente
/usuarios/{userId}/pedidos
{ "fechaCreacion": "2026-03-19", "total": 15000 }
```

### Regla 7: Versionado de la API

Cuando evoluciona una API, hay que gestionar los cambios sin romper a los clientes que ya la usan. El versionado es la estrategia para esto.

Estrategias comunes:

**a) Versión en la URL (más común y explícita):**
```
/v1/usuarios
/v2/usuarios
```

**b) Versión en la cabecera:**
```http
GET /usuarios HTTP/1.1
Accept: application/vnd.miapp.v2+json
```

**c) Versión como query parameter:**
```
/usuarios?version=2
```

> **Recomendación para este curso:** usa versión en la URL (`/v1/...`). Es la más visible, la más fácil de entender y la más fácil de probar.

### Regla 8: Respuestas de error consistentes

Define una estructura de error uniforme para toda la API y úsala siempre:

```json
{
  "timestamp": "2026-03-19T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe un usuario con ID 42",
  "path": "/v1/usuarios/42"
}
```

Si tu API siempre devuelve errores en el mismo formato, el frontend puede manejarlos de forma genérica sin casos especiales.

### Regla 9: Filtrado, paginación y ordenamiento

Para colecciones grandes, no devuelvas todos los registros en una sola respuesta. Usa query parameters:

```
/productos?categoria=ropa                  → filtrar
/productos?pagina=2&tamano=20              → paginar
/productos?orden=precio&direccion=asc      → ordenar
/productos?categoria=ropa&pagina=1&orden=precio  → combinar
```

La respuesta debe incluir metadata sobre la paginación:

```json
{
  "datos": [{...}, {...}],
  "pagina": 2,
  "tamano": 20,
  "total": 347,
  "paginas": 18
}
```

---

## REST vs otras alternativas

REST no es la única forma de diseñar APIs. Existen otras que vale conocer:

| Tecnología | Descripción | Cuándo usarla |
|---|---|---|
| **REST** | Basada en HTTP y recursos. Simple y ampliamente soportada. | La mayoría de los casos: APIs públicas, servicios web generales |
| **GraphQL** | El cliente define exactamente qué datos quiere. Un solo endpoint. | Cuando el cliente necesita mucha flexibilidad en los datos (ej: apps móviles con datos variables) |
| **gRPC** | Basado en Protocol Buffers (binario, eficiente). Tipado estricto. | Comunicación interna entre microservicios que necesitan alto rendimiento |
| **WebSocket** | Conexión bidireccional persistente. El servidor puede enviar datos sin que el cliente pregunte. | Aplicaciones en tiempo real: chat, notificaciones en vivo, juegos |

> **Para este curso:** REST es suficiente para todo lo que haremos. GraphQL y gRPC son temas avanzados que encontrarás en proyectos más complejos.

---

## El Modelo de Madurez de Richardson

Leonard Richardson propuso un modelo para medir qué tan "RESTful" es una API, con cuatro niveles:

| Nivel | Nombre | Descripción |
|---|---|---|
| **Nivel 0** | The Swamp of POX | Un solo endpoint, todo vía POST. (Ej: SOAP, XML-RPC) |
| **Nivel 1** | Resources | Múltiples URLs, una por recurso. Pero un solo método HTTP para todo. |
| **Nivel 2** | HTTP Verbs | Usa correctamente los métodos HTTP y los códigos de estado. |
| **Nivel 3** | Hypermedia Controls (HATEOAS) | Las respuestas incluyen enlaces a acciones relacionadas. |

La mayoría de las APIs "REST" reales están en el **Nivel 2**. El Nivel 3 (HATEOAS) es el verdadero REST según Fielding, pero rara vez se implementa completamente en la práctica.

> **En este curso:** construirás APIs de Nivel 2. Es el estándar de la industria. Conocer el Nivel 3 es importante conceptualmente, pero no es el foco práctico.

---

## Resumen: checklist de diseño de una API REST

Antes de implementar un endpoint, hazte estas preguntas:

- [ ] ¿La URL usa sustantivos (no verbos)?
- [ ] ¿Las colecciones están en plural?
- [ ] ¿El método HTTP refleja correctamente la operación?
- [ ] ¿El código de estado HTTP refleja correctamente el resultado?
- [ ] ¿Los nombres de campos son consistentes con el resto de la API?
- [ ] ¿Las respuestas de error tienen el mismo formato en toda la API?
- [ ] ¿Las colecciones grandes están paginadas?
- [ ] ¿La URL incluye un número de versión?

Si puedes marcar todos estos ítems, tienes una API bien diseñada.






<!-- START OF FILE: docs_lessons_02-apis-and-rest_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 02-apis-and-rest 04 checklist rubrica minima
---
# Lección 02 - Lista de verificación: ¿llegué al mínimo requerido?

Usa esta lista para revisar tu comprensión antes de avanzar a la lección 03, donde vas a construir tu primera API. Si no puedes responder las preguntas de verificación sin releer el material, vuelve a la sección correspondiente.

---

## IE 2.0.1 - Frontend vs Backend

Checklist:

- [ ] Puedo explicar qué es el frontend con un ejemplo concreto
- [ ] Puedo explicar qué es el backend con un ejemplo concreto
- [ ] Entiendo por qué el frontend no puede guardar datos sensibles de forma segura
- [ ] Puedo describir cómo el frontend y el backend se comunican

**Pregunta de verificación:** Un amigo te dice que quiere construir una aplicación de notas con login de usuario. ¿Qué partes serían frontend y qué partes serían backend? ¿Por qué el login debe procesarse en el backend y no en el frontend?

---

## IE 2.0.2 - Arquitectura monolítica

Checklist:

- [ ] Puedo definir qué es una arquitectura monolítica
- [ ] Puedo mencionar al menos 3 ventajas del monolito
- [ ] Puedo mencionar al menos 3 desventajas del monolito
- [ ] Entiendo por qué es el punto de partida correcto para proyectos nuevos

**Pregunta de verificación:** Una startup con 3 desarrolladores está construyendo su primer producto. ¿Le recomendarías una arquitectura monolítica o de microservicios? Justifica tu respuesta con al menos dos argumentos.

---

## IE 2.0.3 - Microservicios

Checklist:

- [ ] Puedo definir qué es una arquitectura de microservicios
- [ ] Puedo comparar microservicios con monolito en al menos 3 dimensiones
- [ ] Entiendo qué problemas resuelven los microservicios y cuáles crean
- [ ] Entiendo en qué contexto tiene sentido migrar de monolito a microservicios

**Pregunta de verificación:** Una empresa grande tiene un monolito que procesa pagos, gestiona inventario y envía emails de notificación. El módulo de pagos necesita escalarse 10 veces más que los otros. ¿Por qué en este caso los microservicios podrían justificarse? ¿Qué nuevo problema aparecería al separarlos?

---

## IE 2.0.4 - Qué es una API

Checklist:

- [ ] Puedo definir qué es una API en términos generales
- [ ] Puedo explicar la analogía del menú de restaurante y qué representa cada parte
- [ ] Entiendo por qué la implementación interna puede cambiar sin afectar al cliente
- [ ] Puedo diferenciar entre una API web/HTTP y otros tipos de API

**Pregunta de verificación:** Cuando usas Google Maps en tu aplicación de delivery, ¿qué rol cumple la API de Google Maps? ¿Qué pasa si Google cambia internamente cómo calcula las rutas? ¿Afecta a tu aplicación?

---

## IE 2.0.5 - Qué es REST y sus principios

Checklist:

- [ ] Puedo explicar qué es REST con mis propias palabras (sin copiar la definición)
- [ ] Puedo nombrar al menos 4 de los 6 principios de REST
- [ ] Entiendo qué significa "interfaz uniforme" y por qué es el principio más importante
- [ ] Entiendo la relación entre REST y HTTP (REST usa HTTP pero no toda API HTTP es REST)

**Pregunta de verificación:** Un colega dice "nuestra API es REST porque usa HTTP y devuelve JSON". ¿Estás de acuerdo? ¿Qué criterios adicionales debería cumplir para ser genuinamente REST?

---

## IE 2.0.6 - Buenas prácticas de diseño de APIs REST

Checklist:

- [ ] Entiendo por qué las URLs deben tener sustantivos, no verbos
- [ ] Puedo diseñar las URLs para un CRUD completo dado un recurso
- [ ] Puedo elegir el método HTTP y código de estado correcto para cada operación
- [ ] Entiendo qué es el versionado de API y por qué existe
- [ ] Entiendo cómo implementar filtrado y paginación con query parameters

**Pregunta de verificación:** Diseña las URLs y métodos HTTP para una API de gestión de libros de una biblioteca. Debe permitir: listar todos los libros, ver un libro específico, agregar un libro nuevo, actualizar la cantidad de ejemplares disponibles, y eliminar un libro del catálogo. ¿Qué código de estado devuelves en cada caso?

---

## IE 2.0.7 - El Modelo de Madurez de Richardson

Checklist:

- [ ] Puedo describir los 4 niveles del Modelo de Madurez de Richardson
- [ ] Puedo identificar en qué nivel está una API dado un ejemplo
- [ ] Entiendo en qué nivel se trabaja en la industria habitualmente
- [ ] Entiendo qué es HATEOAS aunque no lo implementes en este curso

**Pregunta de verificación:** Una API tiene un único endpoint `POST /api` que recibe en el cuerpo un campo `accion` con valores como `"listar_usuarios"`, `"crear_usuario"`, `"eliminar_usuario"`. ¿En qué nivel de Richardson está? ¿Cómo la mejorarías para llevarla al Nivel 2?






<!-- START OF FILE: docs_lessons_02-apis-and-rest_05_actividad_individual.md -->
# Documento: docs lessons 02-apis-and-rest 05 actividad individual
---
# Lección 02 - Actividad individual: investigación y reflexión

Esta actividad no tiene código. Requiere que investigues, analices sistemas reales y defiendas tus propias conclusiones. Las actividades de investigación buscan que salgas de los ejemplos del curso; las reflexivas buscan que desarrolles criterio propio.

> **Formato de entrega:** un documento Markdown (`.md`) con tus respuestas. Cada sección debe tener título y respuesta redactada en tus propias palabras. No copies definiciones de Wikipedia o ChatGPT sin reelaborarlas.

---

## Parte 1: Actividades investigativas

---

### 🔍 Investigación 2.1 — Analiza una API pública real

**Objetivo:** aplicar los conceptos de diseño REST a una API del mundo real.

**Instrucciones:**

Elige UNA de las siguientes APIs públicas y gratuitas:
- [GitHub REST API](https://docs.github.com/en/rest)
- [The Dog API](https://thedogapi.com/)
- [Open-Meteo (meteorología)](https://open-meteo.com/)
- [PokéAPI](https://pokeapi.co/)
- [JSONPlaceholder (API de prueba)](https://jsonplaceholder.typicode.com/)

**Responde en tu documento:**

a) ¿Qué API elegiste? ¿Cuál es su propósito?

b) Encuentra al menos 3 endpoints diferentes en su documentación. Para cada uno, indica:
   - URL completa
   - Método HTTP
   - Qué hace
   - Qué código de estado devuelve en caso de éxito

c) Usando curl o el navegador, haz una petición real a uno de esos endpoints y copia la respuesta completa (URL, código de estado, y primeras líneas del cuerpo JSON).

d) ¿Qué nivel del Modelo de Madurez de Richardson tiene esta API? Justifica con evidencia de su documentación o comportamiento.

e) Identifica al menos UN aspecto de diseño que te parezca bueno y explica por qué. Identifica al menos UN aspecto que mejorarías y explica cómo.

---

### 🔍 Investigación 2.2 — Monolito vs Microservicios: casos reales

**Objetivo:** entender las decisiones arquitectónicas reales y sus consecuencias.

**Instrucciones:** investiga los siguientes casos documentados públicamente:

- **Amazon** pasó de monolito a microservicios alrededor de 2001-2002
- **Netflix** migró de DVD-por-correo a streaming y restructuró completamente su arquitectura
- **Shopify** es una empresa grande que todavía defiende el monolito (busca "Shopify monolith")
- **Stack Overflow** sirve millones de páginas con muy pocos servidores y arquitectura simple

**Responde en tu documento:**

a) Elige DOS de estos casos. Para cada uno, explica:
   - ¿Cuál era el problema que tenían con su arquitectura anterior?
   - ¿Qué decisión arquitectónica tomaron?
   - ¿Qué resultado obtuvieron?

b) ¿Por qué crees que empresas tan grandes como Shopify o Stack Overflow defienden el monolito si tienen los recursos para hacer microservicios?

c) Un desarrollador junior te dice "voy a usar microservicios para mi proyecto de portafolio personal". ¿Qué le respondes?

---

### 🔍 Investigación 2.3 — Tesis doctoral de Roy Fielding

**Objetivo:** conocer el origen de REST desde la fuente primaria.

**Instrucciones:** Lee el capítulo 5 de la tesis de Fielding, disponible gratuitamente en inglés en: [https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)

No tienes que entenderla completamente. Es un documento académico denso. Lee con el objetivo de responder las siguientes preguntas.

**Responde en tu documento:**

a) Según Fielding, ¿qué motivó el diseño de REST? ¿Qué problema intentaba resolver?

b) Fielding escribió REST específicamente pensando en la Web (HTTP). ¿Crees que REST sigue siendo relevante en 2026? ¿Por qué?

c) Muchas APIs que se llaman a sí mismas "REST" no implementan HATEOAS (el nivel 3 de Richardson). Fielding ha criticado esto públicamente varias veces. ¿Quién tiene razón: la industria que ignora HATEOAS o Fielding que lo considera obligatorio? Defiende tu posición.

---

## Parte 2: Actividades reflexivas

---

### 💭 Reflexión 2.4 — Diseña una API desde cero

**Objetivo:** aplicar las buenas prácticas de diseño REST a un problema real.

**Escenario:** estás diseñando la API backend para una aplicación de gestión de tareas (como Trello o Todoist). Los usuarios pueden crear proyectos, y dentro de cada proyecto pueden crear tareas. Las tareas tienen estado (pendiente, en progreso, completada) y pueden tener comentarios.

**Responde en tu documento:**

a) Define todos los recursos del sistema (mínimo 3).

b) Diseña las URLs para el CRUD completo de cada recurso. Incluye método HTTP y código de estado esperado para cada operación.

c) Diseña la estructura JSON de respuesta para al menos uno de los recursos (incluye todos los campos que tendría).

d) ¿Cómo implementarías filtrado? (ej: "solo tareas completadas", "tareas del proyecto X"). Muestra ejemplos de URLs.

e) Si un usuario intenta crear una tarea en un proyecto que no existe, ¿qué código de estado devuelves? ¿Qué cuerpo de respuesta?

---

### 💭 Reflexión 2.5 — Los límites de REST

REST es la arquitectura dominante para APIs web, pero no es perfecta para todos los casos.

**Responde en tu documento:**

a) Imagina una aplicación de chat en tiempo real (como Slack o WhatsApp Web). ¿Por qué el modelo petición-respuesta de REST no es ideal para este caso? ¿Qué alternativa usarías?

b) Imagina una aplicación móvil que necesita mostrar el perfil de un usuario, sus últimas 5 publicaciones y sus 3 amigos más recientes, todo en una sola pantalla. Con REST, ¿cuántas peticiones HTTP necesitarías mínimo? ¿Qué problema crea esto en redes lentas? ¿Cómo lo resolverías?

c) GraphQL fue diseñado explícitamente para resolver algunos de estos problemas. ¿Qué problema concreto del punto anterior resuelve GraphQL? ¿Significa eso que GraphQL es "mejor" que REST? ¿En qué contexto elegiría cada uno?

---

### 💭 Reflexión 2.6 — APIs como producto

Las APIs no son solo código técnico: son interfaces que otros equipos o empresas dependen. Un mal diseño de API puede generar meses de trabajo adicional para quienes la consumen.

**Responde en tu documento:**

a) Si construyes una API que usa rutas como `/getUsers`, `/createUser` y `/deleteUser`, ¿qué problemas concretos le causas a un desarrollador frontend que la consume?

b) Imagina que lanzas la versión 1 de tu API con el campo `nombre_completo` en los usuarios. Seis meses después quieres cambiar ese campo a `nombreCompleto` (camelCase). ¿Cómo manejarías este cambio sin romper a los clientes que ya usan la v1?

c) Stripe es conocida en la industria por tener una de las mejores APIs del mundo. Investiga brevemente por qué la comunidad de desarrolladores la considera tan buena. ¿Qué principios de diseño aplica que la hacen destacar?

---

## Preparación para la lección 03

Al terminar esta actividad, ya tienes todo el modelo mental necesario para la lección siguiente. Como preparación adicional:

**Haz esto antes de la lección 03:**

- [ ] Descarga e instala [IntelliJ IDEA Community o Ultimate](https://www.jetbrains.com/idea/download/)
- [ ] Descarga e instala [Java 21 (JDK)](https://adoptium.net/) si no lo tienes
- [ ] Instala [Postman](https://www.postman.com/downloads/) para probar endpoints
- [ ] Verifica que Java está instalado ejecutando `java -version` en la terminal (debe mostrar versión 21)

**Pregunta para traer respondida a clase:**

> "¿Qué crees que hace Spring Boot que hace que crear un servidor HTTP sea tan simple?" Busca una respuesta de no más de 3 oraciones. La verificaremos y profundizaremos en la lección 03.

---

## Criterios de evaluación de la actividad

| Criterio | Descripción |
|---|---|
| **Profundidad** | Las respuestas van más allá de la definición superficial; demuestran comprensión real |
| **Evidencia** | Las investigaciones citan fuentes, muestran URLs probadas o capturas de respuestas reales |
| **Argumento** | Las reflexiones defienden una posición con argumentos concretos, no solo opiniones |
| **Aplicación** | El diseño de la API del ejercicio 2.4 aplica correctamente las buenas prácticas vistas |
| **Formato** | El documento está en Markdown con estructura clara, títulos y secciones bien organizadas |

---

## Recursos sugeridos para investigar

- [Roy Fielding's Dissertation - Chapter 5 REST](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm) — La fuente original de REST
- [Richardson Maturity Model - Martin Fowler](https://martinfowler.com/articles/richardsonMaturityModel.html) — Explicación clara del modelo de madurez
- [REST API Design Best Practices - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/best-practices/api-design) — Guía práctica de Microsoft
- [Stripe API Reference](https://stripe.com/docs/api) — Ejemplo de API bien diseñada
- [GitHub REST API](https://docs.github.com/en/rest) — Otra API pública bien diseñada para estudiar
- [Microservices - Martin Fowler](https://martinfowler.com/articles/microservices.html) — El artículo que popularizó el término microservicios
- [Monolith First - Martin Fowler](https://martinfowler.com/bliki/MonolithFirst.html) — El argumento a favor de empezar con monolito




