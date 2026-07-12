# Levantamiento de Requerimientos Actualizado — TarroBuild

## Requerimientos originales y cambios aplicados durante el semestre

| ID | Requerimiento original | Cambio realizado | Justificación | Estado final | Evidencia en repositorio |
|----|----------------------|------------------|---------------|--------------|--------------------------|
| RF-01 | Registrar usuario | Sin cambios | — | Implementado | `AuthService.java`, `AuthServiceTest.java` |
| RF-02 | Autenticar usuario con JWT | Sin cambios | — | Implementado | `AuthService.java`, `AuthServiceTest.java` |
| RF-03 | Listar catálogo | Sin cambios | — | Implementado | `ProductController.java`, `ProductServiceTest.java` |
| RF-04 | Detalle de componente | Sin cambios | — | Implementado | `ProductController.java`, `ProductServiceTest.java` |
| RF-05 | Filtrar componentes | Sin cambios | — | Implementado | `ProductController.java`, `ProductServiceTest.java` |
| RF-06 | Crear build personalizada | Sin cambios | — | Implementado | `BuildController.java`, `BuildServiceTest.java` |
| RF-07 | Gestionar items de build | Sin cambios | — | Implementado | `BuildController.java`, `BuildServiceTest.java` |
| RF-08 | Verificar compatibilidad | Sin cambios | — | Implementado | `CompatibilityService.java`, `CompatibilityServiceTest.java` |
| RF-09 | Calcular costo + validar consumo | Unificado en estimate-service | Originalmente eran dos servicios separados; se unificó para simplificar | Implementado | `EstimateService.java`, `EstimateServiceTest.java` |
| RF-10 | Referencias de vendedores | Sin cambios | — | Implementado | `ProviderController.java`, `ProviderServiceTest.java` |
| RF-11 | Análisis consolidado de build | Sin cambios | — | No implementado | Pendiente de implementación |
| RF-12 | Builds favoritas e historial | Sin cambios | — | No implementado | Pendiente de implementación |
| RF-13 | Recomendaciones de mejora | Sin cambios | — | No implementado | hardware-advisor-service tiene estructura base |
| RF-14 | CRUD componentes y atributos | Sin cambios | — | Implementado | `ProductController.java`, `CategoryController.java` |
| RF-15 | Reglas de compatibilidad | Sin cambios | — | Implementado | `CompatibilityController.java`, seed data V2 |
| RF-16 | Precios de mercado | Sin cambios | — | Parcial | ProviderProduct soporta precios, falta integración |
| RF-17 | Alertas de precio | Sin cambios | — | No implementado | Pendiente de implementación |
| RF-18 | Notificaciones automáticas | Sin cambios | — | Parcial | NotificationService implementado, faltan integraciones |
| RNF-01 | Response < 500ms | Sin cambios | — | No implementado | Pendiente de pruebas de performance |
| RNF-02 | Feign + RestClient | Sin cambios | — | Implementado | 6 FeignClients + 9 RestClients en el proyecto |
| RNF-03 | Logout de JWT | Sin cambios | — | Implementado | `AuthController.logout()` |
| RNF-04 | No exponer credenciales | Sin cambios | — | Implementado | `.env.example` por servicio, variables de entorno |
| RNF-05 | Logging SLF4J + Logback | Sin cambios | — | Implementado | Configuración en todos los servicios |
| RNF-06 | BD independiente por servicio | Sin cambios | — | Implementado | 10 bases de datos separadas |

## Mejoras incorporadas durante el semestre

| Mejora | Descripción | Archivos |
|--------|-------------|----------|
| Roles ADMIN/USER en build-service | ADMIN ve todas las builds, USER solo las propias | `BuildController.java`, `BuildService.java` |
| Filtro de correlación | Trazabilidad de requests entre servicios | `CorrelationIdFilter.java` en 11 servicios |
| Springdoc/OpenAPI | Documentación Swagger en todos los servicios | `pom.xml`, `OpenApiConfig.java` en cada servicio |
| Unit tests | 204 tests en 11 servicios | `*ServiceTest.java` en cada servicio |
| Docker compose | compose.yml con healthchecks y dependencias | `compose.yml`, `application-prod.yaml` |
| Postman collections | 210 requests en 10 colecciones | `postman/collections/` |
| Timeouts configurables | RestClient y Feign con timeouts desde YAML | `application.yaml` en servicios con cliente |
| Eureka service discovery | discovery-server (@EnableEurekaServer) + registro de 11 servicios (@EnableDiscoveryClient), comunicación inter-service con `lb://` | `discovery-server/`, `pom.xml`, `application.yaml` en cada servicio |
| Gateway con URLs directas | Gateway redirecciona a servicios via URLs directas desde variables de entorno, no usa `lb://` por limitación de Render (contenedores aislados) | `GatewayRoutesConfig.java`, `compose.yml`, `render.yaml` |
| Timeouts Render 30s/90s | Timeouts aumentados para cold starts de Render free tier (>50s) | `application-render.yaml` en 8 servicios |
| Postman reestructurado | Colecciones organizadas en `Direct/`, `Gateway/`, `Monolith/` con requests de nombre limpio | `postman/collections/` |

## Requerimientos eliminados o reemplazados

| ID | Requerimiento | Acción | Justificación |
|----|--------------|--------|---------------|
| — | ProviderFeignClient en provider-service | Eliminado | provider-service es un módulo CRUD aislado sin necesidad de comunicación saliente |
| — | gateway-reactive (WebFlux) | Reemplazado | Se usa Spring Cloud Gateway MVC (servlet) en vez de reactive, por compatibilidad con el stack del curso |

## Trazabilidad hacia código

Cada requerimiento implementado tiene:
- Endpoint REST verificable via Postman
- Prueba unitaria en la capa service
- Documentación Swagger/OpenAPI
- Logs de operaciones relevantes

Ver `docs/matriz-requerimientos.md` para la trazabilidad completa de RF a pruebas unitarias.
