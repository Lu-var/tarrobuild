# Alignment Revision Plan

**Generated:** May 2026 · **Updated:** Applied
**Supersedes:** Section 9 of `tarrobuild-alignment-analysis.md`

[//]: # (---)

[//]: # ()
[//]: # (## ✅ PROGRESS.md — Applied)

[//]: # ()
[//]: # (- RF section: 8 checkboxes flipped `[ ]`→`[x]`, RF-09 description updated, RF-10 renamed)

[//]: # (- HU section: HU-06 flipped `[ ]`→`[x]`)

[//]: # (- Added cross-cutting block &#40;MethodArgumentTypeMismatchException + correlation ID&#41;)

[//]: # (- auth-service :8081: added 2 pending items &#40;MissingRequestHeaderException, BCrypt hash&#41;)

[//]: # (- hardware-advisor → hardware-advisor-service header rename + generate&#40;&#41; item)

[//]: # (- notification-service :8090: added UserRestClient item)

[//]: # (- Added architecture documentation fixes block)

[//]: # (- Bugs table: 2 name fixes &#40;hardware-advisor → hardware-advisor-service&#41;)

[//]: # (- Removed all `@PreAuthorize` pending items and architecture gap #16)

[//]: # ()
[//]: # (## ✅ README.md &#40;ERS&#41; — Applied)

[//]: # ()
[//]: # (- `WebClient` → `RestClient` on lines 71, 86, 127)

[//]: # (- RF-09: split cost/power description)

[//]: # (- RF-10: removed "precios referenciales y disponibilidad aproximada")

[//]: # (- MS-06: added `buildId` and `createdAt` to CompatibilityCheck entity)

[//]: # (- MS-10: `hardware-advisor` → `hardware-advisor-service`)

[//]: # (- MS-11: removed "consulta user-service")

[//]: # (- §5.1: renamed 4 rows to `hardware-advisor-service`)

[//]: # ()
[//]: # (## ✅ ARCHITECTURE.md — Applied)

[//]: # ()
[//]: # (- MS-10 header: `hardware-advisor` → `hardware-advisor-service`)

[//]: # (- Product entity: `price` → `msrp`)

[//]: # (- Inter-service table: 4 rows renamed)

[//]: # (- H2 contradiction resolved: line 285 fixed to `ddl-auto: create-drop + data.sql` &#40;matching line 61 and the actual convention used by 9/10 services&#41;)

---

## Remaining work

### Code

| Priority | File(s) | Change |
|----------|---------|--------|
| High | `CompatibilityService.java` | Implement real `evaluateRule()` using product attributes from ProductRestClient |
| High | `HardwareAdvisorService.java` + new `client/` package | Implement `generate()` logic + 4 FeignClients (build, product, compatibility, notification) |
| High | auth-service `GlobalExceptionHandler.java` | Add `MissingRequestHeaderException → 401` handler |
| Medium | All 11 services `GlobalExceptionHandler.java` | Add `MethodArgumentTypeMismatchException → 400` handler |
| Medium | `Product.java`, `V1__init.sql`, `V2__seed_data.sql`, `data.sql`, all DTOs | `price` → `msrp` |
| Medium | auth-service `V2__seed_data.sql` | Replace placeholder BCrypt hash with correct hash for 'admin123' |

[//]: # (---)

[//]: # ()
[//]: # (## Scope explicitly dismissed)

[//]: # ()
[//]: # (- Stock/availability tracking — out of course scope)

[//]: # (- Spring Boot version in ARCH.md — 4.0.6 is correct)

[//]: # (- HU-08 as "Partial" — acceptance criteria met, stays Done)

[//]: # (- Folder rename — only doc headers needed fixing &#40;done&#41;)

[//]: # (- notification→user ERS/ARCH ambiguity — solved by implementing RestClient)

[//]: # (- `@PreAuthorize` — gateway path rules already enforce roles per ERS)
