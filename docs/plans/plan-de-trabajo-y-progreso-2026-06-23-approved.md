# Plan de Trabajo y Progreso
---
## [~] Correcciones y Pendientes Existentes

### [✅] Fix merge conflict en compatibility-service
- **Archivo:** `services/compatibility-service/src/main/resources/application.yaml`
- **Qué:** Resolver conflicto HEAD vs 359538b (duplicación de secciones `---` y `spring.config.activate.on-profile` vs `prod`)
- **Resultado:** YAML parseable, perfil `prod` funcional con `product-service.url` bajo `app.services`

### [⚠️] Implementar `evaluateRule()` en compatibility-service
- **Archivo:** `CompatibilityService.java`, `ProductDTO.java`
- **Qué:** Reemplazar stub que siempre retorna compatible con lógica real que compare atributos de producto según la regla (socket, RAM type, form factor, etc.)
- **Depende de:** ProductRestClient ya existe

### [❌] Implementar `generate()` en hardware-advisor-service
- **Qué:** Reemplazar stub que retorna lista vacía
- **Agregar:** FeignClients → build-service, product-service, compatibility-service, notification-service
- **Lógica:** Obtener build → comparar componentes con catálogo → aplicar reglas de compatibilidad → persistir recomendaciones
- **Agregar:** FeignClient fallbacks y timeouts configurables

### [❌] Fix security leak en build-service
- **Endpoint:** `GET /api/builds`
- **Qué:** Debe filtrar builds por usuario autenticado en vez de retornar todos
- **Depende de:** Propagación de user identity desde gateway (userId header)

### [❌] Fix notification-service excepciones
- **Archivo:** `NotificationService.java`, `GlobalExceptionHandler.java`
- **Qué:** Reemplazar `NoSuchElementException` con `EntityNotFoundException`
- **Agregar:** `EntityNotFoundException` handler en `GlobalExceptionHandler`
- **Agregar:** `UserRestClient` → user-service para resolver email en `send()`

### [❌] Provider-service JPA relations
- **Archivos:** `Provider.java`, `ProviderProduct.java`
- **Qué:** Reemplazar `Long providerId` escalar con `@ManyToOne(fetch = FetchType.LAZY) Provider provider` + `@JoinColumn`
- **Agregar:** `@OneToMany(mappedBy = "provider")` en `Provider.java`

### [❌] Agregar PATCH endpoints faltantes
- **Servicios afectados:** user-service, category-service, provider-service
- **Qué:** Endpoint PATCH para actualización parcial de recursos

### [❌] Agregar `@Column(unique = true)` en User.email
- **Archivo:** `User.java` en user-service
- **Validar:** Unicidad de email en registro (cross-check con auth-service)

### [❌] Propagación de Correlation ID y user identity
- **Qué:** Gateway debe reenviar `X-Correlation-Id`, `X-User-Id`, `X-User-Email`, `X-User-Role` como headers
- **Servicios downstream:** Consumir estos headers para logging y autorización

## [ ] Documentación OpenAPI / Swagger UI
### [❌] Agregar springdoc al root pom.xml
- **Dependencia:** `springdoc-openapi-starter-webmvc-ui` en `<dependencyManagement>`

### [❌] Crear OpenApiConfig en cada servicio
- **Clase:** `config/OpenApiConfig.java` (10 servicios, compatibility-service ya lo tiene)
- **Bean:** `OpenAPI` con `title`, `description`, `version`, `license`
- **Títulos por servicio:** TarroBuild - Auth API, TarroBuild - User API, etc.

### [❌] Documentar todos los controladores con `@Tag`
- **10 controllers** (compatibility-service ya tiene `@Tag`)
- **Ejemplo:** `@Tag(name = "Usuarios", description = "Operaciones CRUD para usuarios")

### [❌] Documentar todos los endpoints con `@Operation` + `@ApiResponse`
- **~55 endpoints** sin documentar
- Incluir: `summary`, `description`, códigos HTTP (200, 201, 204, 400, 401, 403, 404, 409, 500)
- Usar `@ApiResponses` para múltiples respuestas
- Usar `@Parameter` en path/query params

### [❌] Documentar DTOs con `@Schema`
- **Todos los DTOs Request y Response** (~30+ clases)
- Incluir: `description`, `example` para cada campo
- Documentar `ErrorResponse` con `@Schema(description = "Respuesta uniforme de error")`

### [❌] Verificar Swagger UI
- Cada servicio: `http://localhost:<port>/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:<port>/v3/api-docs`

## [ ] Contenerización con Docker

