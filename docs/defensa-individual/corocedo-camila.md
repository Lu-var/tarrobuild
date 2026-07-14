# Documento de Defensa Técnica Individual

## Datos del estudiante

- **Nombre:** Camila Corocedo
- **Rol dentro del equipo:** Desarrollo backend — implementación de servicios, integración, pruebas y documentación
- **Asignatura:** DSY1103 — Desarrollo Fullstack I Backend
- **Proyecto:** TarroBuild

## Funcionalidades o módulos en los que participó

- Category-service: CRUD completo de categorías y atributos técnicos
- Compatibility-service: pruebas unitarias, Swagger/OpenAPI, configuración de dependencias
- Provider-service: relaciones JPA @ManyToOne/@OneToMany entre Provider y ProviderProduct
- Seguridad: correlation ID en todos los servicios, @PreAuthorize en auth-service
- Documentación: Swagger/OpenAPI en los 11 servicios (OpenApiConfig)
- Corrección de bugs: auth-service, estimate-service, notification-service, gateway
- Integración Eureka: gateway routes con lb://, FeignClients y RestClients con lb://
- Docker: compose.yml healthchecks, entrypoint.sh CRLF→LF
- Funcionalidades: builds favoritas e historial (RF-12), notificaciones automáticas (RF-18)

## Commits propios más relevantes

