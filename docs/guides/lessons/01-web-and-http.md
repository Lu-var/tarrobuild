<!-- START OF FILE: docs_lessons_01-web-and-http_01_objetivo_y_alcance.md -->
# Documento: docs lessons 01-web-and-http 01 objetivo y alcance
---
# Lección 01 - La Web y HTTP: ¿qué vas a aprender?

## ¿De dónde venimos?

Esta es la primera lección del curso. Llegas sin conocimientos previos de desarrollo backend, o quizás con algo de experiencia en frontend, pero sin claridad sobre qué pasa "del otro lado". Este es el punto de partida.

Antes de escribir una sola línea de código de servidor, necesitas entender el terreno en el que vas a trabajar: **cómo funciona la web** y **cómo se comunican los programas a través de internet**.

---

## ¿Qué vas a aprender?

Al terminar esta lección serás capaz de explicar:

- Qué es la Web y cómo se diferencia de Internet
- Qué rol cumplen el **cliente** y el **servidor** en una comunicación web
- Qué hace el **DNS** y para qué sirve
- Cómo funciona el protocolo **HTTP**: qué es, por qué existe y qué problema resuelve
- Qué partes tiene una **petición HTTP** (request)
- Qué partes tiene una **respuesta HTTP** (response)
- Qué son los **métodos HTTP** y cuándo se usa cada uno
- Qué son los **códigos de estado HTTP** y cómo interpretarlos

Esta lección es 100% teórica. No vas a escribir código. Pero al terminarla tendrás el vocabulario y el modelo mental que necesitas para que todo lo que venga después tenga sentido.

---

## ¿Qué NO cubre esta lección? (y por qué)

| Tema | ¿Por qué lo dejamos después? |
|---|---|
| APIs REST | Primero necesitas entender HTTP; la API es una capa que se construye sobre él |
| Backend vs Frontend | Lo veremos en la lección siguiente, cuando ya tengas el protocolo claro |
| Microservicios | Es un concepto arquitectónico que requiere entender primero qué es un servidor |
| Código Java o Spring Boot | El código viene después; primero el modelo mental |
| HTTPS y seguridad | Es la versión segura de HTTP; se aborda después de entender HTTP básico |

---

## La idea central de esta lección

> "No puedes construir una API sin entender qué hay debajo. HTTP es el idioma; antes de hablar, hay que aprenderlo."

El protocolo HTTP no es magia. Es un conjunto de reglas que todos los programas siguen para poder entenderse. Una vez que lo comprendes, puedes leer cualquier petición o respuesta web como si fuera texto plano, porque en realidad **es** texto plano.

---

## Estructura de esta lección

| Archivo | Contenido |
|---|---|
| `01_objetivo_y_alcance.md` | Este archivo: qué aprenderás y por qué |
| `02_la_web_y_http.md` | La Web, el modelo cliente-servidor, DNS y HTTP |
| `03_request_response_y_codigos.md` | Anatomía de una petición, de una respuesta, métodos y códigos de estado |
| `04_checklist_rubrica_minima.md` | Criterios mínimos de evaluación |
| `05_actividad_individual.md` | Actividades investigativas y reflexivas |






<!-- START OF FILE: docs_lessons_01-web-and-http_02_la_web_y_http.md -->
# Documento: docs lessons 01-web-and-http 02 la web y http
---
# Lección 01 - La Web y HTTP: cómo funciona todo

Esta sección no es un glosario para memorizar. Es la explicación del mecanismo real detrás de cada sitio web que has usado en tu vida. Léela con calma, más de una vez si es necesario.

---

## Internet vs. la Web: no son lo mismo

Un error muy común es usar "Internet" y "Web" como sinónimos. No lo son.

- **Internet** es la infraestructura: la red física global de cables, routers, fibra óptica y antenas que conecta dispositivos entre sí. Es como la red de carreteras del país.
- **La Web** (World Wide Web) es un *servicio* que corre sobre esa infraestructura. Es el sistema que permite acceder a páginas y recursos usando el protocolo HTTP. Es como el sistema de transporte que usa esas carreteras.

Existen otros servicios que también usan Internet pero no son la Web: el correo electrónico (protocolo SMTP/IMAP), la transferencia de archivos (FTP), los videojuegos en línea, etc.

> **En este curso trabajamos con la Web**: construimos servidores que hablan HTTP y responden peticiones que llegan desde navegadores, aplicaciones móviles u otros servicios.

---

## El modelo cliente-servidor

Toda comunicación en la Web sigue el mismo patrón básico:

```mermaid
sequenceDiagram
    participant Client as Cliente
    participant Server as Servidor
    Client->>Server: petición (request)
    Server-->>Client: respuesta (response)
```

### ¿Qué es un cliente?

Un **cliente** es cualquier programa que *inicia* una comunicación y *solicita* algo. Ejemplos:

