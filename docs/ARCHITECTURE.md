# TarroBuild — Architecture

## Overview

Plataforma online para asistencia de armado de computadores y análisis de compatibilidad de hardware. Usuarios exploran componentes, crean configuraciones de PC y reciben validación técnica automática (compatibilidad, consumo energético, precios referenciales, recomendaciones).

**Pattern:** Spring Cloud microservices with API Gateway and Eureka service discovery.

**Key Characteristics:**
- Each service owns its own database (database-per-service pattern)
- All external traffic enters through a single API Gateway (`api-gateway`)
- JWT-based authentication validated centrally at the gateway
- Inter-service communication via `lb://` URIs resolved through Eureka (`RestClient` and `FeignClient`)
- Spring profiles (`h2`, `mysql`, `render`, `prod`) control database and migration strategy
- Correlation ID propagated across all services for distributed tracing

## Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Build | Maven multi-module |
| Auth | BCrypt + JWT |
| Inter-service | RestClient / FeignClient |
| Service Discovery | Eureka (Netflix) |

## Architecture diagram

```
                           DISCOVERY SERVER :8761
                               |
                               v
CLIENTE → API GATEWAY :8080
              |
---------------------------------------------------------------
|       |       |       |       |       |       |             |
AUTH   USER   PRODUCT CATEGORY COMPAT PROVIDER  BUILD
8081   8082    8083    8084    8085   8086     8087
---------------------------------------------------------------
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

## Services

| # | Service | Port | Responsibility | Entities | DB |
|---|---------|------|---------------|----------|----|
| MS-01 | api-gateway | 8080 | Single entry point. Route proxying + JWT auth filter. | none | none |
| MS-02 | auth-service | 8081 | Register, login, JWT emission, role management. | Credential | db_auth |
| MS-03 | user-service | 8082 | User profiles CRUD. | User | db_users |
| MS-04 | category-service | 8084 | PC component categories + technical attribute definitions. | Category, AttributeDefinition | db_categories |
| MS-05 | product-service | 8083 | Hardware catalog with specs and attributes. | Product, ProductAttribute | db_products |
| MS-06 | compatibility-service | 8085 | Validate component compatibility against rules. | CompatibilityRule, CompatibilityCheck | db_compatibility |
| MS-07 | provider-service | 8086 | External vendor references and pricing. | Provider, ProviderProduct | db_providers |
| MS-08 | build-service | 8087 | User build configurations (core service). | Build, BuildItem, BuildHistory, Favorite | db_builds |
| MS-09 | estimate-service | 8088 | Cost calculation for a build. | Estimate | db_estimates |
| MS-10 | hardware-advisor-service | 8089 | Upgrade/suggestion recommendations. | Recommendation | db_advisor |
| MS-11 | notification-service | 8090 | Send and log system notifications. | NotificationLog | db_notifications |
| MS-12 | discovery-server | 8761 | Service discovery (Eureka). All services register and resolve via lb://. | none | none |

## Entry points

**API Gateway (port 8080):**
- Location: `services/api-gateway/src/main/java/cl/tarrobuild/apigateway/ApiGatewayApplication.java`
- Triggers: All external HTTP requests
- Responsibilities: Route to correct service, enforce JWT authentication, enforce role-based authorization, inject identity headers, propagate correlation ID

**Discovery Server (port 8761):**
- Location: `services/discovery-server/src/main/java/cl/tarrobuild/discovery/DiscoveryServerApplication.java`
- Triggers: Service startup (all services register)
- Responsibilities: Maintain service registry, enable `lb://` resolution

**Individual Service Entry Points:**
- Each service has a `*Application.java` with `@SpringBootApplication` and `@EnableDiscoveryClient`
- Some services add `@EnableFeignClients` (`build-service`, `hardware-advisor-service`) or `@EnableAsync` (`notification-service`)

## Data flows

### Build creation flow

1. Client sends `POST /api/builds` → `api-gateway` → `build-service`
2. `build-service` adds items via `POST /api/builds/{buildId}/items` → resolves products via `FeignClient` → `product-service`
3. User triggers compatibility check → `build-service` → `compatibility-service` via `FeignClient`
4. User triggers estimate → `estimate-service` → fetches build from `build-service` and prices from `product-service` via `RestClient`
5. Estimate created → notification sent to `notification-service` via `RestClient`

### Authentication flow

1. Client sends `POST /api/auth/login` → `api-gateway` routes to `auth-service`
2. `auth-service` validates credentials against `Credential` entity → generates JWT
3. Subsequent requests include `Authorization: Bearer <token>` → `JwtAuthFilter` validates via `auth-service` `RestClient`
4. Gateway injects `X-User-Id`, `X-User-Email`, `X-User-Role` headers → downstream services read these for authorization

