# Documento de Defensa Técnica Individual

## Datos del estudiante

- **Nombre:** Lucas Vargas
- **Rol dentro del equipo:** Developer
- **Asignatura:** DSY1103 — Desarrollo Fullstack I Backend
- **Proyecto:** TarroBuild

## Funcionalidades o módulos en los que participó

Scope global del proyecto: 11 microservicios, gateway, discovery-server, Docker Compose y Render.

### Funcionalidades y modulos principales:

- Arquitectura de microservicios (refactor a multi-módulo, Eureka service discovery)
- Gateway con JWT, rutas directas y roles
- Build-service (CRUD, Feign clients, roles ADMIN/USER, notificaciones, favoritos, historial)
- Hardware-advisor-service (recomendaciones de componentes faltantes)
- Compatibility-service (reglas, evaluateRule, buildId opcional)
- Auth-service (JWT, BCrypt, Flyway, @PreAuthorize)
- Correlation ID en los 11 servicios
- Springdoc/OpenAPI en los 11 servicios
- 212 tests unitarios en los 11 servicios
- Postman collections (10 colecciones, estructura Direct/Gateway/Monolith)
- Docker Compose con healthchecks y perfiles
- Despliegue en Render (blueprint + monolith)
- Documentación técnica completa (ARCHITECTURE, PROGRESS, DONE, matriz, levantamiento)

## Commits propios más relevantes

| Hash      | Descripción                                                    | Archivos                             |
| --------- | -------------------------------------------------------------- | ------------------------------------ |
| `35c1f1b` | feat: initialize core TarroBuild microservice modules          | Raíz del proyecto                    |
| `6aca2a6` | feat: implement auth-service with JWT auth, BCrypt, Flyway     | auth-service/                        |
| `2fab8fe` | feat: implement build-service with CRUD, Feign clients, Flyway | build-service/                       |
| `9b7ea42` | feat(compatibility): implementar evaluateRule()                | compatibility-service/               |
| `3399f0d` | feat: lógica de roles ADMIN/USER en build-service              | BuildController, BuildService        |
| `9219879` | feat: discovery-server Eureka + registro de servicios          | discovery-server/                    |
| `e0ae026` | fix: gateway usa URLs directas, lb:// solo entre servicios     | GatewayRoutesConfig                  |
| `11c68e9` | test: pruebas unitarias en 10 servicios + contextLoads         | tests/ en 10 servicios               |
| `f550631` | fix: unit tests api-gateway (50 tests)                         | api-gateway tests/                   |
| `b1eb29d` | feat: Postman collections con soporte Render + tokens JWT      | postman/collections/                 |
| `5522ca1` | feat: builds favoritas e historial (RF-12)                     | FavoriteService, BuildHistoryService |
| `ec4dfae` | feat: notificaciones automáticas al crear build (RF-18)        | BuildService                         |
| `e99c49b` | feat: recomienda componentes para categorías faltantes (RF-13) | HardwareAdvisorService               |
| `3f99403` | fix: buildId opcional en compatibility check                   | CompatibilityCheckRequest            |
| `8677025` | add: render deploy prep                                        | render.yaml, Dockerfiles             |
| `109cf22` | docs: documentos obligatorios EFT                              | docs/                                |

## Tareas del tablero asociadas a su trabajo

No se utilizó Trello ni Github Issues. El progreso se registró en `docs/PROGRESS.md` y las tareas completadas en `docs/DONE.md`. La organización fue con gitflow vía GitHub: ramas por feature (`feat/roles-build-service`, `fix/critical-bugs`, `feat/tests`, etc.) y pull requests para integrar a `develop`.

## Feedback o pendiente que corrigió personalmente

**Feedback de evaluaciones anteriores:**

- Auth-service lanzaba ResponseStatusException incorrecta → cambiado a `IllegalArgumentException` con `@ExceptionHandler`
- Estimate-service usaba CLP como moneda por defecto → cambiado a USD
- Gateway tenía rutas públicas duplicadas → creada clase compartida `PublicPaths.java`
- Notification-service tenía `@ExceptionHandler` duplicado → eliminado
- ProviderFeignClient era código muerto → eliminado
- Sin filtro de correlación en servicios downstream → agregado en 10 servicios
- Sin @PreAuthorize en auth-service → agregado
- Build-service sin trigger de compatibilidad → integrado `CompatibilityFeignClient`
- Sin application-prod.yaml → creado en 11 servicios
- Pruebas unitarias solo en 1/11 servicios → 212 tests en 11 servicios
- Gateway enrutaba solo 2/10 con lb:// sin Eureka → discovery-server agregado, rutas directas
- GET /api/builds sin filtro por usuario → roles ADMIN/USER implementados
- Feign clients sin usar → ProviderFeignClient eliminado, CompatibilityFeignClient integrado

## Archivos principales que modificó

- `services/build-service/` — BuildService, BuildController, FavoriteService, BuildHistoryService, NotificationFeignClient, tests
- `services/compatibility-service/` — controller, DTOs, CompatibilityCheckRequest, migraciones, tests
- `services/hardware-advisor-service/` — HardwareAdvisorService.generate(), tests
- `services/api-gateway/` — GatewayRoutesConfig, JwtAuthFilter, SecurityConfig, PublicPaths
- `services/auth-service/` — AuthController, SecurityConfig
- `services/discovery-server/` — módulo completo (pom.xml, Application, application.yaml)
- `services/estimate-service/` — application-render.yaml, tests
- `services/notification-service/` — application-render.yaml, tests
- `compose.yml`, `render.yaml`, `.gitattributes`
- `postman/collections/` — 10 colecciones
- `Dockerfile.render`
- `docs/` — ARCHITECTURE, PROGRESS, DONE, matriz, levantamiento, plan-cierre