- Un navegador web (Chrome, Firefox, Safari)
- Una aplicación móvil que consulta datos de un servidor
- Postman o Insomnia (herramientas para probar APIs)
- Otro servidor que consume servicios de un tercero

El cliente **siempre inicia** la comunicación. El servidor *espera* y *responde*.

### ¿Qué es un servidor?

Un **servidor** es un programa (no necesariamente una máquina física especial) que *escucha* peticiones y *devuelve* respuestas. Ejemplos:

- Un servidor web como Apache o Nginx que sirve archivos HTML
- Una aplicación Spring Boot que devuelve datos en JSON
- Un servidor de base de datos que responde consultas SQL

La diferencia clave no está en el hardware, sino en el rol: el servidor escucha y responde; el cliente pregunta y espera.

> **Importante:** un mismo programa puede ser cliente y servidor al mismo tiempo. Por ejemplo, tu aplicación Spring Boot es un servidor para el navegador, pero actúa como cliente si consulta una base de datos o llama a otra API.

---

## ¿Cómo sabe el cliente dónde encontrar al servidor?

Cuando escribes `https://www.duoc.cl` en el navegador, no escribes una dirección IP como `200.27.240.10`. Escribes un nombre legible para humanos. Alguien tiene que traducir ese nombre a una dirección real de red. Ese "alguien" es el **DNS**.

### DNS: el directorio telefónico de Internet

**DNS** (Domain Name System) es un sistema distribuido que traduce nombres de dominio legibles (`www.duoc.cl`) a direcciones IP (`200.27.240.10`) que las computadoras usan para encontrarse.

El proceso ocurre automáticamente y en milisegundos, pero los pasos son:

```
1. Escribes "www.duoc.cl" en el navegador
2. Tu computadora pregunta al servidor DNS configurado (usualmente el de tu proveedor de internet o Google 8.8.8.8): "¿Cuál es la IP de www.duoc.cl?"
3. El DNS responde: "200.27.240.10"
4. Tu navegador se conecta directamente a esa IP
5. El servidor en esa IP responde con el contenido
```

> **Analogía:** el DNS es como la agenda de contactos de tu teléfono. Buscas "Mamá" y el teléfono sabe que eso corresponde al número `+56 9 XXXX XXXX`. No tienes que memorizar el número; solo el nombre.

---

## ¿Qué es HTTP?

Una vez que el cliente sabe la dirección IP del servidor, necesita un **lenguaje común** para comunicarse con él. Ese lenguaje es **HTTP**.

**HTTP** (HyperText Transfer Protocol) es el protocolo de comunicación de la Web. Define las reglas sobre:

- **Cómo formatear una petición** para que el servidor la entienda
- **Cómo formatear una respuesta** para que el cliente la entienda
- **Qué tipos de operaciones existen** (obtener, crear, modificar, eliminar)
- **Cómo indicar si algo salió bien o mal**

HTTP es un protocolo **de texto**: las peticiones y respuestas son cadenas de texto con un formato muy específico. No hay magia ni binario. Si pudieras interceptar la comunicación entre tu navegador y un servidor, verías texto plano que cualquier humano puede leer.

### Características fundamentales de HTTP

#### Sin estado (stateless)

HTTP no tiene memoria entre peticiones. Cada petición es completamente independiente de la anterior. El servidor no sabe quién eres ni qué pediste antes, a menos que el cliente le envíe esa información explícitamente (por ejemplo, usando cookies o tokens de autenticación).

> **Implicación práctica:** si quieres que el servidor te "recuerde" entre peticiones (por ejemplo, para mantener una sesión de usuario), tú eres responsable de enviar esa información en cada petición.

#### Basado en texto

Todas las peticiones y respuestas HTTP son texto. Esto las hace fáciles de leer, depurar y entender sin herramientas especiales.

#### Sin conexión persistente por defecto (en HTTP/1.0)

En HTTP/1.0, cada petición abría una nueva conexión TCP y la cerraba al terminar. HTTP/1.1 introdujo las conexiones persistentes (keep-alive) para reutilizar la misma conexión. HTTP/2 y HTTP/3 mejoran esto aún más con multiplexación, pero el modelo básico de petición-respuesta no cambia.

---

## Versiones de HTTP

| Versión | Año | Lo más importante |
|---|---|---|
| HTTP/1.0 | 1996 | El original. Una conexión por petición. |
| HTTP/1.1 | 1997 | Conexiones persistentes. El más usado durante décadas. |
| HTTP/2 | 2015 | Múltiples peticiones en paralelo sobre una sola conexión (multiplexación). Más rápido. |
| HTTP/3 | 2022 | Basado en UDP en lugar de TCP. Más eficiente en redes inestables. |

> **Para este curso** trabajamos con HTTP/1.1. Es el que todos los navegadores, herramientas y servidores soportan sin configuración especial. Los principios que aprenderás aplican igual a HTTP/2 y HTTP/3.

---

## ¿Qué es una URL?

Antes de hablar de peticiones, necesitas entender el formato de una URL (Uniform Resource Locator), que es la dirección que identifica un recurso en la Web.

