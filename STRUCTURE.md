# Codebase Structure

## Directory Layout

```
tarrobuild/
├── services/                    # All microservices (each is a Maven module)
│   ├── api-gateway/             # API Gateway — single entry point, JWT auth, routing
│   ├── auth-service/            # Authentication — register, login, JWT
│   ├── user-service/            # User profiles CRUD
│   ├── product-service/         # Hardware catalog with specs and attributes
│   ├── category-service/        # Component categories and attribute definitions
│   ├── compatibility-service/   # Compatibility rule engine and check history
│   ├── provider-service/        # External vendor references and pricing
│   ├── build-service/           # Core build configurations (PC builds + items)
│   ├── estimate-service/        # Cost calculation for builds
│   ├── hardware-advisor-service/ # Upgrade and compatibility recommendations
│   ├── notification-service/    # System notifications (send + log)
│   └── discovery-server/        # Eureka service discovery server
├── docs/                        # Project documentation
├── postman/                     # Postman collections and environments
├── scripts/                     # Utility scripts (performance testing)
├── logs/                        # Runtime log files (git-ignored in prod)
├── pom.xml                      # Root Maven POM (parent for all modules)
├── compose.yml                  # Docker Compose (12 services + 10 MySQL DBs)
├── render.yaml                  # Render deployment blueprint
├── Dockerfile.render            # Shared Dockerfile for Render builds
└── README.md                    # Project overview and instructions
```

## Service Internal Layout

Every service under `services/<name>/` follows this structure:

```
services/<name>/
├── Dockerfile                   # Multi-stage Maven build → JRE runtime
├── entrypoint.sh                # DATABASE_URL → JDBC conversion + exec java
├── pom.xml                      # Service-specific Maven dependencies
├── mvnw / mvnw.cmd              # Maven wrapper
└── src/
    ├── main/
    │   ├── java/cl/tarrobuild/<package>/
    │   │   ├── *Application.java        # Spring Boot entry point
    │   │   ├── controller/              # @RestController classes
    │   │   ├── service/                 # @Service business logic
    │   │   ├── repository/              # JpaRepository interfaces
    │   │   ├── model/                   # @Entity JPA classes
    │   │   ├── dto/                     # Java records (Request/Response)
    │   │   ├── exception/               # GlobalExceptionHandler + ApiError
    │   │   ├── client/                  # Inter-service clients (RestClient/Feign)
    │   │   └── config/                  # OpenApiConfig, CorrelationIdFilter, etc.
    │   └── resources/
    │       ├── application.yaml              # Default profile (h2)
    │       ├── application-h2.yaml           # H2 in-memory database
    │       ├── application-mysql.yaml        # MySQL + Flyway
    │       ├── application-prod.yaml         # MySQL + Flyway (Docker Compose)
    │       ├── application-render.yaml       # PostgreSQL + Flyway (Render)
    │       └── db/migration/
    │           ├── mysql/
    │           │   ├── V1__init.sql
    │           │   └── V2__seed_data.sql
    │           └── postgresql/
    │               ├── V1__init.sql
    │               └── V2__seed_data.sql
    └── test/
        └── java/cl/tarrobuild/<package>/
            └── *Test.java                   # JUnit 5 + Mockito tests
```

## Directory Purposes

**`services/api-gateway/`:**
- Purpose: API Gateway — single entry point for all external traffic
- Contains: Route config, JWT filter, correlation ID filter, security config, public paths
- Key files: `GatewayRoutesConfig.java`, `JwtAuthFilter.java`, `SecurityConfig.java`, `PublicPaths.java`
- Note: No database, no JPA entities; purely a proxy and auth enforcement layer

**`services/auth-service/`:**
- Purpose: User registration, login, JWT emission, token validation
- Contains: `Credential` entity, `AuthService`, `JwtUtil`, `UserRestClient`
- Key files: `AuthService.java`, `AuthController.java`, `JwtUtil.java`, `UserRestClient.java`
- Note: Calls `user-service` during registration to create user profile

**`services/user-service/`:**
- Purpose: User profile management
- Contains: `User` entity, `UserService`, `UserController`
- Key files: `User.java`, `UserService.java`, `UserController.java`
- Note: Simple CRUD; no outbound service calls

**`services/product-service/`:**
- Purpose: Hardware component catalog with attributes
- Contains: `Product`, `ProductAttribute` entities, `CategoryRestClient`
- Key files: `Product.java`, `ProductAttribute.java`, `ProductController.java`, `CategoryRestClient.java`
- Note: Calls `category-service` to validate category on product creation

**`services/category-service/`:**
- Purpose: PC component categories and attribute type definitions
- Contains: `Category`, `AttributeDefinition` entities, `AttributeValueType` enum
- Key files: `Category.java`, `AttributeDefinition.java`, `CategoryController.java`
- Note: No outbound service calls