| Hash | Descripción | Archivos |
|------|-------------|----------|
| `fadc380` | feat: complete category-service implementation | category-service/ |
| `810d0e6` | feat: implementar documentacion con swagger en compatibility-service | compatibility-service/ |
| `f1708ea` | feat: configurar rutas de enrutamiento en api-gateway | api-gateway/ |
| `8972cff` | Fix/notification exceptions (#50) | notification-service/ |
| `ec6ff06` | fix: tipo de excepción auth-service, moneda USD, rutas gateway | auth, estimate, api-gateway |
| `4123df2` | feat: relaciones JPA @ManyToOne/@OneToMany en provider-service | provider-service/ |
| `38fcca9` | feat: correlation ID en todos los servicios, @PreAuthorize | 11 servicios |
| `3e51e2d` | fix: elimina handler obsoleto y ProviderFeignClient no usado | notification, provider |
| `15482b6` | feat: documentación Swagger/OpenAPI en los 11 servicios | pom.xml, OpenApiConfig |
| `87e42c3` | fix: compose.yml healthchecks, entrypoint.sh CRLF→LF | compose.yml, entrypoint.sh |
| `3718d88` | feat: gateway routes con lb:// service discovery | GatewayRoutesConfig |
| `d89ceb2` | feat: FeignClients con lb:// service discovery | 6 Feign clients |
| `a6cb454` | feat: RestClients con lb:// service discovery | 9 Rest clients |
| `5522ca1` | feat: builds favoritas e historial | FavoriteService, BuildHistoryService |
| `157f448` | feat: script para medir performance | scripts/performance-test.ps1 |

## Tareas del tablero asociadas a su trabajo

No se utilizó Trello. El progreso se registró en `docs/PROGRESS.md` y `docs/DONE.md`. La organización fue vía GitHub con ramas por feature y pull requests.

## Feedback o pendiente que corrigió personalmente

- Notification-service: excepciones y handler duplicado corregidos
- Auth-service: tipo de excepción incorrecta cambiada
- Estimate-service: moneda por defecto cambiada a USD
- Gateway: rutas públicas duplicadas corregidas
- ProviderFeignClient: código muerto eliminado
- Compatibility-service: POMs rotos y dependencias corregidas
- Entrypoint.sh: CRLF→LF para compatibilidad con Docker

## Archivos principales que modificó

- `services/category-service/` — controller, service, repository, DTOs completos
- `services/compatibility-service/` — tests, Swagger config, POM fixes
- `services/provider-service/` — relaciones JPA, ProviderProduct
- `services/api-gateway/` — rutas de enrutamiento
- `services/notification-service/` — excepciones, handler
- `services/auth-service/` — tipo de excepción, @PreAuthorize
- `services/estimate-service/` — moneda por defecto
- Todos los servicios: OpenApiConfig, CorrelationIdFilter, application.yaml
- `compose.yml`, `.gitattributes`, entrypoint.sh
- `scripts/performance-test.ps1`

## Endpoints o flujos asociados a su aporte

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET/POST | `/api/categories` | CRUD categorías |
| GET/POST/PUT/DELETE | `/api/categories/{id}/attributes` | Atributos técnicos por categoría |
| GET/POST/PUT/DELETE | `/api/compatibility/rules` | Reglas de compatibilidad |
| GET | `/api/compatibility/check/{buildId}` | Último chequeo por build |
| GET/POST/PUT/DELETE | `/api/providers` | CRUD proveedores |
| POST | `/api/builds/{id}/favorite` | Marcado de favorito |
| GET | `/api/builds/favorites` | Lista de favoritos |
| Gateway | `/api/v1/**` | Proxy a servicios |

## Pruebas unitarias o REST asociadas a su aporte

- `CategoryServiceTest` (12 tests)
- `CompatibilityServiceTest` (12 tests, tests de reglas y compatibilidad)
- `ProviderServiceTest` (16 tests)
- `BuildServiceTest` (tests de favoritos e historial)
- Test de performance: `scripts/performance-test.ps1`

## Regla de negocio que domina

CRUD de categorías con atributos técnicos. Cada categoría (CPU, GPU, RAM, etc.) tiene atributos obligatorios y opcionales que definen las especificaciones técnicas de los productos. Por ejemplo, la categoría "CPU" tiene atributos como "Socket" (obligatorio), "Cores" (obligatorio), "TDP" (opcional). La validación asegura que todos los atributos obligatorios estén presentes al crear un producto.

Implementado en `CategoryService.java` y probado en `CategoryServiceTest`.

## Relación de base de datos que domina

Relación `@ManyToOne` entre Provider y ProviderProduct.

Un proveedor puede tener múltiples productos asociados (precios, SKU, disponibilidad). La relación se modela con:

```java
// ProviderProduct.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "provider_id")
private Provider provider;
```

- `FetchType.LAZY` evita cargar el proveedor si no se necesita
- La FK `provider_id` relaciona cada producto con su proveedor
- `CascadeType.ALL` en `Provider.java` propaga operaciones de persistencia

Implementado en `provider-service/` commit `4123df2`.

## Comunicación entre servicios que domina

Flujo de gateway routes con lb:// via Eureka.

El gateway define rutas que redirigen requests a los 11 microservicios usando `lb://` cuando Eureka está disponible. Por ejemplo, `/api/recommendations/**` se redirige a `lb://hardware-advisor-service`. En Render las rutas usan URLs directas por el aislamiento de contenedores.

Implementado en `GatewayRoutesConfig.java` commit `3718d88`.

## Dificultad técnica personal y cómo la resolvió

Diseñar el modelo de atributos dinámicos para las categorías. Cada categoría necesita atributos distintos: una CPU tiene "Socket" y "Cores", una GPU tiene "VRAM" y "Core Clock". Un modelo fijo con columnas para cada atributo no funcionaba porque cada categoría tiene los suyos.

La solución fue separar en dos tablas: `categories` con datos fijos, y `attribute_definitions` donde cada fila define un atributo por categoría (nombre, tipo de valor, si es obligatorio). Al crear un producto, el sistema sabe qué atributos pedir según su categoría. Se pueden agregar nuevas categorías sin cambiar el esquema de base de datos.

Implementado en `category-service` con relación `@OneToMany` entre `Category` y `AttributeDefinition`. Test: `CategoryServiceTest`.

## Checklist personal de evidencia entregada

- [x] Commits con participación verificable
- [x] Archivos modificados documentados
- [x] Pruebas asociadas al aporte
- [x] Endpoints asociados al aporte
- [x] Regla de negocio explicada
- [x] Relación de BD explicada
- [x] Comunicación entre servicios explicada