```
https://api.ejemplo.com:8080/usuarios/42?formato=json#seccion
  │         │              │      │      │              │
  │         │              │      │      │              └─ Fragmento (solo en navegador)
  │         │              │      │      └─ Query string (parámetros)
  │         │              │      └─ Ruta del recurso
  │         │              └─ Puerto (8080)
  │         └─ Dominio (host)
  └─ Protocolo (scheme)
```

| Parte | Nombre | Descripción |
|---|---|---|
| `https` | Protocolo | Cómo se comunican. `http` o `https` (la versión segura) |
| `api.ejemplo.com` | Host / Dominio | Dónde está el servidor |
| `:8080` | Puerto | Por cuál "puerta" entrar. Por defecto es 80 para HTTP y 443 para HTTPS |
| `/usuarios/42` | Ruta (path) | Qué recurso específico se solicita |
| `?formato=json` | Query string | Parámetros opcionales para filtrar o modificar la petición |
| `#seccion` | Fragmento | Posición dentro de la página. Solo lo usa el navegador; no llega al servidor |

> **Para el desarrollo de APIs**, lo que más te interesa es la **ruta** y el **query string**. El protocolo y el host los configura el servidor. Los fragmentos no son relevantes en APIs.

---

## Puertos: las puertas del servidor

Un servidor puede tener muchos servicios corriendo al mismo tiempo. Los **puertos** son la forma de distinguirlos: cada servicio escucha en un puerto diferente, como distintas puertas de un edificio.

Algunos puertos relevantes:

| Puerto | Uso común |
|---|---|
| 80 | HTTP (web sin cifrar) |
| 443 | HTTPS (web con cifrado TLS) |
| 8080 | HTTP alternativo (muy usado en desarrollo) |
| 3306 | MySQL |
| 5432 | PostgreSQL |
| 6379 | Redis |

Cuando desarrollas con Spring Boot, por defecto usa el **puerto 8080**. Por eso accedes a tu aplicación con `http://localhost:8080`.

> **`localhost`** es el nombre de dominio especial que siempre apunta a tu propia máquina. Su IP equivalente es `127.0.0.1`. Cuando dices "abrir `localhost:8080`", estás diciendo "conectarme al servicio que está corriendo en el puerto 8080 de mi propia computadora".

---

## Resumen del flujo completo

Juntando todo lo que vimos, el flujo completo desde que escribes una URL hasta que ves el resultado es:

```
1. Escribes https://www.duoc.cl/noticias en el navegador

2. [DNS] El navegador consulta al DNS: "¿Cuál es la IP de www.duoc.cl?"
         DNS responde: "200.27.240.10"

3. [TCP] El navegador establece una conexión TCP con 200.27.240.10 en el puerto 443 (HTTPS)

4. [TLS] Se negocia el cifrado (porque es HTTPS)

5. [HTTP] El navegador envía una petición HTTP:
         GET /noticias HTTP/1.1
         Host: www.duoc.cl
         ...

6. [Servidor] El servidor procesa la petición y devuelve una respuesta HTTP:
         HTTP/1.1 200 OK
         Content-Type: text/html
         ...
         <html>...</html>

7. [Navegador] El navegador recibe la respuesta y muestra la página
```

Este mismo flujo ocurre, con pequeñas variaciones, en cada petición que hace tu aplicación a un servidor. Cuando construyes una API con Spring Boot, **tu aplicación es el paso 6** de este flujo: espera peticiones y devuelve respuestas.






<!-- START OF FILE: docs_lessons_01-web-and-http_03_request_response_y_codigos.md -->
# Documento: docs lessons 01-web-and-http 03 request response y codigos
---
# Lección 01 - Request, Response, Métodos y Códigos HTTP

Ahora que sabes qué es HTTP y cómo funciona la Web, es hora de ver el contenido de las peticiones y respuestas. Esta sección es especialmente importante porque vas a leer y escribir estas estructuras constantemente cuando trabajes con APIs.

---

## Anatomía de una petición HTTP (Request)

Una petición HTTP tiene siempre la misma estructura: tres partes en un orden específico, separadas por líneas en blanco.

```http
POST /usuarios HTTP/1.1
Host: api.ejemplo.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Accept: application/json

{
  "nombre": "Ana Torres",
  "email": "ana@ejemplo.com"
}
```

### Parte 1: Línea de inicio (Request Line)

```
POST /usuarios HTTP/1.1
```

Contiene exactamente tres elementos:

| Elemento | Qué es | En el ejemplo |
|---|---|---|
| Método | Qué tipo de operación se solicita | `POST` |
| Ruta (path) | Qué recurso se solicita | `/usuarios` |
| Versión HTTP | Qué versión del protocolo se usa | `HTTP/1.1` |

Esta línea es **obligatoria** en toda petición HTTP. Sin ella, el servidor no sabe qué se le está pidiendo.