## Endpoints o flujos asociados a su aporte

| Método  | Ruta                            | Descripción                                      |
| ------- | ------------------------------- | ------------------------------------------------ |
| GET     | `/api/builds`                   | Listar builds (ADMIN: todas, USER: propias)      |
| GET     | `/api/builds/user/{userId}`     | Builds por usuario (protegido por rol)           |
| POST    | `/api/builds`                   | Crear build (fire-and-forget notification)       |
| PATCH   | `/api/builds/{id}/status`       | Cambiar estado (notifica + guarda historial)     |
| POST    | `/api/builds/{id}/favorite`     | Toggle favorito                                  |
| GET     | `/api/builds/favorites`         | Listar favoritos                                 |
| GET     | `/api/builds/{id}/history`      | Historial de cambios                             |
| POST    | `/api/recommendations/generate` | Recomendar componentes para categorías faltantes |
| POST    | `/api/compatibility/check`      | Validar compatibilidad entre productos           |
| POST    | `/api/auth/login`               | Login JWT                                        |
| POST    | `/api/auth/register`            | Registro de usuario                              |
| Gateway | `/api/v1/**`                    | Proxy a los 11 servicios                         |

## Pruebas unitarias o REST asociadas a su aporte

- `BuildServiceTest` — 27 tests (CRUD builds, items, notificaciones, favoritos, historial, roles)
- `HardwareAdvisorServiceTest` — 8 tests (recomendaciones, compatibilidad, build sin items)
- `CompatibilityServiceTest` — 12 tests (reglas, compatibilidad, casos borde)
- `SecurityConfigTest` — 15 tests (rutas públicas, protegidas, admin)
- `JwtAuthFilterTest` — 20 tests (token válido, inválido, expirado, ausente)
- `CorrelationIdFilterTest` — 6 tests
- `GlobalExceptionHandlerTest` — 8 tests (400/401/403/404/500/503)
- Adicionales: auth-service (11), user-service (18), product-service (20), category-service (12), provider-service (16), estimate-service (11), notification-service (8), contextLoads (11)

Total: 212 tests, 0 failures.

## Regla de negocio que domina

`POST /api/recommendations/generate` recibe un `buildId`. El `HardwareAdvisorService` obtiene la build vía `BuildFeignClient` e identifica qué categorías de hardware (CPU, GPU, RAM, etc.) no tienen productos asignados. Para cada categoría vacía, busca un producto activo en el catálogo vía `ProductFeignClient` y verifica compatibilidad contra los productos existentes llamando a `CompatibilityFeignClient`. Si el producto sugerido es incompatible, se salta. El resultado es una lista de recomendaciones `MISSING_COMPONENT` que se guardan y notifican al usuario.

Ejemplo: build con CPU y GPU pero sin RAM → "Te falta RAM: Corsair Vengeance 32GB (Corsair) - $180".

Archivo: `HardwareAdvisorService.java`. Test: `HardwareAdvisorServiceTest`.

## Relación de base de datos que domina

Relación `@OneToMany` entre Build y BuildItem.

Una build contiene múltiples items. En JPA:

```java
@OneToMany(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true)
private List<BuildItem> items = new ArrayList<>();

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "build_id")
private Build build;
```

- `mappedBy = "build"` — BuildItem es el dueño de la relación
- `cascade = CascadeType.ALL` — las operaciones de Build se propagan a sus items
- `orphanRemoval = true` — items sin build se eliminan
- `FetchType.LAZY` — los items se cargan solo cuando se necesitan

Migraciones Flyway en `V1__init.sql` con FK `fk_build_items_build`.

## Comunicación entre servicios que domina

`AuthService` → `UserRestClient`.

`POST /api/auth/register` inicia el registro. `AuthService` verifica que el email no exista en credentials, hashea la contraseña con BCrypt y llama a `UserRestClient.createUser()` para crear el perfil en user-service:

```java
UserClientRequest newUserRequest = new UserClientRequest(
    request.name(), request.lastName(), request.email(), request.phone());
UserClientResponse userResponse = userRestClient.createUser(newUserRequest);
```

`UserRestClient` hace `POST lb://user-service/api/users` vía RestClient. Si el email ya existe, `onStatus(409)` lanza `EntityExistsException` → `GlobalExceptionHandler` responde HTTP 409. Si user-service no responde, `ResourceAccessException` → HTTP 503.

`UserClientRequest` y `RegisterRequest` son DTOs separados, cada servicio tiene su propio contrato.

Archivos: `UserRestClient.java`, `AuthService.java`, `UserClientRequest.java`. Test: `AuthServiceTest`.

## Dificultad técnica personal y cómo la resolvió

Eureka service discovery con Render free tier. Los contenedores de Render están aislados (IP interna única por servicio) y `lb://` no resuelve URLs entre ellos.

La solución fue mantener `lb://` para Docker Compose local y usar URLs directas desde variables de entorno para el gateway en Render. El gateway inyecta `X-User-Id` y `X-User-Role` a los servicios downstream, que confían en esos headers porque el gateway ya autenticó al usuario. Esta decisión está documentada en `ARCHITECTURE.md` como limitación de infraestructura.

La segunda dificultad fue Springdoc 2.x incompatible con Spring Boot 4.x (`NoSuchMethodError` en `ControllerAdviceBean`). Se resolvió fijando la versión 2.8.6 en el `dependencyManagement` del root POM y agregando `@Hidden` en los `GlobalExceptionHandler`.

## Checklist personal de evidencia entregada

- [x] Commits con participación verificable
- [x] Archivos modificados documentados
- [x] Pruebas asociadas al aporte
- [x] Endpoints asociados al aporte
- [x] Regla de negocio explicada
- [x] Relación de BD explicada
- [x] Comunicación entre servicios explicada