**`services/compatibility-service/`:**
- Purpose: Rule-based compatibility checking and history storage
- Contains: `CompatibilityRule`, `CompatibilityCheck` entities, `ProductRestClient`, `CategoryRestClient`
- Key files: `CompatibilityService.java`, `CompatibilityController.java`
- Note: Calls `product-service` and `category-service` to fetch specs for rule evaluation

**`services/provider-service/`:**
- Purpose: External vendor references and product pricing
- Contains: `Provider`, `ProviderProduct` entities
- Key files: `Provider.java`, `ProviderProduct.java`, `ProviderController.java`
- Note: No outbound service calls

**`services/build-service/`:**
- Purpose: Core service — PC build configurations with items, favorites, history
- Contains: `Build`, `BuildItem`, `BuildHistory`, `Favorite` entities, Feign clients
- Key files: `BuildController.java`, `BuildService.java`, `BuildItem.java`, `FavoriteService.java`, `BuildHistoryService.java`
- Note: Uses FeignClient for multi-service orchestration (product, compatibility, notification)

**`services/estimate-service/`:**
- Purpose: Cost calculation for builds based on product MSRP
- Contains: `Estimate` entity, RestClient-based clients
- Key files: `EstimateController.java`, `EstimateService.java`, `BuildRestClient.java`, `ProductRestClient.java`, `NotificationRestClient.java`
- Note: Uses RestClient (not Feign) for inter-service calls

**`services/hardware-advisor-service/`:**
- Purpose: Generate hardware upgrade and compatibility recommendations
- Contains: `Recommendation` entity, Feign clients
- Key files: `HardwareAdvisorController.java`, `HardwareAdvisorService.java`
- Note: Most complex outbound dependency — calls build, product, compatibility, and notification services via FeignClient

**`services/notification-service/`:**
- Purpose: Send and log system notifications asynchronously
- Contains: `NotificationLog` entity, `NotificationStatus` enum, `UserRestClient`
- Key files: `NotificationController.java`, `NotificationService.java`
- Note: `@EnableAsync` for async notification processing; calls `user-service` for user info

**`services/discovery-server/`:**
- Purpose: Netflix Eureka service registry
- Contains: Only `DiscoveryServerApplication.java`
- Key files: `DiscoveryServerApplication.java`, `application.yaml`
- Note: `@EnableEurekaServer`, self-preservation disabled, 5s eviction interval

**`docs/`:**
- Purpose: Project documentation (architecture, progress, API docs, defense materials)
- Contains: Markdown files, PDF evaluation rubric
- Key files: `ARCHITECTURE.md`, `PROGRESS.md`, `DONE.md`, `API-DOCS.md`, `TEST-STRATEGY.md`

**`postman/`:**
- Purpose: API testing collections organized per service
- Contains: Collections (Direct, Gateway, Monolith variants), environments, globals, flows, mocks, OpenAPI specs
- Key files: `collections/*/` (one folder per service), `environments/local.environment.yaml`

**`scripts/`:**
- Purpose: Utility scripts for performance testing
- Contains: `performance-test.ps1`

## Key File Locations

**Entry Points:**
- `services/api-gateway/src/main/java/cl/tarrobuild/apigateway/ApiGatewayApplication.java` — Gateway application entry point
- `services/discovery-server/src/main/java/cl/tarrobuild/discovery/DiscoveryServerApplication.java` — Eureka server entry point
- `services/<service>/src/main/java/cl/tarrobuild/<package>/*Application.java` — Per-service entry point

**Configuration:**
- `pom.xml` — Root Maven POM (parent, dependency management, Spring Boot 4.0.6, Spring Cloud 2025.1.1)
- `services/<service>/pom.xml` — Per-service dependencies
- `services/<service>/src/main/resources/application.yaml` — Default config (port, profiles, Eureka)
- `services/<service>/src/main/resources/application-{profile}.yaml` — Profile-specific config (h2/mysql/render/prod)
- `.env.example` — Environment variable template
- `compose.yml` — Docker Compose (12 services, 10 MySQL databases, healthchecks)
- `render.yaml` — Render deployment blueprint (all services + PostgreSQL)

**Core Logic:**
- `services/api-gateway/src/main/java/cl/tarrobuild/apigateway/config/GatewayRoutesConfig.java` — All gateway route definitions
- `services/api-gateway/src/main/java/cl/tarrobuild/apigateway/filter/JwtAuthFilter.java` — JWT validation and identity injection
- `services/api-gateway/src/main/java/cl/tarrobuild/apigateway/config/SecurityConfig.java` — Spring Security authorization rules
- `services/auth-service/src/main/java/cl/tarrobuild/auth/service/AuthService.java` — Authentication logic
- `services/build-service/src/main/java/cl/tarrobuild/build/service/BuildService.java` — Core build management logic
- `services/compatibility-service/src/main/java/cl/tarrobuild/compatibility/service/CompatibilityService.java` — Compatibility rule evaluation