### Parte 2: Cabeceras (Headers)

```
Host: api.ejemplo.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Accept: application/json
```

Las cabeceras son **metadatos** de la petición: información adicional que le da contexto al servidor sobre quién hace la petición, qué formato tiene el cuerpo, qué formato se espera en la respuesta, etc.

Son pares `Clave: Valor`, uno por línea. Algunas cabeceras comunes:

| Cabecera | Qué informa |
|---|---|
| `Host` | El dominio del servidor al que va la petición. **Obligatoria en HTTP/1.1** |
| `Content-Type` | El formato del cuerpo que se envía (ej: `application/json`, `text/plain`) |
| `Accept` | El formato que el cliente puede recibir en la respuesta |
| `Authorization` | Credenciales o token de autenticación |
| `User-Agent` | Información del cliente (navegador, sistema operativo) |
| `Content-Length` | Tamaño en bytes del cuerpo de la petición |

El cliente puede incluir tantas cabeceras como necesite. El servidor también puede ignorar las que no entiende.

### Parte 3: Cuerpo (Body)

```json
{
  "nombre": "Ana Torres",
  "email": "ana@ejemplo.com"
}
```

El cuerpo contiene **los datos que el cliente envía al servidor**. Es la "carga útil" de la petición. No siempre existe: las peticiones `GET` y `DELETE` generalmente no tienen cuerpo, porque solo piden información o solicitan eliminar algo identificado por la URL, sin necesidad de enviar datos adicionales.

Cuando el cuerpo existe, la cabecera `Content-Type` le indica al servidor cómo interpretarlo.

> **Separador importante:** entre las cabeceras y el cuerpo siempre hay **una línea en blanco**. Esta línea vacía es parte del protocolo; sin ella el servidor no sabe dónde terminan las cabeceras y dónde empieza el cuerpo.

---

## Anatomía de una respuesta HTTP (Response)

La respuesta tiene la misma estructura de tres partes, con una diferencia: la primera línea no es una línea de método sino una **línea de estado**.

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /usuarios/98

