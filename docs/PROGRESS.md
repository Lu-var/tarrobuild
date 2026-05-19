# PROGRESS.md

---

## Functional requirements (RF)

- [x] **RF-01** — Register user with bcrypt
- [x] **RF-02** — Authenticate user (JWT)
- [ ] **RF-03** — List catalog components
- [ ] **RF-04** — Component detail
- [ ] **RF-05** — Filter by category/brand/price
- [ ] **RF-06** — Create build
- [ ] **RF-07** — Manage build items
- [ ] **RF-08** — Compatibility check
- [ ] **RF-09** — Power consumption estimate
- [ ] **RF-10** — Reference prices from providers
- [ ] **RF-11** — Consolidated build analysis
- [ ] **RF-12** — Save favorite builds
- [ ] **RF-13** — Upgrade recommendations
- [ ] **RF-14** — CRUD components/attributes
- [ ] **RF-15** — CRUD compatibility rules
- [ ] **RF-16** — CRUD price references
- [ ] **RF-17** — Price/availability alerts
- [ ] **RF-18** — Auto-notifications

## Non-functional requirements (RNF)

- [ ] **RNF-01** — Response < 500ms
- [ ] **RNF-02** — Validation + semantic HTTP codes
- [ ] **RNF-03** — Structured logs with correlation ID
- [ ] **RNF-04** — Independent DB per service
- [ ] **RNF-05** — REST-only inter-service communication
- [x] **RNF-06** — BCrypt password encryption

## User stories (HU)

- [x] **HU-01** — User registration
- [x] **HU-02** — Authentication
- [ ] **HU-03** — Catalog exploration
- [ ] **HU-04** — Build creation
- [ ] **HU-05** — Compatibility validation
- [ ] **HU-06** — Cost estimation
- [ ] **HU-07** — Component recommendations
- [ ] **HU-08** — Build history
- [ ] **HU-09** — System notifications
- [ ] **HU-10** — Admin catalog management

---


## Services

> Current progress per service
---

### api-gateway :8080

- [x] `spring-boot-starter-security` in `pom.xml`
- [x] `GatewayRoutesConfig` — `RouterFunction<ServerResponse>` with `GatewayRouterFunctions.route()` + `BeforeFilterFunctions.uri()` (replaced YAML routes)
- [x] All 9 `*_SERVICE_URL` vars in `.env.example`
- [x] `AuthClientResponse` — DTO record for auth-service validate response
- [x] `RestClientConfig` — `RestClient.Builder` bean
- [x] `AuthRestClient` — RestClient → `auth-service:8081/api/auth/validate` for token delegation (Module 5)
- [x] `JwtAuthFilter` — `OncePerRequestFilter` that extracts Bearer token, delegates to auth-service, populates `SecurityContext`. Blocks invalid tokens with 401 immediately (Module 7)
- [x] `SecurityConfig` — `SecurityFilterChain` with STATELESS sessions, CSRF disabled, public vs. user vs. admin route rules per ARCHITECTURE.md (Module 7)
- [x] `CorrelationIdFilter` — MDC-based `X-Correlation-Id` request tracing (Module 8)
- [x] `ApiError` record — `{ message, details, timestamp }` (Module 9)
- [x] `GlobalExceptionHandler` — `@ControllerAdvice` with `IllegalArgument`→400, `MethodArgumentNotValid`→400, `NoResourceFound`→404, `HttpMessageNotReadable`→400, `BadCredentials`→401, `AccessDenied`→403, generic `Exception`→500 (Module 9)
- [ ] Endpoint tests script
- [ ] Tests

### auth-service :8081

- [x] Model / Entity (Credential) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [x] Seed credentials for admin + test users (link to existing user-service profiles)
- [x] BCrypt password hashing (Module 7)
- [x] JWT token generation / validation (Module 7)
- [x] Roles: USER / ADMIN (Module 7)
- [x] Flyway migrations — MySQL (Module 6)
- [ ] Align application-h2.yaml to use `data.sql` + `ddl-auto: create-drop` (like other services)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [ ] Add ResourceAccessException → 503 to GlobalExceptionHandler (downstream service unavailable)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### user-service :8082

- [x] Model / Entity (User) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [x] Seed data (20 users) (Module 2)
- [ ] User with unique email (Module 3)
- [ ] Ensure email uniqueness validated across user-service and auth-service (duplicate check in register flow)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### category-service :8084

- [x] Model / Entity (Category, AttributeDefinition) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [x] Seed data (8 categories, ~30 attributes) (Module 2)
- [x] `@OneToMany` Category -> AttributeDefinition (Module 3)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### product-service :8083

- [x] Model / Entity (Product, ProductAttribute) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [x] Seed data (32 products, ~100 attributes) (Module 2)
- [x] `@OneToMany` Product -> ProductAttribute (Module 3)
- [x] RestClient → category-service (Module 5)
- [x] Add to ARCHITECTURE.md inter-service table (product → category)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Refactor endpoint tests script (notification template)
- [ ] Tests

### compatibility-service :8085

- [x] Model / Entity (CompatibilityRule, CompatibilityCheck) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [x] Seed data (5 rules) (Module 2)
- [ ] RestClient → product-service (Module 5)
- [x] Add to ARCHITECTURE.md inter-service table (compatibility → product)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Align CompatibilityCheck entity in README.md: add `buildId` and `createdAt`
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### provider-service :8086

- [x] Model / Entity (Provider, ProviderProduct) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [x] Seed data (4 providers) (Module 2)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### build-service :8087

- [x] Model / Entity (Build, BuildItem) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [ ] Seed data (5 builds, ~25 items) (Module 2) — product IDs (101–124) don't match product-service (1–32)
- [x] `@OneToMany` Build -> BuildItem (Module 3)
- [ ] PATCH /api/builds/{id}/status for partial updates (Module 3)
- [ ] FeignClient → product-service (Module 5)
- [ ] FeignClient → compatibility-service (Module 5)
- [ ] FeignClient → provider-service (Module 5)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### estimate-service :8088

- [x] Model / Entity (Estimate) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [ ] Seed data (Module 2)
- [ ] RestClient → build-service (Module 5)
- [ ] RestClient → product-service (Module 5)
- [ ] RestClient → notification-service (Module 5)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### hardware-advisor :8089

- [x] Model / Entity (Recommendation) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [ ] Seed data (Module 2)
- [ ] FeignClient → build-service (Module 5)
- [ ] FeignClient → product-service (Module 5)
- [ ] FeignClient → compatibility-service (Module 5)
- [ ] FeignClient → notification-service (Module 5)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Endpoint tests script (notification as template)
- [ ] Tests

### notification-service :8090

- [x] Model / Entity (NotificationLog) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Request / Response) (Module 1)
- [x] Service (Module 1)
- [x] Controller (Module 1)
- [x] Exception handling (Module 1)
- [x] Migrate to JPA + database (Module 2)
- [ ] Seed data (Module 2)
- [ ] Flyway migrations (Module 6)
- [x] `@Slf4j` on services + `log.info()` in CRUD
- [ ] Per-profile log levels + correlation ID filter
- [x] GlobalExceptionHandler (Module 9)
- [ ] Add AccessDeniedException → 403 to GlobalExceptionHandler (Module 9)
- [x] Handle HttpMessageNotReadableException in GlobalExceptionHandler (invalid enums return 400)
- [ ] Endpoint tests script (no script file found)
- [ ] Tests

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
- Correlation ID for request tracing

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