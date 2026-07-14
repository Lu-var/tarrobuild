# Presentación de Defensa Técnica Grupal — TarroBuild

## 1. Datos del proyecto

- **Nombre del proyecto:** TarroBuild
- **Asignatura:** DSY1103 — Desarrollo Fullstack I Backend
- **Tecnologías:** Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1, Maven, MySQL, PostgreSQL, Docker, Render

## 2. Problema abordado

Los usuarios que desean armar o actualizar un computador no disponen de un sistema centralizado que permita validar automáticamente la compatibilidad técnica entre componentes, estimar requerimientos energéticos, comparar precios de referencia y recibir recomendaciones de mejora antes de tomar una decisión de compra.

## 3. Solución propuesta

Sistema backend de microservicios que permite:
- Navegar un catálogo de componentes con especificaciones técnicas
- Crear builds personalizadas con validación automática de compatibilidad
- Estimar costos totales de configuración
- Consultar precios de referencia de proveedores
- Recibir recomendaciones de mejora

## 4. Alcance final del sistema

11 microservicios desplegados de forma independiente:
- **API Gateway** (puerto 8080): punto de entrada único con autenticación JWT
- **auth-service** (8081): registro, login, roles
- **user-service** (8082): gestión de usuarios
- **product-service** (8083): catálogo de componentes
- **category-service** (8084): categorías y atributos técnicos
- **compatibility-service** (8085): reglas de compatibilidad
- **provider-service** (8086): proveedores y precios
- **build-service** (8087): builds personalizadas
- **estimate-service** (8088): cálculo de costos
- **hardware-advisor-service** (8089): recomendaciones
- **notification-service** (8090): notificaciones y alertas

## 5. Principales requerimientos cumplidos

- RF-01 a RF-10: Core funcional completo (gestión de usuarios, catálogo, builds, compatibilidad, estimación, proveedores)
- RF-14 a RF-15: Administración de catálogo y reglas
- RF-16: Precios de referencia (parcial)
- RF-18: Notificaciones automáticas (implementado)

## 6. Feedback y correcciones aplicadas

Ver `docs/plan-cierre-feedback.md` para el detalle completo (15 items corregidos).

Resumen:
- Auth-service: tipo de excepción corregido
- Estimate-service: moneda default USD
- Gateway: rutas públicas deduplicadas
- Notification-service: handler duplicado eliminado
- ProviderFeignClient: código muerto eliminado
- Filtro de correlación agregado en 10 servicios
- @PreAuthorize en auth-service
- Build-service: trigger de compatibilidad
- Application-prod.yaml para Docker compose


- 204 unit tests en 11 servicios
- Springdoc/OpenAPI en 11 servicios
- Roles ADMIN/USER en build-service
- Docker compose funcional
- Documentación técnica completa

## 7. Arquitectura general

```
Cliente → API Gateway (:8080)
              ├── auth-service (:8081)
              ├── user-service (:8082)
              ├── product-service (:8083)
              ├── category-service (:8084)
              ├── compatibility-service (:8085)
              ├── provider-service (:8086)
              ├── build-service (:8087)
              ├── estimate-service (:8088)
              ├── hardware-advisor-service (:8089)
              └── notification-service (:8090)
```

Cada servicio: Controller → Service → Repository → Model/DTO
Base de datos independiente por servicio (MySQL/PostgreSQL/H2)

## 8. Modelo de datos resumido

- **auth-service**: Credentials (email, password_hash, role)
- **user-service**: User (email, name, role, is_active)
- **product-service**: Product → ProductAttribute
- **category-service**: Category → AttributeDefinition
- **compatibility-service**: CompatibilityRule (reglas con 3 operadores)
- **build-service**: Build → BuildItem
- **estimate-service**: Estimate (total_cost, currency)
- **provider-service**: Provider → ProviderProduct
- **notification-service**: Notification (type, status)
- **hardware-advisor-service**: Recommendation (user_id, build_id)

## 9. Seguridad y roles

- JWT con BCrypt
- Roles: ADMIN y USER
- Gateway valida tokens, inyecta X-User-Id y X-User-Role
- Endpoints públicos: login, register, GET products, GET categories
- @PreAuthorize en auth-service
- 401 para token ausente/inválido, 403 para rol insuficiente

## 10. Pruebas realizadas

- 204 unit tests (JUnit 5 + Mockito) en 11 servicios
- Patrón Given-When-Then
- Mocks para dependencias externas
- @SpringBootTest contextLoads en todos los servicios
- 210 requests Postman (10 colecciones)
- 8 scripts de prueba PowerShell

## 11. Swagger/OpenAPI

Springdoc 2.8.6 en los 11 servicios con @Tag, @Operation, @ApiResponse.
Swagger UI disponible en cada servicio.

## 12. Despliegue local y remoto

### Local
- Perfil h2: `mvnw.cmd spring-boot:run` en cada servicio
- Docker Compose: `docker compose up --build`

### Remoto (Render)
Los 11 servicios desplegados como servicios web independientes con PostgreSQL.

URLs:
- Gateway: https://api-gateway-tzkw.onrender.com
- auth-service: https://auth-service-oww8.onrender.com
- user-service: https://user-service-yuz5.onrender.com
- product-service: https://product-service-e903.onrender.com
- category-service: https://category-service-4fmr.onrender.com
- compatibility-service: https://compatibility-service.onrender.com
- provider-service: https://provider-service-9ip0.onrender.com
- build-service: https://build-service-lym5.onrender.com
- estimate-service: https://estimate-service-dkhv.onrender.com
- hardware-advisor-service: https://hardware-advisor-service.onrender.com
- notification-service: https://notification-service-e4ij.onrender.com

## 13. Principales dificultades técnicas

| Dificultad | Solución |
|------------|----------|
| Springdoc incompatible con Spring Boot 4.x | Fijar versión 2.8.6 en dependencyManagement |
| build-service sin trigger de compatibilidad | Agregar FeignClient + lógica en BuildService |

| Auth circular dependency en tests | Separar @Bean BCryptPasswordEncoder |
| Error de moneda en estimate-service | Cambiar default a USD |
| Rutas públicas duplicadas en gateway | Crear clase compartida PublicPaths |
| Render cold starts (free tier) | Documentado como limitación conocida |

## 14. Distribución de trabajo

- Lucas Vargas
- Camila Corocedo

Ver `docs/defensa-individual/` para los documentos individuales de cada integrante.
