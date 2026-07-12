**Evaluación Parcial 2**

**TarroBuild**

*Plataforma online para asistencia de armado de computadores y analisis de compatibilidad de hardware*

## Contenido

- [1. Identificación del Problema](#1-identificación-del-problema)
- [2. Requerimientos del Sistema](#2-requerimientos-del-sistema)
- [3. Solución Propuesta](#3-solución-propuesta)
- [4. Historias de Usuario](#4-historias-de-usuario)
- [5. Definicion de Microservicios](#5-definicion-de-microservicios)

# **1\. Identificación del Problema**

## **1.1 Contexto del dominio**

En Chile, el interés por el armado de computadores ha experimentado un crecimiento sostenido, impulsado por la masificación del trabajo remoto, el gaming y la demanda de equipos de alto rendimiento a un menor costo. Sin embargo, para el usuario promedio, configurar y comprar hardware por piezas sigue siendo complicado: se deben visitar múltiples sitios, comparar precios manualmente y verificar si los componentes elegidos son compatibles entre sí.

Actualmente, un usuario que quiere armar o actualizar su PC se enfrenta a que la información técnica de compatibilidad (socket de CPU, tipo de RAM, formato de placa madre, TDP y fuente de poder requerida) está repartida por foros, fichas, documentos en inglés y videos de YouTube. Por otro lado, las tiendas online locales no ofrecen herramientas que validen automáticamente la selección de componentes, dejando al usuario expuesto a compras incorrectas que generan costos de devolución, tiempos de espera y frustración.

La ausencia de una plataforma inteligente y centralizada obliga a los usuarios a realizar múltiples verificaciones manuales, generando incompatibilidades, sobrecostos y decisiones de compra deficientes.

## **1.2 Declaración del problema principal**

*Los usuarios que desean armar o actualizar un computador no disponen de un sistema centralizado que permita validar automáticamente la compatibilidad técnica entre componentes, estimar requerimientos energéticos, comparar precios de referencia y recibir recomendaciones de mejora antes de tomar una decisión de compra.*

## **1.3 Actores / Usuarios involucrados**

| Actor | Descripción y necesidades |
| --- | --- |
| **Usuario registrado** | Puede crear configuraciones personalizadas, guardar builds, recibir análisis completos, comparar precios referenciales y gestionar alertas/notificaciones. |
| **Usuario no registrado** | Puede explorar el catálogo de componentes, consultar especificaciones técnicas y ejecutar verificaciones básicas de compatibilidad. |
| **Administrador** | Gestiona el catálogo de componentes, atributos técnicos, reglas de compatibilidad, referencias de precio y monitorea el funcionamiento general del sistema. |
| **Sistema** | Ejecuta validaciones automáticas, cálculos energéticos, generación de recomendaciones y envío de alertas configuradas. |

# **2\. Requerimientos del Sistema**

## **2.1 Requerimientos Funcionales**

| ID | Descripción | Actor |
| ----- | ----- | ----- |
| **RF-01** | Registrar un nuevo usuario con nombre, correo electrónico y contraseña encriptada (bcrypt). | Usuario no registrado |
| **RF-02** | Autenticar un usuario mediante correo y contraseña, retornando un token JWT válido. | Usuario no registrado / Administrador |
| **RF-03** | Listar todos los componentes del catálogo con categoría y especificaciones resumidas. | Usuario (registrado o no registrado) |
| **RF-04** | Obtener el detalle técnico completo de un componente. | Usuario (registrado o no registrado) |
| **RF-05** | Filtrar componentes por categoría, marca y rango de precio. | Usuario (registrado o no registrado) |
| **RF-06** | Crear una nueva build personalizada asociada a un usuario autenticado. | Usuario registrado |
| **RF-07** | Agregar, actualizar o eliminar componentes dentro de una build activa. | Usuario registrado |
| **RF-08** | Verificar automáticamente la compatibilidad entre todos los componentes de una build. | Usuario (registrado o no registrado) |
| **RF-09** | Calcular costo total de una build (estimate-service) y validar consumo energético / potencia de fuente de poder (compatibility-service). | Usuario (registrado o no registrado) |
| **RF-10** | Obtener referencias de vendedores registrados para componentes del catálogo. | Usuario (registrado o no registrado) |
| **RF-11** | Generar análisis consolidado de build incluyendo compatibilidad, costo estimado y advertencias. | Usuario registrado |
| **RF-12** | Guardar builds favoritas e historial de configuraciones. | Usuario registrado |
| **RF-13** | Generar recomendaciones de mejora o upgrade según presupuesto o uso objetivo. | Usuario registrado |
| **RF-14** | Crear/editar componentes y atributos técnicos del catálogo. | Administrador |
| **RF-15** | Crear/editar reglas de compatibilidad entre categorías. | Administrador |
| **RF-16** | Registrar/actualizar referencias de precios de mercado. | Administrador |
| **RF-17** | Configurar alertas para cambios de precio o disponibilidad. | Usuario registrado |
| **RF-18** | Enviar notificaciones automáticas al usuario cuando una build presente alertas o cambios relevantes. | Sistema |

## **2.2 Requerimientos No Funcionales**

| ID | Descripción |
| ----- | ----- |
| **RNF-01** | Cada endpoint debe retornar una respuesta en menos de 500 ms bajo condiciones normales de carga. |
| **RNF-02** | Todos los endpoints deben validar los datos de entrada y retornar códigos HTTP semánticos (200, 201, 400, 401, 403, 404, 409, 500) con cuerpo de error estructurado en JSON. |
| **RNF-03** | Cada microservicio debe mantener logs estructurados (nivel INFO/ERROR) con identificador de correlación para trazabilidad entre servicios. |
| **RNF-04** | Cada microservicio debe tener su propia base de datos independiente. Está estrictamente prohibido que dos servicios compartan tablas o esquemas directamente. |
| **RNF-05** | La comunicación entre microservicios debe realizarse exclusivamente vía API REST usando RestClient o Feign Client. No se permite acceso directo a bases de datos ajenas. |
| **RNF-06** | Las contraseñas de usuarios deben almacenarse encriptadas con BCrypt. Ningún endpoint debe retornar contraseñas en texto plano bajo ninguna circunstancia. |

# **3\. Solución Propuesta**

## **3.1 Descripción general**

TarroBuild es una plataforma web basada en arquitectura de microservicios que permite a los usuarios explorar componentes de hardware, crear configuraciones de PC personalizadas y someterlas a procesos automáticos de validación técnica.

A través de un API Gateway centralizado, el sistema expone servicios especializados que colaboran entre sí para entregar una evaluación integral de cada build: compatibilidad entre piezas, estimación de consumo energético, consulta de precios referenciales, sugerencias de mejora y almacenamiento histórico de configuraciones.

El sistema no gestiona directamente procesos de venta física ni despacho de productos. En su lugar, centraliza información técnica de componentes, disponibilidad consultada desde proveedores, validación automática de compatibilidad y generación de cotizaciones persistentes.

La principal característica diferenciadora es que la plataforma no se limita a mostrar componentes, sino que interpreta técnicamente la combinación seleccionada por el usuario y genera un informe consolidado que reduce errores de compatibilidad y mejora la toma de decisiones antes de una compra real en el mercado.

Cada microservicio opera con su propia base de datos independiente y se comunica mediante RestClient o Feign Client bajo un modelo REST desacoplado.

## **3.2 Justificación del uso de microservicios**

TarroBuild utiliza una arquitectura de microservicios para separar los distintos contextos del dominio de configuración y validación de hardware en módulos independientes. Esto permite aislar responsabilidades como catálogo, compatibilidad y recomendaciones, evitando un diseño monolítico con alto acoplamiento.

Cada servicio puede evolucionar de forma autónoma según sus reglas de negocio y nivel de cambio, lo que facilita el mantenimiento y la escalabilidad del sistema. Por ejemplo, la lógica de compatibilidad puede ajustarse sin afectar el resto del sistema.

Además, la separación por servicios permite escalar de manera independiente aquellos módulos con mayor carga de consultas, manteniendo eficiencia en el uso de recursos.

## **3.3 Diagrama conceptual de la arquitectura**

## Arquitectura de Microservicios — TarroBuild

La plataforma se organiza en servicios especializados conectados mediante un API Gateway central.

```
CLIENTE / FRONTEND
        |
        v
API GATEWAY :8080
        |
------------------------------------------------------------
|        |        |        |        |        |        |
AUTH   USER   PRODUCT  CATEGORY  COMPAT.  PROVIDER  BUILD
8081   8082    8083     8084      8085     8086      8087
------------------------------------------------------------
                         |
                         v
                 ESTIMATE :8088
                         |
                         v
             HARDWARE ADVISOR :8089
                         |
                         v
             NOTIFICATION :8090
```

### Características de la arquitectura

1. Cada microservicio cuenta con base de datos independiente (H2 / MySQL).
2. La comunicación entre servicios se realiza mediante RestClient o Feign Client.
3. El API Gateway actúa como único punto de entrada del sistema.
4. La arquitectura prioriza la separación de responsabilidades y la escalabilidad.

### Flujos de comunicación inter-servicio clave

Estas relaciones muestran qué servicio consulta a cuál para resolver cada caso de uso.

- build-service → product-service: obtiene información técnica de componentes para construir la configuración

- build-service → compatibility-service: valida compatibilidad entre componentes seleccionados en la build

- build-service → provider-service: consulta disponibilidad o referencia de componentes externos

- estimate-service → build-service: obtiene la configuración validada para generar la cotización

- estimate-service → product-service: obtiene precios y atributos técnicos actualizados

- hardware-advisor-service → build-service: analiza la configuración actual del usuario

- hardware-advisor-service → product-service: obtiene catálogo para generar sugerencias

- estimate-service → notification-service: envía notificación al usuario al generar una cotización

- hardware-advisor-service → notification-service: notifica recomendaciones relevantes generadas al usuario


## 4. Historias de Usuario

### HU-01 — Registro de usuario

Como usuario no registrado, quiero crear una cuenta en el sistema, para poder guardar y gestionar mis configuraciones de hardware.

**Criterios de aceptación:**
- Dado que ingreso nombre, correo válido y contraseña válida, cuando envío el formulario, entonces el sistema crea la cuenta y retorna HTTP 201.
- Si el correo ya existe, el sistema retorna HTTP 409 Conflict.
- La contraseña debe almacenarse encriptada con BCrypt.

---

### HU-02 — Autenticación de usuario

Como usuario registrado, quiero iniciar sesión en el sistema, para acceder a mis builds y recomendaciones.

**Criterios de aceptación:**
- Dado credenciales correctas, el sistema retorna HTTP 200 con un JWT válido.
- Si las credenciales son incorrectas, retorna HTTP 401 Unauthorized.
- El JWT debe incluir el rol del usuario.

---

### HU-03 — Exploración de catálogo

Como usuario, quiero explorar el catálogo de componentes, para conocer las piezas disponibles para mis builds.

**Criterios de aceptación:**
- GET /api/products retorna lista de productos con nombre, precio y especificaciones técnicas.
- Se puede filtrar por categoría (CPU, GPU, RAM, etc.).
- Productos inactivos no deben aparecer en la respuesta.

---

### HU-04 — Creación de build

Como usuario, quiero crear una configuración de hardware seleccionando componentes, para diseñar mi PC personalizada.

**Criterios de aceptación:**
- El sistema permite agregar múltiples productos a una build.
- Cada build queda asociada al usuario autenticado.
- Si un producto no existe, retorna HTTP 404.

---

### HU-05 — Validación de compatibilidad

Como usuario, quiero verificar si los componentes de mi build son compatibles, para evitar errores de configuración.

**Criterios de aceptación:**
- El sistema valida compatibilidad entre componentes seleccionados.
- Retorna HTTP 200 con resultado compatible o incompatible.
- Si existe incompatibilidad, se entrega detalle técnico del conflicto.

---

### HU-06 — Generación de estimación

Como usuario, quiero obtener una estimación del costo total de mi build, para conocer el valor aproximado de mi configuración.

**Criterios de aceptación:**
- El sistema calcula el precio total basado en los componentes seleccionados.
- Retorna HTTP 200 con el monto total calculado.
- Si la build no existe, retorna HTTP 404.

---

### HU-07 — Recomendaciones de componentes

Como usuario, quiero recibir recomendaciones de mejoras para mi build, para optimizar rendimiento o compatibilidad.

**Criterios de aceptación:**
- El sistema analiza la build actual del usuario.
- Sugiere componentes compatibles o de mejor rendimiento.
- Retorna lista de recomendaciones con justificación técnica.

---

### HU-08 — Consulta de builds

Como usuario, quiero ver mis builds guardadas, para gestionar mis configuraciones anteriores.

**Criterios de aceptación:**
- GET /api/builds retorna lista de builds (ADMIN: todas, USER: solo las del usuario autenticado).
- Cada build incluye sus componentes y estado.
- Si no existen builds, retorna arreglo vacío.

---

### HU-09 — Notificaciones del sistema

Como usuario, quiero recibir notificaciones cuando se generen estimaciones o recomendaciones, para estar informado del estado de mis builds.

**Criterios de aceptación:**
- El sistema envía notificaciones al usuario cuando se genera una estimación.
- También notifica recomendaciones nuevas.
- Las notificaciones quedan registradas en el sistema.

---

### HU-10 — Gestión de catálogo (Admin)

Como administrador, quiero gestionar el catálogo de componentes, para mantener actualizada la información del sistema.

**Criterios de aceptación:**
- Permite crear, actualizar y desactivar productos.
- Solo usuarios con rol ADMIN pueden acceder.
- Cambios se reflejan inmediatamente en builds y estimaciones.


# **5\. Definicion de Microservicios**

A continuacion se define cada uno de los 11 microservicios del sistema TarroBuild, con su responsabilidad, entidades JPA, endpoints REST, dependencias y base de datos propia.

---

| MS-01 · api-gateway (puerto :8080) | |
| :---- | :---- |
| **Responsabilidad** | Actúa como único punto de entrada del sistema. Enruta solicitudes HTTP hacia los microservicios internos y centraliza la validación de seguridad mediante JWT. |
| **Entidades JPA** | No maneja entidades JPA propias. Configuración de rutas en application.yml. |
| **Endpoints REST** | Expone únicamente endpoints proxy hacia los servicios internos. GET /actuator/health para monitoreo del estado del sistema. |
| **Comunica con** | Consulta auth-service para validar tokens JWT. |
| **Base de datos** | No posee base de datos. |

---

| MS-02 · auth-service (puerto :8081) | |
| :---- | :---- |
| **Responsabilidad** | Gestiona autenticación de usuarios mediante registro, login, cierre de sesión y emisión de JWT. |
| **Entidades JPA** | Credential { id, email, passwordHash, role, userId, createdAt } |
| **Endpoints REST** | POST /api/auth/register, POST /api/auth/login, POST /api/auth/logout, GET /api/auth/validate |
| **Comunica con** | Es consultado por api-gateway para validar tokens JWT. Consulta user-service para asociación de perfil de usuario. |
| **Base de datos** | db_auth |

---

| MS-03 · user-service (puerto :8082) | |
| :---- | :---- |
| **Responsabilidad** | Gestiona información de usuario y perfiles asociados. |
| **Entidades JPA** | User { id, name, lastName, email, phone, createdAt } |
| **Endpoints REST** | CRUD de usuarios (/api/users) |
| **Comunica con** | Es consultado por notification-service y auth-service para obtener datos del usuario. |
| **Base de datos** | db_users |

---

| MS-04 · category-service (puerto :8084) | |
| :---- | :---- |
| **Responsabilidad** | Administra categorías de productos y sus atributos técnicos asociados. |
| **Entidades JPA** | Category { id, name, slug, description, isActive } / AttributeDefinition { id, categoryId, attributeName, valueType, isRequired } |
| **Endpoints REST** | CRUD de categorías y atributos técnicos. |
| **Comunica con** | Es consultado por product-service para validar que una categoría existe. |
| **Base de datos** | db_categories |

---

| MS-05 · product-service (puerto :8083) | |
| :---- | :---- |
| **Responsabilidad** | Gestiona el catálogo de componentes de hardware y sus especificaciones técnicas. |
| **Entidades JPA** | Product { id, name, description, price, categoryId, brand, model, isActive } / ProductAttribute { id, productId, attributeName, attributeValue } |
| **Endpoints REST** | CRUD de productos y consulta de atributos técnicos. |
| **Comunica con** | Consulta category-service para validar categorías. Es consultado por build-service, compatibility-service, hardware-advisor y estimate-service para obtener datos de productos. |
| **Base de datos** | db_products |

---

| MS-06 · compatibility-service (puerto :8085) | |
| :---- | :---- |
| **Responsabilidad** | Valida compatibilidad técnica entre componentes de hardware basándose en reglas definidas. |
| **Entidades JPA** | CompatibilityRule { id, sourceCategory, sourceAttributeName, operator, targetCategory, targetAttributeName, incompatibilityReason } / CompatibilityCheck { id, buildId, productIds, result, details, createdAt } |
| **Endpoints REST** | POST /api/compatibility/check, CRUD de reglas de compatibilidad. |
| **Comunica con** | Consulta product-service para obtener atributos técnicos. |
| **Base de datos** | db_compatibility |

---

| MS-07 · provider-service (puerto :8086) | |
| :---- | :---- |
| **Responsabilidad** | Gestiona proveedores externos o referencias de mercado de componentes. |
| **Entidades JPA** | Provider { id, name, contact, website, isActive } / ProviderProduct { id, providerId, productId, externalReference } |
| **Endpoints REST** | CRUD de proveedores y consulta de referencias externas. |
| **Comunica con** | Es consultado por build-service para entregar referencias externas de componentes. |
| **Base de datos** | db_providers |

---

| MS-08 · build-service (puerto :8087) | |
| :---- | :---- |
| **Responsabilidad** | Núcleo del sistema. Gestiona configuraciones de hardware creadas por el usuario (builds). |
| **Entidades JPA** | Build { id, userId, name, status, createdAt } / BuildItem { id, buildId, productId, quantity } |
| **Endpoints REST** | CRUD de builds y gestión de componentes dentro de una configuración. |
| **Comunica con** | Consulta product-service, compatibility-service y provider-service para construir y validar builds. |
| **Base de datos** | db_builds |

---

| MS-09 · estimate-service (puerto :8088) | |
| :---- | :---- |
| **Responsabilidad** | Calcula el costo total de una configuración de hardware basada en los componentes seleccionados. |
| **Entidades JPA** | Estimate { id, buildId, totalPrice, currency, createdAt } |
| **Endpoints REST** | POST /api/estimate/calculate, GET /api/estimate/{buildId} |
| **Comunica con** | Consulta build-service y product-service para calcular el costo total. Notifica a notification-service al generar una estimación. |
| **Base de datos** | db_estimates |

---

| MS-10 · hardware-advisor-service (puerto :8089) | |
| :---- | :---- |
| **Responsabilidad** | Genera recomendaciones de componentes compatibles o mejoras para una configuración existente. |
| **Entidades JPA** | Recommendation { id, buildId, ruleApplied, suggestedProductId, reason, createdAt } |
| **Endpoints REST** | GET /api/recommendations/{buildId}, POST /api/recommendations/generate |
| **Comunica con** | Consulta build-service, product-service y compatibility-service para generar recomendaciones. Notifica a notification-service al generar sugerencias relevantes. |
| **Base de datos** | db_advisor |

---

| MS-11 · notification-service (puerto :8090) | |
| :---- | :---- |
| **Responsabilidad** | Envía notificaciones al usuario sobre eventos del sistema (estimaciones, recomendaciones, cambios en builds). |
| **Entidades JPA** | NotificationLog { id, userId, type, content, status, timestamp } |
| **Endpoints REST** | POST /api/notifications/send, GET /api/notifications/logs |
| **Comunica con** | Es consultado por estimate-service y hardware-advisor-service. |
| **Base de datos** | db_notifications |

## 6. Información del equipo

| Integrante | Rol |
|------------|-----|
| [Nombre 1] | [Rol] |
| [Nombre 2] | [Rol] |
| [Nombre 3] | [Rol] |

## 7. Puertos y bases de datos

| Servicio | Puerto | Base de datos |
|----------|--------|---------------|
| api-gateway | 8080 | — |
| auth-service | 8081 | db_auth |
| user-service | 8082 | db_users |
| product-service | 8083 | db_products |
| category-service | 8084 | db_categories |
| compatibility-service | 8085 | db_compatibility |
| provider-service | 8086 | db_providers |
| build-service | 8087 | db_builds |
| estimate-service | 8088 | db_estimates |
| hardware-advisor-service | 8089 | db_recommendations |
| notification-service | 8090 | db_notifications |

## 8. Variables de entorno

Copiar `.env.example` a `.env` y configurar según el entorno:

```bash
cp .env.example .env
```

Variables principales:
- `SPRING_PROFILES_ACTIVE`: h2 (desarrollo), mysql (Docker), render (producción)
- `{SERVICE}_URL`: URLs de comunicación entre servicios (local: `http://localhost:PUERTO`)
- `JWT_SECRET`: secreto para firmar tokens JWT
- `DATABASE_URL`: solo para perfil render (PostgreSQL)

## 9. Ejecución local

### Requisitos previos
- Java 21 (Eclipse Temurin)
- Maven 3.9+ (incluye `mvnw.cmd`)
- Docker Desktop (opcional, para compose)

### Windows (PowerShell)

```powershell
# Compilar todo el proyecto
cd C:\ruta\tarrobuild
mvnw.cmd clean compile -DskipTests

# Ejecutar un servicio (perfil h2 por defecto)
cd services\api-gateway
mvnw.cmd spring-boot:run

# Ejecutar pruebas de un servicio específico
cd services\auth-service
mvnw.cmd test

# Ejecutar todas las pruebas del proyecto
cd ..\..
mvnw.cmd test

# Docker Compose (base de datos MySQL + todos los servicios)
docker compose up --build
```

### Linux/macOS

```bash
# Compilar
./mvnw clean compile -DskipTests

# Ejecutar servicio
cd services/api-gateway
./mvnw spring-boot:run

# Pruebas
./mvnw test
```

### Orden de arranque (local sin Docker)
1. Los servicios no tienen dependencias externas en perfil h2 (BD embebida)
2. Iniciar en cualquier orden; el gateway funciona independientemente
3. Para flujo completo, auth-service y user-service deben estar activos

### Orden de arranque (Docker Compose)
```bash
docker compose up --build
```
Levanta automáticamente: 10 MySQL → 11 servicios Java con healthchecks.

### Verificar funcionamiento
```powershell
# Login como admin
curl.exe -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{\"email\":\"admin@tarrobuild.cl\",\"password\":\"admin123\"}'

# Listar productos (público)
curl.exe http://localhost:8080/api/v1/products
```

## 10. Usuarios de prueba

| Rol | Email | Contraseña |
|-----|-------|------------|
| Administrador | admin@tarrobuild.cl | admin123 |
| Usuario registrado | user@test.com | test123 |

## 11. Swagger UI

Cada servicio expone Swagger UI en:
```
http://localhost:{puerto}/swagger-ui/index.html
```

Ejemplos:
- Gateway: http://localhost:8080/swagger-ui/index.html
- auth-service: http://localhost:8081/swagger-ui/index.html
- product-service: http://localhost:8083/swagger-ui/index.html

## 12. Despliegue en Render

Los 11 servicios están desplegados en Render como servicios web independientes:

| Servicio | URL |
|----------|-----|
| api-gateway | https://api-gateway-tzkw.onrender.com |
| auth-service | https://auth-service-oww8.onrender.com |
| user-service | https://user-service-1ycx.onrender.com |
| product-service | https://product-service-e903.onrender.com |
| category-service | https://category-service-91sc.onrender.com |
| compatibility-service | https://compatibility-service-3hfz.onrender.com |
| provider-service | https://provider-service-56hc.onrender.com |
| build-service | https://build-service-lym5.onrender.com |
| estimate-service | https://estimate-service-h0h9.onrender.com |
| hardware-advisor-service | https://hardware-advisor-service-9b8i.onrender.com |
| notification-service | https://notification-service-d4ww.onrender.com |

## 13. Enlace a tablero de gestión

[Agregar enlace a Trello / GitHub Projects / Jira]

## 14. Documentación adicional

| Archivo | Descripción |
|---------|-------------|
| `docs/matriz-requerimientos.md` | Matriz de requerimientos con trazabilidad |
| `docs/plan-cierre-feedback.md` | Feedback recibido y acciones correctivas |
| `docs/documentacion-funcional.md` | Documentación funcional del sistema |
| `docs/documentacion-tecnica.md` | Documentación técnica y ejecución desde cero |
| `docs/levantamiento-requerimientos-actualizado.md` | Levantamiento actualizado de requerimientos |
| `docs/ARCHITECTURE.md` | Arquitectura detallada del sistema |
| `docs/PROGRESS.md` | Estado de avance por servicio |
| `docs/DONE.md` | Historial de trabajo completado |
| `docs/API-DOCS.md` | Documentación de APIs Swagger/OpenAPI |
| `docs/TEST-STRATEGY.md` | Estrategia y cobertura de pruebas |
| `docs/presentacion-defensa-grupal.md` | Presentación de defensa técnica grupal |
| `docs/defensa-individual/` | Documentos de defensa individual por integrante |

## 5.1 Resumen de comunicación inter-servicio

| Servicio origen | Servicio destino | Motivo de la llamada |
| ----- | ----- | ----- |
| **build-service** | **product-service** | Obtener información técnica de componentes para construir configuraciones de hardware |
| **build-service** | **compatibility-service** | Validar compatibilidad entre componentes seleccionados en la build |
| **build-service** | **provider-service** | Consultar disponibilidad o referencias externas de componentes |
| **estimate-service** | **build-service** | Obtener configuración completa para calcular el costo total |
| **estimate-service** | **product-service** | Obtener precios actualizados de los componentes |
| **estimate-service** | **notification-service** | Solicitar notificación al usuario cuando se genera una estimación de build |
| **hardware-advisor-service** | **build-service** | Analizar la configuración actual del usuario para generar sugerencias |
| **hardware-advisor-service** | **product-service** | Obtener catálogo de componentes para generar recomendaciones |
| **hardware-advisor-service** | **compatibility-service** | Validar compatibilidad de recomendaciones generadas |
| **hardware-advisor-service** | **notification-service** | Solicitar notificación de recomendaciones de mejora o compatibilidad al usuario |
| **auth-service** | **user-service** | Asociar credenciales con perfil de usuario durante autenticación y registro |
| **api-gateway** | **auth-service** | Validar tokens JWT en cada solicitud entrante |
```