### Compatibility check flow

1. Request to `POST /api/compatibility/check` → `compatibility-service` receives `CompatibilityCheckRequest` with optional build ID and product IDs
2. `CompatibilityService` fetches product attributes from `product-service` and category specs from `category-service` via `RestClient`
3. Evaluates stored `CompatibilityRule` entries against fetched attributes
4. Persists `CompatibilityCheck` result and returns report

### Hardware recommendation flow

1. Request to `POST /api/recommendations/generate` → `hardware-advisor-service` fetches build data from `build-service`, product catalog from `product-service`, and existing compatibility checks from `compatibility-service` — all via `FeignClient`
2. `HardwareAdvisorService` identifies missing component slots in the build and recommends compatible products
3. Applies recommendation logic → generates `Recommendation` entities
4. Notification sent via `notification-service` `FeignClient`

## Database

Each service has its own independent database — strictly no shared tables.

Flyway manages schema changes for MySQL (production) and PostgreSQL (Render). H2 (development) uses JPA `ddl-auto: create-drop` + `data.sql` for seed data. Hibernate uses `ddl-auto: validate` in production and `ddl-auto: create-drop` in development.

### Migration structure

```
services/<service>/src/main/resources/
  db/migration/mysql/
    V1__init.sql       — CREATE TABLE with MySQL syntax
    V2__seed_data.sql  — INSERT reference data
  db/migration/postgresql/
    V1__init.sql       — CREATE TABLE with PostgreSQL syntax
    V2__seed_data.sql  — INSERT reference data
  application.yaml            — default profile (h2)
  application-h2.yaml         — H2 (JPA auto-DDL + data.sql)
  application-mysql.yaml      — MySQL + Flyway
  application-render.yaml     — PostgreSQL + Flyway (Render)
```

### Production — MySQL

```yaml
spring:
  flyway:
    locations: classpath:db/migration/mysql
  jpa:
    hibernate:
      ddl-auto: validate
```

### Docker compose (prod)

Local deployment via `docker compose up` uses the `prod` Spring profile (`SPRING_PROFILES_ACTIVE=prod`). Each service has an `application-prod.yaml` with MySQL datasource + Flyway pointing to `classpath:db/migration/mysql`. All services register with Eureka at `http://discovery-server:8761/eureka/`. See `compose.yml` at project root for full configuration (12 services, 10 MySQL databases, healthchecks, port mappings).

### Render — PostgreSQL

```yaml
spring:
  flyway:
    locations: classpath:db/migration/postgresql
  jpa:
    hibernate:
      ddl-auto: validate
```

### Development — H2

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
```

Switch profiles via `SPRING_PROFILES_ACTIVE=mysql` or `SPRING_PROFILES_ACTIVE=render` (defaults to `h2`). Development uses `data.sql` for seed data. Production/Render use Flyway `V2__seed_data.sql`.

## Entity definitions

```
// auth-service
Credential { Long id, String email, String passwordHash, String role, Long userId, LocalDateTime createdAt }

// user-service
User { Long id, String name, String lastName, String email, String phone, LocalDateTime createdAt }

// category-service
Category { Long id, String name, String slug, String description, Boolean isActive,
           @OneToMany List<AttributeDefinition> attributes }
AttributeDefinition { Long id, String attributeName, AttributeValueType valueType (STRING, NUMBER, BOOLEAN), Boolean isRequired,
                      @ManyToOne Category category }

// product-service
Product { Long id, String name, String description, Integer msrp, Long categoryId,
          String brand, String model, Boolean isActive,
          @OneToMany List<ProductAttribute> attributes }
ProductAttribute { Long id, String attributeName, String attributeValue,
                   @ManyToOne Product product }

// compatibility-service
CompatibilityRule { Long id, String sourceCategory, String sourceAttributeName,
                    String operator, String targetCategory, String targetAttributeName,
                    String incompatibilityReason }
CompatibilityCheck { Long id, Long buildId, String productIds, Boolean result, String details, LocalDateTime createdAt }

// provider-service
Provider { Long id, String name, String contact, String website, Boolean isActive,
           @OneToMany List<ProviderProduct> products }
ProviderProduct { Long id, @ManyToOne Provider provider, Long productId, String externalReference }

// build-service
Build { Long id, Long userId, String name, BuildStatus status, LocalDateTime createdAt,
        @OneToMany List<BuildItem> items }
