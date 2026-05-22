# PROGRESS.md

---

## Functional requirements (RF)

- [x] **RF-01** — Register user with bcrypt
- [x] **RF-02** — Authenticate user (JWT)
- [x] **RF-03** — List catalog components
- [x] **RF-04** — Component detail
- [ ] **RF-05** — Filter by category/brand/price
- [x] **RF-06** — Create build
- [x] **RF-07** — Manage build items
- [ ] **RF-08** — Compatibility check
- [ ] **RF-09** — Power consumption estimate (cost: done via estimate-service, power validation: pending in compatibility-service)
- [x] **RF-10** — Provider references (SKU / external links)
- [ ] **RF-11** — Consolidated build analysis
- [ ] **RF-12** — Save favorite builds
- [ ] **RF-13** — Upgrade recommendations
- [x] **RF-14** — CRUD components/attributes
- [x] **RF-15** — CRUD compatibility rules
- [x] **RF-16** — CRUD price references
- [ ] **RF-17** — Price/availability alerts
- [ ] **RF-18** — Auto-notifications

## Non-functional requirements (RNF)

- [ ] **RNF-01** — Response < 500ms
- [x] **RNF-02** — Validation + semantic HTTP codes
- [ ] **RNF-03** — Structured logs with correlation ID and downstream propagation
- [x] **RNF-04** — Independent DB per service
- [x] **RNF-05** — REST-only inter-service communication
- [x] **RNF-06** — BCrypt password encryption

## User stories (HU)

