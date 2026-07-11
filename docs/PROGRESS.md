# PROGRESS.md

---

## Functional requirements (RF)

- [x] **RF-01** — Register user with bcrypt
- [x] **RF-02** — Authenticate user (JWT)
- [x] **RF-03** — List catalog components
- [x] **RF-04** — Component detail
- [x] **RF-05** — Filter by category/brand/price
- [x] **RF-06** — Create build
- [x] **RF-07** — Manage build items
- [x] **RF-08** — Compatibility check (invoked by build-service on item create/update/delete)
- [x] **RF-09** — Power consumption estimate (cost via estimate-service + power validation via compatibility rules, triggered on item change)
- [x] **RF-10** — Provider references (SKU / external links)
- [ ] **RF-11** — Consolidated build analysis
- [ ] **RF-12** — Save favorite builds
- [ ] **RF-13** — Upgrade recommendations
- [x] **RF-14** — CRUD components/attributes
- [x] **RF-15** — CRUD compatibility rules
- [x] **RF-16** — CRUD provider references
- [ ] **RF-17** — Price/availability alerts
- [ ] **RF-18** — Auto-notifications

## Non-functional requirements (RNF)

- [ ] **RNF-01** — Response < 500ms
- [x] **RNF-02** — Validation + semantic HTTP codes
- [x] **RNF-03** — Structured logs with correlation ID and downstream propagation
- [x] **RNF-04** — Independent DB per service
- [x] **RNF-05** — REST-only inter-service communication
- [x] **RNF-06** — BCrypt password encryption

## User stories (HU)

- [x] **HU-01** — User registration
- [x] **HU-02** — Authentication
- [x] **HU-03** — Catalog exploration
- [x] **HU-04** — Build creation
- [x] **HU-05** — Compatibility validation
- [x] **HU-06** — Cost estimation
- [ ] **HU-07** — Component recommendations
- [x] **HU-08** — Build history
- [x] **HU-09** — System notifications
- [x] **HU-10** — Admin catalog management

---


## Services

> Current progress per service. Completed items archived in [DONE.md](DONE.md).

---
### Cross-cutting

**Pending**
- [ ] (optional) Split profiles into `dev`/`prod` (environment) + `h2`/`mysql` (database)
- [ ] Fix dependency chain in `.opencode/agents/shared-context.md` — add `user-service` before `category-service` (currently omitted, but `auth-service` depends on it)
- [ ] Fix dependency chain in `.opencode/agents/planner.md` — same missing `user-service` fix
- [ ] Fix `.opencode/agents/implementor.md` — change "entity" to "model" as the package name (actual package is `model/`, not `entity/`)

---
### api-gateway :8080

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] ContextLoads test

**Pending**
- [ ] Unit tests

---

### auth-service :8081
**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (12 tests)
- [x] ContextLoads test

---

### user-service :8082

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (18 tests)
- [x] ContextLoads test

---

### category-service :8084

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (12 tests)
- [x] ContextLoads test

---

### product-service :8083

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (20 tests)
- [x] ContextLoads test

**Pending**
- [ ] FeignClient fallbacks and configurable timeouts (Module 5)
- [ ] Endpoint tests script

---

### compatibility-service :8085

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (12 tests)
- [x] ContextLoads test

**Pending**
- [ ] Endpoint tests script

---

### provider-service :8086

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (16 tests)
- [x] ContextLoads test

**Pending**
- [ ] Cross-service integration — currently isolated, no upstream service queries provider data
- [ ] Endpoint tests script

---

### build-service :8087

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (27 tests)
- [x] ContextLoads test
- [x] Lógica de roles (ADMIN/USER) en endpoints GET — ADMIN ve todas, USER solo las propias; /user/{userId} protegido

**Pending**
- [ ] Endpoint tests script

---

### estimate-service :8088

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (11 tests)
- [x] ContextLoads test

**Pending**
- [ ] Deduplicate keys in `application.yaml` (build/product/notification service URLs + server.port are duplicated)
- [ ] Endpoint tests script (notification as template)

---

### hardware-advisor-service :8089

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (8 tests)
- [x] ContextLoads test

**Pending**
- [ ] Endpoint tests script (notification as template)

---

### notification-service :8090

**Done**
- [x] Swagger/OpenAPI (springdoc)
- [x] Tests unitarios (8 tests)
- [x] ContextLoads test