BuildItem { Long id, Long productId, Integer quantity, @ManyToOne Build build }
BuildHistory { Long id, Long buildId, BuildStatus oldStatus, BuildStatus newStatus, LocalDateTime changedAt }
Favorite { Long id, Long userId, Long buildId, LocalDateTime createdAt }

// estimate-service
Estimate { Long id, Long buildId, Integer totalCost, String currency, LocalDateTime createdAt }

// hardware-advisor
Recommendation { Long id, Long buildId, String ruleApplied, Long suggestedProductId,
                 String reason, LocalDateTime createdAt }

// notification-service
NotificationLog { Long id, Long userId, String type, String content, NotificationStatus status (INFO, WARNING, SUCCESS, ERROR),
                  LocalDateTime timestamp }
```

## Inter-service communication

All calls use `lb://SERVICE-NAME` resolved through Eureka service discovery. The discovery-server runs on port 8761 and every service registers with its `spring.application.name`.

| Origin | → | Destination | Reason | Method |
|--------|---|-------------|--------|--------|
| api-gateway | → | auth-service | Validate token | RestClient |
| auth-service | → | user-service | Link user profile | RestClient |
| compatibility-service | → | product-service | Get product specs | RestClient |
| build-service | → | product-service | Get product info | Feign |
| build-service | → | compatibility-service | Check compatibility | Feign |
| build-service | → | notification-service | Send notification | Feign |
| estimate-service | → | build-service | Get build | RestClient |
| estimate-service | → | product-service | Get msrp | RestClient |
| estimate-service | → | notification-service | Send notification | RestClient |
| product-service | → | category-service | Validate category | RestClient |
| hardware-advisor-service | → | build-service | Get build | Feign |
| hardware-advisor-service | → | product-service | Get products | Feign |
| hardware-advisor-service | → | compatibility-service | Check compatibility | Feign |
| hardware-advisor-service | → | notification-service | Send notification | Feign |
| compatibility-service | → | category-service | Get category specs | RestClient |
| notification-service | → | user-service | Get user info | RestClient |

## Actors

| Actor | Role | Access |
|-------|------|--------|
| Usuario no registrado | — (public) | Catalog exploration, product detail, filtering, compatibility checks |
| Usuario registrado | USER | Own builds, estimates, recommendations, notifications, alerts |
| Administrador | ADMIN | CRUD products, categories, attributes, compatibility rules, provider references |
| Sistema | — | Internal automated operations (validations, calculations, notifications) |

## Security

### Auth flow

```
POST /api/auth/register  → BCrypt hash → Credential created → 201
POST /api/auth/login     → email + password validated → JWT returned → 200
GET  /api/auth/validate  → JWT validated → user info returned → 200
```

- Passwords stored as BCrypt hash (never plain text)
- JWT includes user id, email, role
- API Gateway validates JWT on every proxied request
- Stateless sessions (`SessionCreationPolicy.STATELESS`), CSRF disabled
- Identity propagated via `X-User-Id`, `X-User-Email`, `X-User-Role` request headers

### Roles

| Role | Description |
|------|-------------|
| `USER` | Registered user. Manages own builds, items, estimates, recommendations, notifications. |
| `ADMIN` | Full system management. CRUD products, categories, attributes, compatibility rules, provider references. |

### Access control

| Scope | Endpoints | Required role |
|-------|-----------|---------------|
| **Public** | `GET /api/products`, `GET /api/categories`, `POST /api/compatibility/check`, auth login/register | None |
| **User** | `POST/PUT/DELETE /api/builds`, `POST /api/estimate/calculate`, `POST /api/recommendations/generate` | USER or ADMIN |
| **Admin** | `POST/PUT/DELETE /api/products`, `/api/categories`, `/api/compatibility/rules`, `/api/providers` | ADMIN |

### Error handling

`GlobalExceptionHandler` with `@ControllerAdvice` in every service. Environment-aware: development shows stack trace, production shows message only.

**Standard error response:**
```java
public record ApiError(String message, String details, String timestamp) {}
```

| Exception | HTTP Code |
|-----------|-----------|
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` | 400 |
| `HttpMessageNotReadableException` | 400 |
| `NumberFormatException` | 400 |
| `MissingRequestHeaderException` | 400 |
| `MethodArgumentTypeMismatchException` | 400 |
| `EntityNotFoundException` | 404 |
| `NoResourceFoundException` | 404 |
| `EntityExistsException` | 409 |
| `BadCredentialsException` | 401 |
| `AccessDeniedException` | 403 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `ResourceAccessException` | 503 |
| `Exception` (generic) | 500 |

**Gateway-specific:**
- `JwtAuthFilter` returns 401 for missing/invalid tokens, 503 if `auth-service` is unreachable
- `SecurityConfig` defines `authenticationEntryPoint` (401) and `accessDeniedHandler` (403) with JSON responses

## Conventions

### Layered structure

Every service follows this package layout:

```
cl.tarrobuild.<service>/
  dto/        — Java records (Request with @Valid, Response plain)
  controller/ — @RestController, constructor injection
  service/    — @Service @Slf4j, constructor injection
  repository/ — extends JpaRepository<Entity, Long>
  model/      — @Entity, Lombok, JPA annotations
  exception/  — ApiError record + @ControllerAdvice handler
  client/     — RestClient or FeignClient for inter-service calls
  config/     — OpenApiConfig, CorrelationIdFilter, rest/feign config
