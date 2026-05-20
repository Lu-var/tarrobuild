# Build-Service Plan

## Stage 1: Fix 3 known bugs (surgical, highest impact) DONE

---

<!-- | # | File | What | Why |
|---|------|------|-----|
| 1 | `BuildController.java` | Add `PUT /{buildId}/items/{itemId}` | Currently 500 (no mapping exists) |
| 2 | `BuildService.java` | Add `updateItem()` method | Service method doesn't exist |
| 3 | `BuildController.java` | Add `PATCH /{id}/status` | Currently 404 (not implemented) |
| 4 | `BuildService.java` | Add `updateBuildStatus()` method | Needed for PATCH endpoint |
| 5 | `data.sql` | Fix product IDs (101-124 → 1-32) | Seed data references nonexistent products |git  -->

## Stage 2: Validate product existence (FeignClient → product-service) DONE

---

<!-- | # | File | What |
|---|------|------|
| 6 | `pom.xml` | Add `spring-cloud-starter-openfeign` |
| 7 | `client/ProductClient.java` | FeignClient → `product-service:8083` |
| 8 | `BuildService.java` | Call ProductClient in `createItem()` — return 404 if productId doesn't exist | -->

## Stage 3: Other inter-service FeignClients

| # | File | What |
|---|------|------|
| 9 | `client/CompatibilityClient.java` | FeignClient → `compatibility-service:8085` |
| 10 | `client/ProviderClient.java` | FeignClient → `provider-service:8086` |

## Stage 4: Infrastructure & polish

| # | File | What |
|---|------|------|
| 11 | `db/migration/mysql/V1__init.sql` | Flyway schema for MySQL |
| 12 | `db/migration/mysql/V2__seed_data.sql` | Flyway seed data with corrected product IDs |
| 13 | `application-mysql.yaml` | MySQL + Flyway config |
| 14 | `GlobalExceptionHandler.java` | Add `NumberFormatException` → 400, `AccessDeniedException` → 403 |
| 15 | `BuildServiceApplication.java` | Add `@EnableFeignClients` |
| 16 | `.env.example` | Add service URL vars |
| 17 | Endpoint test script | Following notification-service pattern |
| 18 | Unit/integration tests | — |

## Bug fixes mapping

| Bug from integration test | Stage |
|--------------------------|-------|
| `PUT /{buildId}/items/{itemId}` → 500 | Stage 1 |
| `PATCH /{buildId}/status` → 404 | Stage 1 |
| `POST /{buildId}/items` with bad productId → 201 | Stage 2 |