- [x] **HU-01** — User registration
- [x] **HU-02** — Authentication
- [x] **HU-03** — Catalog exploration
- [x] **HU-04** — Build creation
- [ ] **HU-05** — Compatibility validation
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
- [ ] Add MethodArgumentTypeMismatchException → 400 handler to every GlobalExceptionHandler (Bugs #2–8)
- [ ] Ensure correlation ID propagation is consistently planned across all services (RNF-03)
- [ ] Add user identity propagation — gateway forwards userId/email/role as headers, downstream services consume them (Gap #17)
- [ ] Add ResourceAccessException → 503 handler to every GlobalExceptionHandler (Gap #18)
- [ ] (optional) Split profiles into `dev`/`prod` (environment) + `h2`/`mysql` (database)

---
### api-gateway :8080

**Pending**
- [ ] Add `RestClientConfig` (missing explicit bean, currently using Spring auto-config)
- [ ] Propagate user identity (userId/email/role) as headers to downstream services
- [ ] Tests

---

### auth-service :8081

**Pending**
- [ ] Align application-h2.yaml to use `data.sql` + `ddl-auto: create-drop` (like other services)
- [ ] Correlation ID propagation to downstream services
- [ ] Add MissingRequestHeaderException → 401 handler
- [ ] Generate correct BCrypt hash for 'admin123' and replace placeholder in V2__seed_data.sql
- [ ] Add ResourceAccessException → 503 to GlobalExceptionHandler (downstream service unavailable)
- [ ] Endpoint tests script
- [ ] Tests

---
### user-service :8082

**Pending**
- [ ] User with unique email (Module 3) — add `@Column(unique = true)` to `User.email`
- [ ] Ensure email uniqueness validated across user-service and auth-service (duplicate check in register flow)
- [ ] PATCH endpoint for partial user updates
- [ ] Correlation ID propagation to downstream services
- [ ] Endpoint tests script
- [ ] Tests

---

### category-service :8084

**Pending**
- [ ] PATCH endpoint for partial category updates
- [ ] Correlation ID propagation to downstream services
- [ ] Endpoint tests script
- [ ] Tests

---

### product-service :8083

**Pending**
- [ ] FeignClient fallbacks and configurable timeouts (Module 5)
- [ ] Correlation ID propagation to downstream services
- [ ] Endpoint tests script
- [ ] Tests

---

### compatibility-service :8085

**Pending**
- [ ] `evaluateRule()` business logic
- [ ] Correlation ID propagation to downstream services
- [ ] Align CompatibilityCheck entity in README.md: add `buildId` and `createdAt`
- [ ] Endpoint tests script
- [ ] Tests

---
### provider-service :8086

**Pending**
- [ ] Add `@ManyToOne`/`@OneToMany` JPA relations on `Provider` and `ProviderProduct` instead of scalar `Long` fields (Module 3)
- [ ] PATCH endpoint for partial provider updates
- [ ] Correlation ID propagation to downstream services
- [ ] Endpoint tests script
- [ ] Tests

---

### build-service :8087

**Pending**
- [ ] (optional) Log who performed each action (Module 8 — extra challenge)
- [ ] FeignClient fallbacks and configurable timeouts (Module 5)
- [ ] Propagate user identity (userId/email/role) from incoming request for build ownership checks
- [ ] Correlation ID propagation to downstream services
- [ ] Tests

---

### estimate-service :8088

**Pending**
- [ ] Deduplicate keys in `application.yaml` (build/product/notification service URLs + server.port are duplicated)
- [ ] Correlation ID propagation to downstream services
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

---

### hardware-advisor-service :8089

**Pending**
- [ ] FeignClient → build-service (Module 5)
- [ ] FeignClient → product-service (Module 5)
- [ ] FeignClient → compatibility-service (Module 5)
- [ ] FeignClient → notification-service (Module 5)
- [ ] FeignClient fallbacks and configurable timeouts (Module 5)
- [ ] Correlation ID propagation to downstream services
- [ ] Implement generate() recommendation logic (fetch build → compare catalog → apply rules → persist)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

---

### notification-service :8090

**Pending**
- [ ] Replace `NoSuchElementException` with `EntityNotFoundException` for 404 responses
- [ ] Add `EntityNotFoundException` handler to `GlobalExceptionHandler`
- [ ] Add UserRestClient → user-service to resolve email on send()
- [ ] Correlation ID propagation to downstream services
- [ ] Endpoint tests script (no script file found)
- [ ] Tests

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

## Bugs

> Pre-existing issues in downstream services found during gateway testing. `[SERVICE]` = affected service, `[GW]` = gateway issue.

### High: 400/404/500 mismatches (semantic HTTP codes)

| # | Service | Endpoint | Got | Expected | Root Cause |
|---|---------|----------|-----|----------|------------|
| 1 | auth-service | `GET /api/auth/validate` (no header) | 500 | 401 | `MissingRequestHeaderException` unhandled → generic handler |
| 2 | user-service | `GET /api/users/{nonNumeric}` | 500 | 400 | `MethodArgumentTypeMismatchException` (wraps `NumberFormatException`) unhandled |
| 3 | category-service | `GET /api/categories/{nonNumeric}` | 500 | 400 | Same `MethodArgumentTypeMismatchException` as #2 |
| 4 | category-service | `PUT /api/categories/{id}` | 405 | 200 | PUT endpoint removed entirely — feature missing, not a 500 bug |
| 5 | product-service | `GET /api/products/{nonNumeric}` | 500 | 400 | Same `MethodArgumentTypeMismatchException` as #2 |
| 6 | product-service | `GET /api/products/category/{nonNumeric}` | 500 | 400 | Same `MethodArgumentTypeMismatchException` as #2 |
| 7 | product-service | `GET /api/products/price?minPrice=abc` | 500 | 400 | Same `MethodArgumentTypeMismatchException` on `@RequestParam` |
| 8 | product-service | `GET /api/products/{id}/attributes/{badId}` | 404 | 404 | Endpoint removed entirely — GET single attribute feature missing |

### Medium: Missing/incorrect validation

| # | Service | Endpoint | Got | Expected | Root Cause |
|---|---------|----------|-----|----------|------------|
| 12 | hardware-advisor-service | `GET /api/recommendations/{badBuildId}` | 200 (empty) | 404 | Returns empty list instead of 404 for nonexistent build — blocked on FeignClient → build-service |
| 13 | hardware-advisor-service | `POST /api/recommendations/generate` (bad buildId) | 201 | 404 | Generates recommendations for nonexistent build — blocked on FeignClient → build-service |

<!-- ### Low: Missing feature

| # | Service | Endpoint | Got | Expected | Root Cause |
|---|---------|----------|-----|----------|------------| -->

### Seed data issues

| # | Service | Details |
|---|---------|---------|
| 15 | auth-service | Admin BCrypt hash in `V2__seed_data.sql` doesn't match password "admin123" — placeholder hash used |

---

