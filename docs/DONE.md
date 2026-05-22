# DONE.md

> Archive of completed work per service.

---

### api-gateway :8080

- [x] `spring-boot-starter-security` in `pom.xml`
- [x] `GatewayRoutesConfig` — RouterFunction with GatewayRouterFunctions.route() + BeforeFilterFunctions.uri()
- [x] All 9 `*_SERVICE_URL` vars in `.env.example`
- [x] `AuthClientResponse` — DTO record for auth-service validate response
- [x] `RestClientConfig` — `RestClient.Builder` bean
- [x] `AuthRestClient` — RestClient → auth-service:8081/api/auth/validate (Module 5)
- [x] `JwtAuthFilter` — OncePerRequestFilter for Bearer token delegation (Module 7)
- [x] `SecurityConfig` — SecurityFilterChain with STATELESS, CSRF disabled, route rules (Module 7)
- [x] `CorrelationIdFilter` — MDC-based X-Correlation-Id tracing (Module 8)
- [x] `ApiError` record + `GlobalExceptionHandler` (Module 9)
- [x] Endpoint tests scripts

### auth-service :8081

- [x] Model / Entity (Credential) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs (Module 1)
- [x] Service + Controller + Exception handling (Module 1)
- [x] Seed credentials for admin + test users
- [x] BCrypt password hashing (Module 7)
- [x] JWT generation / validation (Module 7)
- [x] Roles: USER / ADMIN (Module 7)
- [x] Flyway migrations (Module 6)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Align application-h2.yaml to use `data.sql` + `ddl-auto: create-drop` (Flyway H2 removed)
- [x] Generate correct BCrypt hash for admin123 / test123 in seed data (Bug #15)
- [x] H2 console secured behind admin role

### category-service :8084
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Seed data (8 categories, ~30 attributes) (Module 2)
- [x] @OneToMany Category → AttributeDefinition (Module 3)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)

### product-service :8083

- [x] Model / Entity (Product, ProductAttribute) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Seed data (32 products, ~100 attributes) (Module 2)
- [x] @OneToMany Product → ProductAttribute (Module 3)
- [x] RestClient → category-service (Module 5)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)

### compatibility-service :8085

- [x] Model / Entity (CompatibilityRule, CompatibilityCheck) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Seed data (5 rules) (Module 2)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)
- [x] RestClient → product-service (Module 5)
- [x] RestClientConfig, ProductRestClient, DTOs
- [x] `evaluateRule()` business logic (stub — always passes, pending attribute comparison logic)
- [x] Align CompatibilityCheck entity in README.md: added `buildId` and `createdAt`

### provider-service :8086

- [x] Model / Entity (Provider, ProviderProduct) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Seed data (4 providers) (Module 2)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)

### build-service :8087

- [x] Model / Entity (Build, BuildItem) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] @OneToMany Build → BuildItem (Module 3)
- [x] Seed data (5 builds, ~25 items) (Module 2)
- [x] PATCH /api/builds/{id}/status (Module 3)
- [x] FeignClient → product-service (Module 5)
- [x] FeignClient → compatibility-service (Module 5)
- [x] FeignClient → provider-service (Module 5)
- [x] Flyway migrations (Module 6)
- [x] application-mysql.yaml + application.yaml with service URLs
- [x] .env.example with URL vars
- [x] Endpoint tests script
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] Per-profile log levels (Module 8)
- [x] File-based logging
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Data seed fix + bug fixes

### estimate-service :8088

- [x] Model / Entity (Estimate) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Seed data (3 sample estimates) (Module 2)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)

### hardware-advisor :8089

- [x] Model / Entity (Recommendation) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Seed data (3 sample recommendations) (Module 2)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)

### notification-service :8090

- [x] Model / Entity (NotificationLog) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Migrate to JPA + database (Module 2)
- [x] Seed data (3 sample notification logs) (Module 2)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)

---

### Cross-cutting (GlobalExceptionHandler additions)

- [x] MissingRequestHeaderException → 401 handler in all services (Bug #1)
- [x] MethodArgumentTypeMismatchException → 400 handler in all services (Bugs #2–8)
- [x] HttpRequestMethodNotSupportedException → 405 handler in all services
- [x] ResourceAccessException → 503 handler in all services (Gap #18)

### Product renaming

- [x] Product entity `price` → `msrp` (entity, DTOs, repository, client DTOs, SQL)
- [x] Estimate entity `totalPrice` → `totalCost` (model, DTO, SQL)
- [x] Test scripts + Postman request bodies updated to `msrp`
- [x] Docs: ARCHITECTURE.md, PROGRESS.md, shared-context.md, INTER-SERVICE-WIRING.md updated

### Bugs fixed

- [x] Bug #1: `MissingRequestHeaderException` → 401 in auth-service
- [x] Bugs #2–8: `MethodArgumentTypeMismatchException` → 400 in all services
- [x] Bug #15: Admin BCrypt hash corrected in auth-service seed data