```

### DTO conventions

- **Request** = Java record with `jakarta.validation.constraints.*`
- **Response** = Java record, no validation, includes nested child responses as `List<ChildResponse>`
- **Update** = separate record only when fields differ from create (e.g. `UserUpdateRequest` omits `email`)

### Controller patterns

- Constructor injection only (no `@Autowired`)
- `@Valid @RequestBody` on POST/PUT/PATCH
- PATCH for partial updates (fields may differ from create/update DTO)
- `@PathVariable Long id` on single-resource endpoints
- Sub-resource routes: `/api/parents/{parentId}/children`
- Returns `ResponseEntity<T>` with explicit status code

### Service patterns

- `@Slf4j` for logging
- Throws: `EntityNotFoundException`, `IllegalArgumentException`, `EntityExistsException`
- `toResponse(Entity)` method for entity to DTO mapping
- `Optional.map().orElseThrow()` for find-by-id

### Repository patterns

- `extends JpaRepository<Entity, Long>`
- Derived query methods (e.g. `findByCategoryId`, `existsByEmail`, `findByIdAndBuild_Id`)

### Inter-service communication patterns

| Tool | When to use |
|------|-------------|
| **RestClient** | Single-endpoint calls (e.g. product-service → category-service to validate one category) |
| **FeignClient** | Multi-endpoint orchestration (e.g. hardware-advisor → 4 services, multiple calls per request) |

- Fallbacks with `*FallbackFactory` classes for fault tolerance
- Configurable timeouts: 30s connect, 90s read (Render); 5s connect, 10s read (local/Docker)
- Fire-and-forget for notifications
- URL resolution via `lb://SERVICE-NAME` through Eureka service discovery
- RestClient.Builder annotated with `@LoadBalanced` for lb:// support
- FeignClient without `url` attribute resolves via Eureka by service name

### Config pattern

Each service has:

- `application.yaml` — name, port, default profile (h2)
- `application-h2.yaml` — H2 (`ddl-auto: create-drop` + `data.sql`), Eureka disabled
- `application-mysql.yaml` — MySQL + Flyway (`db/migration/mysql/`), `ddl-auto: validate`
- `application-render.yaml` — PostgreSQL + Flyway (`db/migration/postgresql/`), `ddl-auto: validate`, Eureka disabled
- `application-prod.yaml` — MySQL + Flyway (`db/migration/mysql/`), `ddl-auto: validate` (Docker Compose)
- `V1__init.sql` — schema creation (in `db/migration/{profile}/`)
- `V2__seed_data.sql` — reference data (in `db/migration/{profile}/`)
- `pom.xml` — parent: `tarrobuild/pom.xml`, deps: webmvc, data-jpa, validation, h2, mysql-connector-j, flyway-core, flyway-mysql, lombok, eureka-client
- `eureka.client.serviceUrl.defaultZone` — `${EUREKA_URL:http://localhost:8761/eureka/}`
- `entrypoint.sh` — shell script parsing `DATABASE_URL` (postgres://) into JDBC url, guarded by conditional

## Cross-cutting concerns

### Logging
- SLF4J + Logback with rolling file appenders per service (`logs/<service-name>.log`)
- Max file size: 10MB, max history: 10 files
- Correlation ID stored in MDC for distributed tracing
- Package-level logging: `cl.tarrobuild: INFO`, `org.hibernate.SQL: WARN`

### Deployment
- Docker multi-stage builds: Maven build stage → Eclipse Temurin JRE runtime
- Non-root container user (`appuser`)
- Docker Compose for local production (MySQL + all services)
- Render for cloud deployment (PostgreSQL + all services)
- `render.yaml` defines all services with `DATABASE_URL` and `EUREKA_URL` environment variables

### Documentation
- OpenAPI / Swagger via `springdoc-openapi-starter-webmvc-ui` at `/swagger-ui/index.html` per service
- Postman collections (311 requests across 10 services in Direct/Gateway/Monolith folders)