{
  "id": 98,
  "nombre": "Ana Torres",
  "email": "ana@ejemplo.com",
  "creadoEn": "2026-03-19T10:30:00Z"
}
```

### Parte 1: Línea de estado (Status Line)

```
HTTP/1.1 201 Created
```

| Elemento | Qué es | En el ejemplo |
|---|---|---|
| Versión HTTP | Qué versión usa el servidor | `HTTP/1.1` |
| Código de estado | Un número que resume el resultado | `201` |
| Texto de estado | Una descripción legible del código | `Created` |

### Parte 2: Cabeceras de respuesta

```
Content-Type: application/json
Location: /usuarios/98
```

El servidor también usa cabeceras para dar contexto sobre la respuesta:

| Cabecera | Qué informa |
|---|---|
| `Content-Type` | El formato del cuerpo de la respuesta |
| `Content-Length` | Tamaño del cuerpo en bytes |
| `Location` | La URL del recurso recién creado (útil en respuestas `201 Created`) |
| `Cache-Control` | Instrucciones sobre caching |
| `Set-Cookie` | Solicita al cliente que guarde una cookie |

### Parte 3: Cuerpo de la respuesta

```json
{
  "id": 98,
  "nombre": "Ana Torres",
  "email": "ana@ejemplo.com",
  "creadoEn": "2026-03-19T10:30:00Z"
}
```

El cuerpo contiene la **respuesta real**: el recurso solicitado, el mensaje de error, la confirmación de la operación, etc. En las APIs modernas el formato más común es **JSON** (JavaScript Object Notation).

---

## Los métodos HTTP

Los métodos HTTP (también llamados "verbos HTTP") indican **qué tipo de operación** quiere realizar el cliente. Cada método tiene una semántica específica y convenciones sobre si puede tener cuerpo, si es seguro y si es idempotente.

### Los métodos principales

| Método | Operación | ¿Tiene cuerpo? | Uso típico |
|---|---|---|---|
| `GET` | Obtener un recurso | No | Leer datos. No modifica nada. |
| `POST` | Crear un recurso | Sí | Crear un nuevo registro |
| `PUT` | Reemplazar un recurso completo | Sí | Actualizar completamente un recurso existente |
| `PATCH` | Modificar parcialmente un recurso | Sí | Actualizar solo algunos campos |
| `DELETE` | Eliminar un recurso | No (usualmente) | Borrar un registro |

### ¿Qué significa "seguro" e "idempotente"?

Dos propiedades importantes de los métodos HTTP:

**Seguro (safe):** el método no modifica el estado del servidor. Solo *lee*. `GET` es seguro. `POST` no lo es.

**Idempotente (idempotent):** hacer la misma petición una vez o diez veces produce el mismo resultado. `PUT` es idempotente: si envías los mismos datos cinco veces, el recurso queda igual que si lo enviaste una. `POST` no es idempotente: enviarlo cinco veces crea cinco recursos distintos.

| Método | ¿Seguro? | ¿Idempotente? |
|---|---|---|
| `GET` | ✅ Sí | ✅ Sí |
| `POST` | ❌ No | ❌ No |
| `PUT` | ❌ No | ✅ Sí |
| `PATCH` | ❌ No | ❌ No (puede serlo según implementación) |
| `DELETE` | ❌ No | ✅ Sí (eliminar algo que ya no existe sigue siendo el mismo resultado) |

> Estas propiedades no las hace cumplir el protocolo automáticamente. Las cumple o viola **el código que escribes**. Si haces un `GET` que modifica la base de datos, estás violando la semántica del protocolo aunque técnicamente funcione.

### Métodos menos comunes pero útiles

| Método | Uso |
|---|---|
| `HEAD` | Igual que `GET` pero sin cuerpo en la respuesta. Útil para verificar si un recurso existe o revisar sus cabeceras |
| `OPTIONS` | Pregunta qué métodos acepta el servidor para una URL. Lo usan los navegadores en peticiones CORS |

---

## Códigos de estado HTTP

Los códigos de estado son números de tres dígitos que el servidor incluye en cada respuesta para indicar **qué pasó con la petición**. El primer dígito define la categoría.

### Las cinco categorías

| Rango | Categoría | Significado general |
|---|---|---|
| `1xx` | Informativos | La petición fue recibida; el proceso continúa |
| `2xx` | Éxito | La petición fue recibida, entendida y procesada correctamente |
| `3xx` | Redirección | Se necesita una acción adicional para completar la petición |
| `4xx` | Error del cliente | La petición tiene un problema del lado del cliente |
| `5xx` | Error del servidor | El servidor falló al procesar una petición válida |

### Los códigos más importantes para APIs

#### Éxito (2xx)

| Código | Texto | Cuándo usarlo |
|---|---|---|
| `200 OK` | OK | La petición fue exitosa. El cuerpo contiene el resultado. |
| `201 Created` | Created | Se creó un recurso nuevo exitosamente. Incluir cabecera `Location` con la URL del nuevo recurso. |
| `204 No Content` | No Content | La operación fue exitosa pero no hay contenido que devolver (ej: DELETE exitoso). |

#### Error del cliente (4xx)

| Código | Texto | Cuándo usarlo |
|---|---|---|
| `400 Bad Request` | Bad Request | La petición tiene errores de formato o datos inválidos. |
| `401 Unauthorized` | Unauthorized | El cliente no está autenticado. Falta el token o es inválido. |
| `403 Forbidden` | Forbidden | El cliente está autenticado pero no tiene permiso para esta operación. |
| `404 Not Found` | Not Found | El recurso solicitado no existe. |
| `405 Method Not Allowed` | Method Not Allowed | El método HTTP no está permitido para esta URL. |
| `409 Conflict` | Conflict | Hay un conflicto con el estado actual del recurso (ej: email duplicado). |
| `422 Unprocessable Entity` | Unprocessable Entity | Los datos tienen formato correcto pero fallan la validación de negocio. |

#### Error del servidor (5xx)

| Código | Texto | Cuándo usarlo |
|---|---|---|
| `500 Internal Server Error` | Internal Server Error | Error inesperado en el servidor. Nunca debería llegar al cliente en producción. |
| `502 Bad Gateway` | Bad Gateway | El servidor actuó como proxy y recibió una respuesta inválida del servidor de origen. |
| `503 Service Unavailable` | Service Unavailable | El servidor está temporalmente no disponible (mantenimiento o sobrecarga). |

### Cómo leer un código de estado

Un truco simple: si el primer dígito es `2`, algo salió bien. Si es `4`, el problema está en la petición que envió el cliente. Si es `5`, el servidor tiene un problema.

```
200 → éxito, hay contenido
201 → éxito, se creó algo
204 → éxito, no hay contenido

400 → la petición tiene errores
401 → no sé quién eres (falta autenticación)
403 → sé quién eres, pero no tienes permiso
404 → lo que buscas no existe
405 → ese método no está permitido aquí

500 → me rompí por dentro (error del servidor)
```

---

## El flujo completo de una interacción HTTP

Veamos un ejemplo concreto de principio a fin: un cliente que consulta el perfil de un usuario.

**Petición:**
```http
GET /usuarios/42 HTTP/1.1
Host: api.ejemplo.com
Accept: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta exitosa (el usuario existe):**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 42,
  "nombre": "Carlos Martínez",
  "email": "carlos@ejemplo.com"
}
```

**Respuesta si el usuario no existe:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "Usuario no encontrado",
  "codigo": 404
}
```

**Respuesta si el token es inválido:**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
WWW-Authenticate: Bearer realm="api.ejemplo.com"

