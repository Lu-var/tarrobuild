# TarroBuild — Shared Project Context

## Documentation map

| File                               | Purpose                                                                                                                                                                                                           |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `README.md`                        | Project domain, actors, RF/RNF/HU, 11 service definitions, inter-service communication map                                                                                                                        |
| `docs/ARCHITECTURE.md`             | Stack, service table, database (Flyway, per-profile), entity definitions, security flow, roles (USER/ADMIN), conventions (DTOs, controllers, services, repos, exceptions), inter-service patterns, config pattern |
| `docs/PROGRESS.md`                 | RF/RNF/HU checklists, per-service layered checklists with course module annotations, current state of each service                                                                                                |
| `docs/guides/lessons`              | Full course lesson material (lessons 00–18)                                                                                                                                                                       |
| `docs/guides/syllabus-extras.md`   | Supplementary material (Java, Git, Docker, SOLID, etc.)                                                                                                                                                           |
| `docs/guides/syllabus-projects.md` | Ticket project READMEs (Tickets-10 through Tickets-18)                                                                                                                                                            |

## Layered structure (all services)

```
cl.tarrobuild.<service>/
  dto/        — Java records (Request with @Valid, Response plain)
  controller/ — @RestController, constructor injection
  service/    — @Service @Slf4j, constructor injection
  repository/ — extends JpaRepository<Entity, Long>
  model/      — @Entity, Lombok, JPA annotations
  exception/  — ApiError record + @ControllerAdvice handler
```

## Conventions

### Entities

- `@Entity`, `@Table`, `@Id` + `@GeneratedValue(GenerationType.IDENTITY)`
- `@Getter` `@Setter` `@NoArgsConstructor` from Lombok
- Cross-service references as plain `Long` IDs — never `@ManyToOne` across services
- Within-service references use `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn`
- `@OneToMany(mappedBy = ...)` with `cascade = ALL` where appropriate
- Default enum values in `@PrePersist`, never as field initializers (bypassed by `@AllArgsConstructor`)

### DTOs

- **Request** = Java record with `jakarta.validation.constraints.*` (`@NotBlank`, `@Size`, `@NotNull`)
- **Response** = Java record, no validation, includes nested child responses where applicable
- **Update** = separate record only when fields differ from create (e.g. omits email)

### Controllers

- Constructor injection only (no `@Autowired`)
- `@Valid @RequestBody` on POST/PUT/PATCH
- `@PathVariable Long id` on single-resource endpoints
- Sub-resource routes: `/api/parents/{parentId}/children`
- PATCH for partial updates (fields may differ from create/update DTO)
- Returns `ResponseEntity<T>` with explicit status code

### Services

- `@Slf4j` for logging
- `log.info()` on create, update, delete
- Throws: `EntityNotFoundException`, `IllegalArgumentException`, `EntityExistsException`
- `Optional.map().orElseThrow()` for find-by-id
- `toResponse(Entity)` method for entity to DTO mapping
- Delete methods return `boolean` (true = deleted, false = not found)

### Repositories

- `extends JpaRepository<Entity, Long>`
- Derived query methods (e.g. `findByCategoryId`, `existsByEmail`, `findByIdAndBuild_Id`)

### Exception handling

- `@ControllerAdvice` with `GlobalExceptionHandler`
- Development shows stack trace, production shows message only
- Exception → HTTP mapping:

| Exception                         | HTTP code |
|-----------------------------------|-----------|
| `IllegalArgumentException`        | 400       |
| `MethodArgumentNotValidException` | 400       |
| `EntityNotFoundException`         | 404       |
| `NoResourceFoundException`        | 404       |
| `EntityExistsException`           | 409       |
| `BadCredentialsException`         | 401       |
| `AccessDeniedException`           | 403       |
| `Exception` (generic)             | 500       |

### Inter-service communication

| Tool            | When                         |
|-----------------|------------------------------|
| **RestClient**  | Single-endpoint calls        |
| **FeignClient** | Multi-endpoint orchestration |

- Fallbacks for fault tolerance
- Timeouts: 5s connect, 5s read
- Fire-and-forget for notifications
- URL via `@Value` or `@FeignClient(url = "${...}")` with environment variable fallbacks

### Configuration

- 3 YAMLs per service: `application.yaml` (default h2), `application-h2.yaml`, `application-mysql.yaml`
- `.env` with `spring-dotenv` for environment variables
- Profiles switched via `SPRING_PROFILES_ACTIVE` (defaults to h2)

### Database

- Each service has its own independent database
- Per ARCHITECTURE.md, Flyway is for MySQL (production) only. H2 (dev) uses `data.sql` + JPA `ddl-auto: create-drop`
- MySQL migration directories:
    - `db/migration/mysql/`: `V1__init.sql` + `V2__seed_data.sql` (MySQL syntax)
- See PROGRESS.md for current migration status per service

## Security

### Auth flow

```
POST /api/auth/register  → BCrypt hash → Credential created → 201
POST /api/auth/login     → email + password validated → JWT returned → 200
GET  /api/auth/validate  → JWT validated → user info returned → 200
```

### Roles

| Role    | Description                                                                                      |
|---------|--------------------------------------------------------------------------------------------------|
| `USER`  | Registered user. Own builds, estimates, recommendations, notifications.                          |
| `ADMIN` | Full system management. CRUD products, categories, compatibility rules, providers, product MSRP. |

### Access control

| Scope  | Endpoints                                                                                           | Required role |
|--------|-----------------------------------------------------------------------------------------------------|---------------|
| Public | `GET /api/products`, `GET /api/categories`, `POST /api/compatibility/check`                         | None          |
| User   | `POST/PUT/DELETE /api/builds`, `POST /api/estimate/calculate`, `POST /api/recommendations/generate` | USER or ADMIN |
| Admin  | `POST/PUT/DELETE /api/products`, `/api/categories`, `/api/compatibility/rules`, `/api/providers`    | ADMIN         |

## Dependency order (service implementation)

```
category-service → provider-service → product-service
→ compatibility-service → build-service → estimate-service
→ hardware-advisor-service → notification-service
→ auth-service → api-gateway
```

## Tech stack

| Component    | Version                 |
|--------------|-------------------------|
| Java         | 21                      |
| Spring Boot  | 4.0.6                   |
| Spring Cloud | 2025.1.1                |
| Build        | Maven multi-module      |
| Auth         | BCrypt + JWT            |
| Database     | H2 (dev) / MySQL (prod) |
