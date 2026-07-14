# Plan de Cierre y Feedback — TarroBuild

## Feedback recibido y acciones realizadas

| ID | Observación o feedback | Acción realizada | Archivo(s) modificados | Evidencia de verificación | Estado |
|----|----------------------|------------------|------------------------|---------------------------|--------|
| FB-01 | Auth-service lanzaba `ResponseStatusException` incorrecta | Cambiado a `IllegalArgumentException` con `@ExceptionHandler` | `AuthService.java`, `GlobalExceptionHandler.java` | Test `register_emailDuplicado_lanzaExcepcion` pasa | Corregido |
| FB-02 | Estimate-service usaba CLP como moneda por defecto | Cambiado default a `USD` | `EstimateService.java` | Test `calcular_estimate_monedaUSD` pasa | Corregido |
| FB-03 | Gateway tenía rutas públicas duplicadas en JwtAuthFilter y SecurityConfig | Creado `PublicPaths.java` como constante compartida | `PublicPaths.java`, `JwtAuthFilter.java`, `SecurityConfig.java` | Ambos filtros usan la misma lista | Corregido |
| FB-04 | Notification-service tenía `@ExceptionHandler` duplicado | Eliminado handler redundante | `GlobalExceptionHandler.java` | Compila sin errores | Corregido |
| FB-05 | ProviderFeignClient era código muerto (nunca inyectado) | Cliente eliminado | `ProviderFeignClient.java`, `ProviderFeignClientFallbackFactory.java` | No hay referencias en el código | Corregido |
| FB-06 | Sin filtro de correlación en servicios downstream | Agregado `CorrelationIdFilter` en 10 servicios | `CorrelationIdFilter.java` en cada servicio | Header X-Correlation-Id presente en logs | Corregido |
| FB-07 | Sin @PreAuthorize en auth-service | Agregado `@PreAuthorize` en `AuthController.java` + `PreAuthorizeConfig.java` | `AuthController.java`, `PreAuthorizeConfig.java` | Test de seguridad verifican acceso por rol | Corregido |
| FB-08 | build-service sin trigger de compatibilidad al crear items | Agregada llamada a `CompatibilityFeignClient` en `BuildService` | `BuildService.java` | Log muestra "Compatibility check" en creación de items | Corregido |
| FB-09 | Sin aplicación-prod.yaml para Docker compose | Creado `application-prod.yaml` en 11 servicios | `application-prod.yaml` en cada servicio | Docker compose levanta con perfil prod | Corregido |
| FB-11 | Sin unit tests en la mayoría de servicios | Agregados 204 tests en 11 servicios | Múltiples archivos en `src/test/java/` | `mvn test` pasa en todos los servicios | Corregido |
| FB-12 | Sin springdoc/OpenAPI en la mayoría de servicios | Agregado springdoc 2.8.6 en 11 servicios | `pom.xml` de cada servicio, `OpenApiConfig.java` | `/v3/api-docs` responde en todos los servicios | Corregido |
| FB-13 | Sin roles ADMIN/USER en build-service | Agregada lógica de roles en `BuildController` y `BuildService` | `BuildController.java`, `BuildService.java` | ADMIN ve todas las builds, USER solo las suyas | Corregido |
| FB-14 | Sin docker compose funcional | Creado `compose.yml` con healthchecks y dependencias | `compose.yml`, `application-prod.yaml` | `docker compose up` levanta todo el stack | Corregido |
| FB-15 | Falta de documentación de arquitectura y APIs | Creados `ARCHITECTURE.md`, `API-DOCS.md`, `TEST-STRATEGY.md` | `docs/` múltiples archivos | Documentación revisada y alineada con código | Corregido |
| FB-16 | Pruebas unitarias solo en 1/11 servicios | Agregados 212 tests en 11 servicios siguiendo estructura de compatibility-service | `src/test/java/` en todos los servicios | `mvn test` BUILD SUCCESS en los 11 servicios | Corregido |
| FB-17 | Gateway enrutaba solo 2/10 servicios con lb:// sin Eureka | Agregado discovery-server (@EnableEurekaServer), registro en 11 servicios, gateway cambió a URLs directas | `discovery-server/`, `GatewayRoutesConfig.java`, `application.yaml` en cada servicio | Las 10 rutas del gateway funcionan con URLs directas desde variables de entorno | Corregido |
| FB-18 | GET /api/builds sin filtro por usuario (falla de seguridad) | Implementada lógica de roles ADMIN/USER: ADMIN ve todas, USER solo las propias; /user/{userId} protegido con 403 | `BuildController.java`, `BuildService.java` | Tests y verificación en Render: ADMIN=14 builds, USER=2 builds, cross-user=403 | Corregido |
| FB-19 | Feign clients declarados pero nunca usados (compatibility, provider) | Eliminado ProviderFeignClient (código muerto); CompatibilityFeignClient integrado en BuildService.triggerCompatibilityCheck() | `ProviderFeignClient.java`, `ProviderFeignClientFallbackFactory.java`, `BuildService.java` | No hay referencias a ProviderFeignClient; BuildService llama a checkCompatibility() en cada cambio de items | Corregido |

## Tareas pendientes identificadas por el equipo

| ID | Tarea | Prioridad | Estado | Justificación si no se corrige |
|----|-------|-----------|--------|-------------------------------|
| P-01 | RF-11: Análisis consolidado de build | Media | Pendiente | Cada servicio expone su endpoint individual; requiere orquestador no contemplado |
| P-02 | RF-12: Builds favoritas e historial | Media | Corregido | Implementado con FavoriteService + BuildHistoryService |
| P-03 | RF-13: Recomendaciones de mejora/upgrade | Media | Corregido | generate() recomienda componentes para slots vacíos |
| P-04 | RF-17: Alertas de precio | Baja | Pendiente | Requiere integración con APIs externas de proveedores |
| P-05 | RF-18: Notificaciones automáticas | Baja | Corregido | BuildService envía notificaciones en create/update |
| P-06 | RNF-01: Response < 500ms | Media | Pendiente | Aplica en estado estable; Render free tier añade cold starts |
| P-07 | Eureka Server (service discovery) | Alta | Corregido | discovery-server creado, @EnableDiscoveryClient en 11 servicios, registro en Eureka verificado |
| P-08 | Gateway rutas con `lb://` | Alta | Rechazado | El gateway usa URLs directas porque las rutas son de entrada única (no necesitan discovery); `lb://` se usa solo para comunicación service-to-service (Feign/RestClient) |