{
  "error": "Token inválido o expirado"
}
```

Esta interacción, con sus variantes, es el patrón que repetirás cientos de veces al construir y consumir APIs.

---

## Herramientas para inspeccionar HTTP

Para trabajar con APIs necesitas herramientas que te permitan construir y enviar peticiones HTTP arbitrarias (no solo `GET` desde el navegador):

| Herramienta | Tipo | Cuándo usarla |
|---|---|---|
| **Postman** | GUI de escritorio | Probar APIs visualmente. La más usada en equipos de desarrollo. |
| **Insomnia** | GUI de escritorio | Alternativa a Postman, más liviana. |
| **curl** | Línea de comandos | Probar APIs desde la terminal. Disponible en cualquier sistema. |
| **DevTools del navegador** | Integrado en el navegador | Ver las peticiones que hace una página web (pestaña "Network") |

> **Recomendación:** instala Postman ahora. Lo usarás desde la lección 03 en adelante para probar todos los endpoints que construyas.

Ejemplo de una petición con `curl`:
```bash
curl -X GET "http://localhost:8080/greetings" \
     -H "Accept: text/plain"
```

Este comando hace exactamente lo mismo que escribir `http://localhost:8080/greetings` en el navegador, pero desde la terminal y con control total sobre las cabeceras.






<!-- START OF FILE: docs_lessons_01-web-and-http_04_checklist_rubrica_minima.md -->
# Documento: docs lessons 01-web-and-http 04 checklist rubrica minima
---
# Lección 01 - Lista de verificación: ¿llegué al mínimo requerido?

Usa esta lista para revisar tu comprensión antes de avanzar a la lección siguiente. Esta lección es teórica: el criterio de evaluación no es código que funcione, sino conceptos que puedas explicar con tus propias palabras.

---

## ¿Qué significa "entender" en esta lección?

Memorizar definiciones no es suficiente. Entender un concepto significa que puedes:

1. **Explicarlo** sin leer este documento
2. **Aplicarlo** a un ejemplo nuevo (no el mismo del tutorial)
3. **Relacionarlo** con otros conceptos de la lección

Cada ítem de la lista tiene una pregunta de verificación. Si no puedes responderla sin mirar, vuelve a leer la sección correspondiente.

---

## IE 1.0.1 - Distinguir Internet de la Web

Checklist:

- [ ] Puedo explicar qué es Internet en una oración
- [ ] Puedo explicar qué es la Web en una oración
- [ ] Puedo dar un ejemplo de un servicio de Internet que NO sea la Web

**Pregunta de verificación:** Un amigo te dice "la web y el internet son lo mismo". ¿Qué le respondes y qué ejemplo le darías para ilustrar la diferencia?

---

## IE 1.0.2 - Modelo cliente-servidor

Checklist:

- [ ] Puedo explicar qué es un cliente y qué es un servidor
- [ ] Puedo describir el flujo petición-respuesta
- [ ] Entiendo que el cliente siempre inicia la comunicación
- [ ] Entiendo que un mismo programa puede actuar como cliente y como servidor

**Pregunta de verificación:** Cuando usas la aplicación de Instagram en tu teléfono, ¿quién es el cliente y quién es el servidor? ¿Qué pasa si ese servidor necesita pedirle datos a otro servidor?

---

## IE 1.0.3 - DNS y URL

Checklist:

- [ ] Puedo explicar para qué sirve el DNS con una analogía
- [ ] Puedo identificar las partes de una URL: protocolo, host, puerto, ruta, query string
- [ ] Sé qué significa `localhost` y por qué lo usamos en desarrollo
- [ ] Sé por qué el puerto por defecto para HTTP es 80 y para el desarrollo con Spring Boot es 8080

**Pregunta de verificación:** Analiza esta URL: `http://localhost:8080/api/productos?categoria=electronica&pagina=2`. Identifica cada parte y explica qué representa.

---

## IE 1.0.4 - Protocolo HTTP y sus características

Checklist:

- [ ] Puedo explicar qué es HTTP y para qué existe
- [ ] Entiendo qué significa que HTTP es "sin estado" (stateless)
- [ ] Puedo explicar la implicación práctica del stateless en el desarrollo de APIs
- [ ] Conozco las diferencias principales entre HTTP/1.1 y HTTP/2

**Pregunta de verificación:** ¿Por qué si cierras el navegador y lo vuelves a abrir, la mayoría de los sitios te piden que inicies sesión de nuevo? ¿Qué característica de HTTP explica este comportamiento?

---

## IE 1.0.5 - Anatomía del Request y Response

Checklist:

- [ ] Puedo nombrar las tres partes de una petición HTTP: línea de inicio, cabeceras, cuerpo
- [ ] Puedo nombrar las tres partes de una respuesta HTTP: línea de estado, cabeceras, cuerpo
- [ ] Sé qué hace la cabecera `Content-Type` y por qué es importante
- [ ] Entiendo cuándo una petición tiene cuerpo y cuándo no

**Pregunta de verificación:** Escribe (de memoria, sin copiar) el esqueleto de una petición HTTP que crea un usuario. ¿Qué método usarías? ¿Qué cabeceras incluirías? ¿Qué iría en el cuerpo?

