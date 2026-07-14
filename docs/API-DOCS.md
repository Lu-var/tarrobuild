# Documentación Swagger/OpenAPI — TarroBuild

## Resumen

Los 11 microservicios tienen documentación OpenAPI 3.0 generada con Springdoc OpenAPI Starter WebMVC UI (v2.8.6). Cada servicio expone:

- `/v3/api-docs` → JSON OpenAPI spec
- `/swagger-ui/index.html` → Swagger UI interactiva

## Stack

| Componente | Versión |
|------------|---------|
| springdoc-openapi-starter-webmvc-ui | 2.8.6 |
| OpenAPI | 3.0 |
| Swagger UI | (incluido en springdoc) |

La versión 2.8.6 es compatible con Spring Boot 4.0.6 / Spring Framework 7. Versiones anteriores (2.5.x) causaban `NoSuchMethodError` por incompatibilidad binaria con `ControllerAdviceBean`.

## URLs por servicio (Render)

| Servicio | URL Swagger UI |
|----------|---------------|
| api-gateway | `https://api-gateway-tzkw.onrender.com/swagger-ui/index.html` |
| auth-service | `https://auth-service-oww8.onrender.com/swagger-ui/index.html` |
| user-service | `https://user-service-yuz5.onrender.com/swagger-ui/index.html` |
| product-service | `https://product-service-e903.onrender.com/swagger-ui/index.html` |
| category-service | `https://category-service-4fmr.onrender.com/swagger-ui/index.html` |
| compatibility-service | `https://compatibility-service.onrender.com/swagger-ui/index.html` |
| provider-service | `https://provider-service-9ip0.onrender.com/swagger-ui/index.html` |
| build-service | `https://build-service-lym5.onrender.com/swagger-ui/index.html` |
| estimate-service | `https://estimate-service-dkhv.onrender.com/swagger-ui/index.html` |
| hardware-advisor-service | `https://hardware-advisor-service.onrender.com/swagger-ui/index.html` |
| notification-service | `https://notification-service-e4ij.onrender.com/swagger-ui/index.html` |

> Local: `http://localhost:{puerto}/swagger-ui/index.html`

## Anotaciones usadas

Cada controller usa `@Tag` para agrupar endpoints, `@Operation` para describir cada endpoint, y `@ApiResponse` para documentar códigos HTTP de error:

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gestión de usuarios")
public class UserController {

    @GetMapping
    @Operation(summary = "Listar todos los usuarios")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    public ResponseEntity<List<UserResponse>> getUsers() { ... }
}
```

## Modelos documentados

Springdoc documenta automáticamente los DTOs (Java records) usados como `@RequestBody` y en el tipo de retorno de `ResponseEntity<T>`:

| Servicio | Request DTOs | Response DTOs |
|----------|-------------|---------------|
| auth-service | AuthRequest | AuthResponse |
| user-service | CreateUserRequest, UpdateUserRequest | UserResponse |
| product-service | ProductRequest, ProductAttributeRequest | ProductResponse, ProductAttributeResponse |
| category-service | CategoryRequest, AttributeDefinitionRequest | CategoryResponse, AttributeDefinitionResponse |
| compatibility-service | CompatibilityRequest, CompatibilityRuleRequest | CompatibilityResponse, CompatibilityRuleResponse |
| provider-service | ProviderRequest, ProviderProductRequest | ProviderResponse, ProviderProductResponse |
| build-service | BuildRequest, BuildItemRequest, BuildStatusRequest | BuildResponse, BuildItemResponse |
| estimate-service | EstimateRequest | EstimateResponse |
| hardware-advisor-service | RecommendationRequest | RecommendationResponse |
| notification-service | NotificationRequest | NotificationResponse |

## Excepciones documentadas

Los `GlobalExceptionHandler` con `@ControllerAdvice` están excluidos de la generación OpenAPI mediante `@Hidden` para evitar el error `NoSuchMethodError: ControllerAdviceBean.<init>(Object)` que ocurría con springdoc 2.x en Spring Boot 4.x.

## Seguridad

- `auth-service` requiere autenticación para acceder a Swagger UI (protegido por Spring Security)
- Los demás servicios tienen Swagger UI público
- Los endpoints protegidos (requieren JWT) se documentan pero requieren el header `Authorization: Bearer {token}` para ejecutarse desde Swagger UI

## Consistencia código-documentación

Springdoc genera la spec OpenAPI a partir de las anotaciones del código (`@RestController`, `@RequestMapping`, `@RequestBody`, `@PathVariable`). No hay spec desincronizada porque no hay spec escrita a mano.

## Verificación

Cada servicio fue verificado en Render contra el endpoint `/v3/api-docs`. Los 11 servicios retornan OpenAPI JSON válido. Los Swagger UI cargan correctamente.