**Pending**
- [ ] Endpoint tests script (no script file found)
---

---

# Course Modules Requirements

> Each item represents a concept/feature required by the semester project's rubric

---

### Module 1: Foundation (Lessons 1–9)

- CSR: Controller / Service / Repository / Model
- REST endpoints with semantic HTTP codes (200, 201, 204, 400, 404, 409, 500)
- DTOs as Java records (Request with `@Valid`, Response without)
- Local `@ExceptionHandler` per service
- Declarative validation (`@NotBlank`, `@Size`, `@NotNull`)
- `HashMap<Long, Entity>` repository (pre-JPA)

### Module 2: JPA & Persistence (Lessons 10–11)

- JPA entities (`@Entity`, `@Id`, `@GeneratedValue`)
- `JpaRepository<T, Long>` with derived queries
- H2 in-memory database (h2 profile)
- Spring profiles: h2 / mysql with `application-{profile}.yaml`
- `.env` with `spring-dotenv`
- `DataInitializer`, `data.sql` or Flyway `V2__seed_data.sql` for seed data

### Module 3: JPA Relations (Lesson 12)

- `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn`
- `@OneToMany(mappedBy = ...)` with `cascade = ALL`
- PATCH endpoints for partial updates
- User entity with unique email

### Module 5: Inter-service Communication (Lesson 14)

- RestClient
- FeignClient with `@EnableFeignClients`
- Fallbacks for fault tolerance
- Configurable timeouts (connect + read)
- Fire-and-forget for notifications

### Module 6: Flyway Migrations (Lesson 15)

- `flyway-core` + `flyway-mysql` dependencies
- Versioned migrations (`V1__`, `V2__`, etc.)
- Per-profile migration directories (MySQL only)
- `ddl-auto: validate` in production

### Module 7: Spring Security (Lesson 16)

- `spring-boot-starter-security`
- Authentication with STATELESS sessions (JWT)
- BCryptPasswordEncoder
- Roles: USER / ADMIN
- `@PreAuthorize` with custom permission logic
- Public vs. protected vs. admin-only endpoints

### Module 8: Logging (Lesson 17)

- `@Slf4j` from Lombok on Services
- `log.info()` on create / update / delete
- Log levels per profile
- Correlation ID for request tracing with downstream propagation

### Module 9: Global Exception Handling (Lesson 18)

- `@ControllerAdvice` with `GlobalExceptionHandler`
- `IllegalArgumentException` → 400
- `MethodArgumentNotValidException` → 400
- `EntityNotFoundException` → 404
- `BadCredentialsException` → 401
- `AccessDeniedException` → 403
- **`ResourceAccessException` → 503 (downstream unavailable)**
- Generic `Exception` → 500
- Dev (stack trace) vs. prod (message only) responses

---

## Documentación de apoyo

- [TEST-STRATEGY.md](TEST-STRATEGY.md) — Estrategia de pruebas unitarias (154 tests, 10 servicios)
- [API-DOCS.md](API-DOCS.md) — Documentación Swagger/OpenAPI (11 servicios)

---

## Bugs

> Pre-existing issues in downstream services found during gateway testing. `[SERVICE]` = affected service, `[GW]` = gateway issue.

### High: 400/404/500 mismatches (semantic HTTP codes)

| # | Service | Endpoint | Got | Expected | Root Cause |
|---|---------|----------|-----|----------|------------|
| 4 | category-service | `PUT /api/categories/{id}` | 405 | 200 | PUT endpoint removed entirely — feature missing, not a 500 bug |
| 8 | product-service | `GET /api/products/{id}/attributes/{badId}` | 404 | 404 | Endpoint removed entirely — GET single attribute feature missing |

### Medium: Missing/incorrect validation

| # | Service | Endpoint | Got | Expected | Root Cause |
|---|---------|----------|-----|----------|------------|
| 12 | hardware-advisor-service | `GET /api/recommendations/{badBuildId}` | 200 (empty) | 404 | Returns empty list instead of 404 for nonexistent build — blocked on FeignClient → build-service |
| 13 | hardware-advisor-service | `POST /api/recommendations/generate` (bad buildId) | 201 | 404 | Generates recommendations for nonexistent build — blocked on FeignClient → build-service |

<!-- ### Low: Missing feature

| # | Service | Endpoint | Got | Expected | Root Cause |
|---|---------|----------|-----|----------|------------| -->

---