---

## IE 1.0.6 - Métodos HTTP

Checklist:

- [ ] Puedo asociar cada método HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) con su operación
- [ ] Entiendo qué significa que un método es "seguro" y qué implica
- [ ] Entiendo qué significa que un método es "idempotente" y qué implica
- [ ] Puedo decir cuál método usar dado un requerimiento específico

**Pregunta de verificación:** Un sistema de inventario necesita las siguientes operaciones. ¿Qué método HTTP usarías para cada una?
- Consultar todos los productos disponibles
- Registrar un producto nuevo
- Actualizar el precio de un producto existente
- Marcar un producto como descontinuado (cambiar solo el campo `estado`)
- Eliminar un producto del catálogo

---

## IE 1.0.7 - Códigos de estado HTTP

Checklist:

- [ ] Puedo explicar qué significa cada categoría (2xx, 4xx, 5xx)
- [ ] Puedo distinguir cuándo usar `200`, `201` y `204`
- [ ] Puedo distinguir la diferencia entre `401` y `403`
- [ ] Sé por qué un `500` nunca debería ver el usuario en producción
- [ ] Puedo interpretar un código de estado que no conocía previamente, solo por su primer dígito

**Pregunta de verificación:** Recibes estas respuestas al probar una API. ¿Qué problema indica cada una y dónde buscarías la causa?
- `404 Not Found`
- `401 Unauthorized`
- `500 Internal Server Error`
- `405 Method Not Allowed`






<!-- START OF FILE: docs_lessons_01-web-and-http_05_actividad_individual.md -->
# Documento: docs lessons 01-web-and-http 05 actividad individual
---
# Lección 01 - Actividad individual: investigación y reflexión

Esta actividad no tiene código. Tiene preguntas que requieren que investigues, pienses y escribas tus propias conclusiones. El objetivo es que construyas criterio propio, no que copies definiciones de Wikipedia.

> **Formato de entrega:** un documento Markdown (`.md`) con tus respuestas. Cada sección debe tener título y respuesta redactada en tus propias palabras. El largo mínimo por respuesta es el necesario para que se entienda tu razonamiento; no hay máximo.

---

## Parte 1: Actividades investigativas

Estas actividades requieren que busques información y la sintetices con tus propias palabras.

---

### 🔍 Investigación 1.1 — Inspecciona una petición HTTP real

**Objetivo:** ver HTTP en acción con tus propios ojos, no solo en ejemplos del curso.

**Instrucciones:**

1. Abre el navegador (Chrome o Firefox)
2. Ve a cualquier sitio web que uses regularmente (ej: `reddit.com`, `github.com`, `emol.com`)
3. Abre las DevTools con `F12` o clic derecho → "Inspeccionar"
4. Ve a la pestaña **"Network"** (o "Red" si está en español)
5. Recarga la página con `F5`
6. Haz clic en cualquiera de las peticiones que aparecen en la lista

**Responde en tu documento:**

a) ¿Qué URL visitaste? ¿Cuántas peticiones HTTP se generaron al cargar la página?

b) Selecciona UNA de las peticiones y copia su método HTTP, URL completa y código de respuesta. ¿Qué crees que hace esa petición específica?

c) Mira las cabeceras de respuesta (`Response Headers`). Lista al menos tres cabeceras que encuentres y explica, en tus propias palabras, qué podrían significar (aunque no las hayas visto antes, usa el nombre como pista).

d) ¿Hay peticiones con código `3xx`? ¿A qué URL redirigen? ¿Por qué crees que existen esas redirecciones?

---

### 🔍 Investigación 1.2 — HTTP vs HTTPS

**Objetivo:** entender por qué HTTPS importa y qué problema resuelve.

**Instrucciones:** investiga en fuentes confiables (documentación oficial, MDN Web Docs, artículos técnicos). No uses resúmenes de IA como fuente única; contrasta al menos dos fuentes.

**Responde en tu documento:**

a) ¿Qué diferencia hay entre HTTP y HTTPS a nivel técnico? ¿Qué agrega HTTPS que HTTP no tiene?

b) ¿Qué es TLS? ¿Cuál es su relación con HTTPS?

c) Si alguien interceptara el tráfico de red de un sitio HTTP, ¿qué podría ver? ¿Y si fuera HTTPS?

d) ¿Por qué los navegadores modernos marcan los sitios HTTP como "No seguros"? ¿Qué riesgo concreto existe al usar HTTP para enviar, por ejemplo, una contraseña en un formulario de login?

e) Para el desarrollo local (`localhost`), ¿por qué generalmente se usa HTTP sin problema? ¿Qué hace diferente a `localhost` de un dominio en internet?

---

### 🔍 Investigación 1.3 — Evolución de HTTP

**Objetivo:** entender por qué HTTP sigue evolucionando y qué problemas concretos resuelve cada versión.

