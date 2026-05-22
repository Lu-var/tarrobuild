# TarroBuild — Architecture

## Overview

Plataforma online para asistencia de armado de computadores y análisis de compatibilidad de hardware. Usuarios exploran componentes, crean configuraciones de PC y reciben validación técnica automática (compatibilidad, consumo energético, precios referenciales, recomendaciones).

## Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Build | Maven multi-module |
| Auth | BCrypt + JWT |
| Inter-service | RestClient / FeignClient |

## Architecture diagram

```
CLIENTE
   |
API GATEWAY :8080
   |
------------------------------------------------------------
|       |       |       |       |       |       |          |
AUTH   USER   PRODUCT CATEGORY COMPAT PROVIDER  BUILD
8081   8082    8083    8084    8085   8086     8087
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
| MS-08 | build-service | 8087 | User build configurations (core service). | Build, BuildItem | db_builds |
| MS-09 | estimate-service | 8088 | Cost calculation for a build. | Estimate | db_estimates |
| MS-10 | hardware-advisor-service | 8089 | Upgrade/suggestion recommendations. | Recommendation | db_advisor |
| MS-11 | notification-service | 8090 | Send and log system notifications. | NotificationLog | db_notifications |

## Database

Each service has its own independent database — strictly no shared tables.

Flyway manages schema changes for MySQL (production). H2 (development) uses JPA `ddl-auto: create-drop` + `data.sql` for seed data. Hibernate uses `ddl-auto: validate` in production and `ddl-auto: create-drop` in development.

### Migration structure

```
services/<service>/src/main/resources/
  db/migration/mysql/
    V1__init.sql       — CREATE TABLE with MySQL syntax
    V2__seed_data.sql  — INSERT reference data
  application.yaml            — default profile (h2)
  application-h2.yaml         — H2 (JPA auto-DDL + data.sql)
  application-mysql.yaml      — MySQL + Flyway
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

Switch profiles via `SPRING_PROFILES_ACTIVE=mysql` (defaults to `h2`). Development uses `data.sql` for seed data. Production uses Flyway `V2__seed_data.sql`.

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
Provider { Long id, String name, String contact, String website, Boolean isActive }
ProviderProduct { Long id, Long providerId, Long productId, String externalReference }

// build-service
Build { Long id, Long userId, String name, BuildStatus status, LocalDateTime createdAt,
        @OneToMany List<BuildItem> items }
BuildItem { Long id, Long productId, Integer quantity, @ManyToOne Build build }

// estimate-service
Estimate { Long id, Long buildId, Integer totalPrice, String currency, LocalDateTime createdAt }

// hardware-advisor
Recommendation { Long id, Long buildId, String ruleApplied, Long suggestedProductId,
                 String reason, LocalDateTime createdAt }

// notification-service
NotificationLog { Long id, Long userId, String type, String content, NotificationStatus status (INFO, WARNING, SUCCESS, ERROR),
                  LocalDateTime timestamp }
```

## Inter-service communication

| Origin | → | Destination | Reason | Method |
|--------|---|-------------|--------|--------|
| api-gateway | → | auth-service | Validate token | RestClient |
| auth-service | → | user-service | Link user profile | RestClient |
| compatibility-service | → | product-service | Get product specs | RestClient |
| build-service | → | product-service | Get product info | Feign |
| build-service | → | compatibility-service | Check compatibility | Feign |
| build-service | → | provider-service | Get provider references | Feign |
| estimate-service | → | build-service | Get build | RestClient |
| estimate-service | → | product-service | Get msrp | RestClient |
| estimate-service | → | notification-service | Send notification | RestClient |
| product-service | → | category-service | Validate category | RestClient |
| hardware-advisor-service | → | build-service | Get build | Feign |
| hardware-advisor-service | → | product-service | Get products | Feign |
| hardware-advisor-service | → | compatibility-service | Check compatibility | Feign |
| hardware-advisor-service | → | notification-service | Send notification | Feign |

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

### Roles

| Role | Description |
|------|-------------|
| `USER` | Registered user. Manages own builds, items, estimates, recommendations, notifications. |
| `ADMIN` | Full system management. CRUD products, categories, attributes, compatibility rules, provider references. |

### Access control

| Scope | Endpoints | Required role |
|-------|-----------|---------------|
| **Public** | `GET /api/products`, `GET /api/categories`, `POST /api/compatibility/check` | None |
| **User** | `POST/PUT/DELETE /api/builds`, `POST /api/estimate/calculate`, `POST /api/recommendations/generate` | USER or ADMIN |
| **Admin** | `POST/PUT/DELETE /api/products`, `/api/categories`, `/api/compatibility/rules`, `/api/providers` | ADMIN |

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

### Exception handling

```java
public record ApiError(String message, String details, String timestamp) {}
```

`GlobalExceptionHandler` with `@ControllerAdvice`. Environment-aware: development shows stack trace, production shows message only.

| Exception | HTTP code |
|-----------|-----------|
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` | 400 |
| `EntityNotFoundException` | 404 |
| `NoResourceFoundException` | 404 |
| `EntityExistsException` | 409 |
| `BadCredentialsException` | 401 |
| `AccessDeniedException` | 403 |
| `Exception` (generic) | 500 |

### Inter-service communication patterns

| Tool | When to use |
|------|-------------|
| **RestClient** | Single-endpoint calls (e.g. product-service → category-service to validate one category) |
| **FeignClient** | Multi-endpoint orchestration (e.g. hardware-advisor → 4 services, multiple calls per request) |

- Fallbacks for fault tolerance
- Configurable timeouts: 5s connect, 5s read
- Fire-and-forget for notifications
- URL config via `@Value` or `@FeignClient(url = "${...}")` with environment variable fallbacks

### Config pattern

Each service has:

- `application.yaml` — name, port, default profile (h2)
- `application-h2.yaml` — H2 (`ddl-auto: create-drop` + `data.sql`)
- `application-mysql.yaml` — MySQL + Flyway (`db/migration/mysql/`), `ddl-auto: validate`
- `V1__init.sql` — schema creation (in `db/migration/{profile}/`)
- `V2__seed_data.sql` — reference data (in `db/migration/{profile}/`)
- `pom.xml` — parent: `tarrobuild/pom.xml`, deps: webmvc, data-jpa, validation, h2, mysql-connector-j, flyway-core, flyway-mysql, lombok