**Tests:**
- `services/api-gateway/src/test/java/cl/tarrobuild/apigateway/` — Gateway tests (SecurityConfig, filters, exception handler)
- `services/<service>/src/test/java/cl/tarrobuild/<package>/` — Per-service tests

**Database Migrations:**
- `services/<service>/src/main/resources/db/migration/mysql/V1__init.sql` — MySQL schema
- `services/<service>/src/main/resources/db/migration/mysql/V2__seed_data.sql` — MySQL seed data
- `services/<service>/src/main/resources/db/migration/postgresql/V1__init.sql` — PostgreSQL schema
- `services/<service>/src/main/resources/db/migration/postgresql/V2__seed_data.sql` — PostgreSQL seed data

## Naming Conventions

**Files:**
- Application classes: `*Application.java` (e.g., `BuildServiceApplication.java`)
- Controllers: `*Controller.java` (e.g., `BuildController.java`)
- Services: `*Service.java` (e.g., `BuildService.java`, `FavoriteService.java`)
- Repositories: `*Repository.java` (e.g., `BuildRepository.java`)
- Entities: Singular noun, `@Entity` (e.g., `Build.java`, `Product.java`)
- DTOs: `*Request.java`, `*Response.java` (e.g., `BuildRequest.java`, `BuildResponse.java`)
- REST clients: `*RestClient.java` (e.g., `UserRestClient.java`)
- Feign clients: `*FeignClient.java` (e.g., `ProductFeignClient.java`)
- Feign fallbacks: `*FeignClientFallbackFactory.java` (e.g., `ProductFeignClientFallbackFactory.java`)
- Config: `*Config.java` (e.g., `SecurityConfig.java`, `OpenApiConfig.java`, `FeignConfig.java`)
- Filters: `*Filter.java` (e.g., `JwtAuthFilter.java`, `CorrelationIdFilter.java`)
- Exception handler: `GlobalExceptionHandler.java`
- Error model: `ApiError.java`

**Directories:**
- Service names: lowercase with hyphens (e.g., `api-gateway`, `build-service`, `hardware-advisor-service`)
- Java packages: `cl.tarrobuild.<servicename>` (e.g., `cl.tarrobuild.build`, `cl.tarrobuild.apigateway`)
- Sub-packages: `controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/`, `client/`, `config/`, `filter/`

**Database:**
- Database names: `db_<service>` (e.g., `db_auth`, `db_builds`, `db_recommendations`)
- Table names: Plural lowercase (e.g., `builds`, `products`, `users`, `build_items`)
- Column names: snake_case (e.g., `user_id`, `created_at`, `password_hash`, `category_id`)
- Flyway migrations: `V1__init.sql`, `V2__seed_data.sql`

## Where to Add New Code

**New microservice:** `services/<service-name>/` — create `pom.xml` (parent: root `pom.xml`), `Dockerfile`, `entrypoint.sh`, `src/main/java/cl/tarrobuild/<package>/` following the standard layout, add `<module>` to root `pom.xml`, add to `compose.yml` and `render.yaml`

**New entity:** `services/<service>/src/main/java/cl/tarrobuild/<package>/model/<Entity>.java` — `@Entity` with Lombok, add Flyway migration `V3__*.sql` in both `mysql/` and `postgresql/` directories

**New controller endpoint:** `services/<service>/src/main/java/cl/tarrobuild/<package>/controller/<Service>Controller.java` — add `@GetMapping`/`@PostMapping`/etc., return `ResponseEntity<T>`, use `@Valid @RequestBody` for mutations

**New service method:** `services/<service>/src/main/java/cl/tarrobuild/<package>/service/<Service>Service.java` — add business logic, throw `EntityNotFoundException`/`IllegalArgumentException`/`EntityExistsException`

**New inter-service client (RestClient):** `services/<origin>/src/main/java/cl/tarrobuild/<package>/client/<Target>RestClient.java` — inject `RestClient.Builder`, set `lb://<target-service>` as base URL, add `onStatus` handlers

**New inter-service client (Feign):** `services/<origin>/src/main/java/cl/tarrobuild/<package>/client/<Target>FeignClient.java` — `@FeignClient(name = "<target-service>")`, add `*FallbackFactory` class, register in `@EnableFeignClients`

**New DTO:** `services/<service>/src/main/java/cl/tarrobuild/<package>/dto/` — `*Request.java` (record with validation), `*Response.java` (record without validation)

**New filter:** `services/<service>/src/main/java/cl/tarrobuild/<package>/filter/` — extend `OncePerRequestFilter` or implement `Filter`, register in `SecurityConfig` or auto-detect via `@Component`

**New test:** `services/<service>/src/test/java/cl/tarrobuild/<package>/` — JUnit 5 + Mockito, name as `<Class>Test.java`

**New Postman collection:** `postman/collections/<Service Name>/` — create folders for `Direct/`, `Gateway/`, `Monolith/` variants

**New database migration:** `services/<service>/src/main/resources/db/migration/{mysql,postgresql}/V<next>__<description>.sql`
