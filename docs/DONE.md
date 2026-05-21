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

### user-service :8082

- [x] Model / Entity (User) (Module 2)
- [x] Repository (Module 2)
- [x] DTOs, Service, Controller, Exception handling (Module 1)
- [x] Seed data (20 users) (Module 2)
- [x] `@Slf4j` + `log.info()` in CRUD
- [x] GlobalExceptionHandler (Module 9)
- [x] HttpMessageNotReadableException → 400
- [x] Per-profile log levels (Module 8)
- [x] NumberFormatException → 400 + AccessDeniedException → 403
- [x] Flyway migrations (Module 6)

### category-service :8084

- [x] Model / Entity (Category, AttributeDefinition) (Module 2)
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