**Instrucciones:** investiga las diferencias entre HTTP/1.1, HTTP/2 y HTTP/3.

**Responde en tu documento:**

a) ¿Qué problema concreto de HTTP/1.1 resolvió HTTP/2? ¿Qué es la "multiplexación" y por qué es importante?

b) HTTP/3 usa un protocolo de transporte diferente a HTTP/2. ¿Cuál es y por qué se eligió?

c) ¿Cómo puedes saber qué versión de HTTP usa una página web que visitas? (Pista: las DevTools del navegador te lo dicen.) Inspecciona un sitio grande como `google.com` o `cloudflare.com` y reporta qué versión encontraste.

d) ¿Afecta la versión de HTTP al código que escribirías en Spring Boot? ¿Por qué?

---

## Parte 2: Actividades reflexivas

Estas actividades no tienen una respuesta correcta única. Requieren que analices, compares y defiendas tu posición con argumentos.

---

### 💭 Reflexión 1.4 — Stateless: ¿ventaja o limitación?

HTTP es sin estado (stateless): el servidor no recuerda peticiones anteriores.

**Responde en tu documento:**

a) Piensa en tres aplicaciones web que usas frecuentemente. Para cada una, identifica una funcionalidad que claramente requiere "recordar" al usuario entre peticiones (ej: el carrito de compras de un e-commerce). ¿Cómo crees que esas aplicaciones logran ese "recuerdo" si HTTP es stateless?

b) Imagina que HTTP fuera stateful (con estado): cada conexión se mantendría abierta y el servidor recordaría todo sobre cada usuario. ¿Qué ventajas tendría? ¿Qué problemas crearía si el servidor tiene miles de usuarios simultáneos?

c) ¿Por qué crees que los diseñadores de HTTP eligieron el modelo stateless? ¿Fue una buena decisión?

---

### 💭 Reflexión 1.5 — Semántica de los métodos HTTP

El protocolo HTTP define la semántica de los métodos, pero no la hace cumplir técnicamente. Un servidor podría recibir un `DELETE` y crear un registro, y HTTP no lo impediría.

**Responde en tu documento:**

a) ¿Por qué es importante respetar la semántica de los métodos HTTP aunque el protocolo no lo exija? Piensa en qué pasaría si un equipo de desarrollo decide usar siempre `POST` para todo.

b) Algunos desarrolladores argumentan que `PATCH` es innecesario y que `PUT` podría reemplazarlo siempre. ¿Estás de acuerdo? ¿En qué caso concreto `PATCH` es claramente mejor que `PUT`?

c) Busca en internet algún ejemplo real de una API pública (Twitter/X, GitHub, Spotify, etc.) que use métodos HTTP de forma incorrecta o no convencional. ¿Encontraste alguno? ¿Cómo lo justifican?

---

### 💭 Reflexión 1.6 — Códigos de estado y experiencia de usuario

Los códigos de estado HTTP están pensados para la comunicación entre máquinas. Pero el usuario final también los ve a veces.

**Responde en tu documento:**

a) Cuando un usuario ve un `500 Internal Server Error` en su pantalla, ¿qué significa eso desde la perspectiva técnica? ¿Y desde la perspectiva del usuario? ¿Qué debería mostrar una aplicación bien diseñada en lugar del código crudo?

b) ¿Cuál es la diferencia entre un `404` real (el recurso genuinamente no existe) y un `404` de privacidad (el servidor sabe que el recurso existe pero no quiere revelarlo)? ¿En qué situaciones tiene sentido usar el segundo?

c) Hay un debate en la comunidad sobre si las APIs deberían devolver siempre `200 OK` con información de error en el cuerpo, o si deben usar los códigos de estado correctamente. ¿Cuál enfoque prefieres y por qué?

---

## Criterios de evaluación de la actividad

| Criterio | Descripción |
|---|---|
| **Comprensión** | Las respuestas demuestran entendimiento del concepto, no solo copia de definiciones |
| **Evidencia** | Las investigaciones citan fuentes o evidencia observada (capturas, URLs, etc.) |
| **Argumento** | Las reflexiones presentan una posición clara con al menos un argumento de respaldo |
| **Redacción** | Las respuestas están escritas con claridad y en español correcto |
| **Formato** | El documento está en Markdown, con títulos y estructura legible |

---

## Recursos sugeridos para investigar

- [MDN Web Docs - HTTP](https://developer.mozilla.org/es/docs/Web/HTTP) — La referencia más completa y confiable de HTTP en español
- [HTTP Status Codes](https://httpstatuses.com/) — Referencia rápida de todos los códigos de estado
- [How HTTPS works](https://howhttpsworks.com/) — Explicación visual e interactiva de HTTPS
- [HTTP/3 explained](https://http3-explained.haxx.se/en/) — El libro gratuito sobre HTTP/3 del creador de curl
- [DevTools Network panel](https://developer.chrome.com/docs/devtools/network/) — Guía oficial de Chrome DevTools para el panel Network