### [❌] Crear Dockerfile para cada servicio
- **Base image:** `eclipse-temurin:21-jre`
- **Estructura:**
  ```dockerfile
  FROM eclipse-temurin:21-jre
  WORKDIR /app
  COPY target/*.jar app.jar
  RUN addgroup -S appgroup && adduser -S appuser -G appgroup
  USER appuser
  EXPOSE <port>
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
- **Configurar:** Perfil activo vía variable de entorno `SPRING_PROFILES_ACTIVE`

### [❌] Crear compose.yml en raíz
- **Servicios MySQL:** 11 contenedores (uno por microservicio)
  - `db_auth`, `db_users`, `db_products`, `db_categories`, `db_compatibility`, `db_providers`, `db_builds`, `db_estimates`, `db_recommendations`, `db_notifications`
  - Cada uno con su propia DB, user, password, puerto
- **Servicios app:** 11 contenedores Spring Boot
  - Conectados a sus respectivas DBs por nombre de servicio
  - Variables de entorno para URLs de otros servicios
  - `depends_on` para orden de inicio
- **Network:** `tarrobuild-net` (bridge)
- **Volumes:** Named volumes para persistencia de cada DB

### [❌] Jerarquía de inicio
1. Bases de datos (todos los mysql)
2. Servicios sin dependencias (auth, user, category, notification)
3. product-service (depende de category)
4. compatibility-service (depende de product)
5. provider-service, estimate-service
6. build-service (depende de product, compatibility, provider)
7. hardware-advisor-service (depende de build, product, compatibility, notification)
8. api-gateway (depende de todos)

### [❌] Scripts de build y deploy
- Script `build-all.sh` para compilar todos los servicios
- Script `deploy.sh` para `docker compose up -d`
- Verificar con `docker compose ps`

---

## [ ] HATEOAS — Links en respuestas API

### [❌] Agregar dependencia
- **Root pom.xml:** `spring-boot-starter-hateoas` en `<dependencyManagement>`
- **Cada servicio:** Agregar dependencia (al menos 3-4 servicios principales)

### [❌] Crear LinkAssembler por servicio
- **Estrategia:** `EntityModel<T>` wrapper (no modificar DTOs existentes)
- **Clases a crear:**
  - `UserLinkAssembler`
  - `ProductLinkAssembler`
  - `CategoryLinkAssembler`
  - `BuildLinkAssembler`
  - `ProviderLinkAssembler`
  - `NotificationLinkAssembler`
- **Links por recurso:** `self`, `all`, `update`, `delete` (condicional según estado/rol)

### [❌] Actualizar controllers
- **GET por id:** `ResponseEntity<EntityModel<T>>`
- **GET lista:** `ResponseEntity<CollectionModel<EntityModel<T>>>`
- Agregar `linkTo(methodOn(...)).withSelfRel()` en colecciones
- Links condicionales según estado del recurso

### [❌] Documentar HATEOAS en OpenAPI
- Actualizar `@Operation(description = ...)` para mencionar que respuesta incluye `_links`

---

## [ ] Pruebas Unitarias

### [❌] Crear plan de pruebas unitarias
- **Archivo:** `docs/test-plan.md`
- **Contenido:** Objetivo, alcance, fuera de alcance, tabla de casos con ID/GWT/resultado

### [❌] Service Tests por servicio
- **Patrón:** `@ExtendWith(MockitoExtension.class)`, `@Mock` repositorios + clients, `@InjectMocks` service
- **Estructura:** `src/test/java/.../service/<Service>ServiceTest.java`
- **Casos mínimos por servicio (3-5 tests):**
  - Happy path (create, findById, findAll)
  - Error path (entity no encontrado lanza `EntityNotFoundException`)
  - Validación (datos inválidos lanzan excepción)
  - Fallback (microservicio externo caído no rompe la operación)
  - Negocio (reglas específicas del servicio)

### [❌] Controller Tests (opcional, si alcanza)
- **Patrón:** `@WebMvcTest(Controller.class)` + `@MockBean` service
- **Estructura:** `src/test/java/.../controller/<Service>ControllerTest.java`
- Probar códigos HTTP y estructura de respuesta

### [❌] Ejecutar y verificar
- `mvnw.cmd test` en cada servicio
- Todos los tests deben pasar sin requerir base de datos real ni servicios externos
- Ningún test debe usar `Thread.sleep` o `try/catch` vacío

---

## Resumen de Entregables

| Componente | Archivos a crear/modificar | Prioridad |
|------------|---------------------------|-----------|
| Fixes existentes | ~15 archivos (controllers, services, models, configs) | Alta |
| OpenAPI | ~12 archivos (pom.xml, OpenApiConfig × 11, anotaciones en ~55 endpoints, @Schema en ~30 DTOs) | Alta |
| Docker | ~13 archivos (Dockerfile × 11, compose.yml, build script) | Media |
| HATEOAS | ~18 archivos (pom.xml, LinkAssembler × 6-7, controllers actualizados × 6-7) | Media |
| Unit Tests | ~25 archivos (plan, ServiceTest × 11, ControllerTest × 11) | Alta |

---

## Dependencias entre fases

```
Correcciones ──┬──> OpenAPI ──> HATEOAS
                 │
                ├──> Docker [independiente]
                 │
                └──> Pruebas Unitarias [independiente, pero código debe estar estable}
```
