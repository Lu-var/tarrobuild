# Documentación Técnica — TarroBuild

## Stack tecnológico

| Componente | Versión |
|------------|---------|
| Java | 21 (Eclipse Temurin) |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Maven | 3.9+ |
| Base de datos | MySQL 8.0 (prod), PostgreSQL 16 (Render), H2 (dev) |
| Migraciones | Flyway |
| Seguridad | BCrypt + JWT |
| Documentación API | Springdoc OpenAPI 2.8.6 |
| Testing | JUnit 5 + Mockito |
| Contenedores | Docker + Docker Compose |
| Despliegue | Render (11 servicios individuales) |

## Arquitectura general

TarroBuild sigue una arquitectura de microservicios con API Gateway como punto de entrada único. Cada servicio es independiente con su propia base de datos, desplegable y escalable de forma autónoma.

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

Cada servicio sigue la arquitectura por capas:
```
Controller → Service → Repository → Model / DTO
```

## Microservicios y responsabilidades

| Servicio | Puerto | Responsabilidad | Base de datos |
|----------|--------|-----------------|---------------|
| api-gateway | 8080 | Enrutamiento, autenticación JWT, correlación | — |
| auth-service | 8081 | Registro, login, validación JWT, roles | db_auth |
| user-service | 8082 | CRUD de usuarios | db_users |
| product-service | 8083 | Catálogo de componentes con especificaciones | db_products |
| category-service | 8084 | Categorías y atributos técnicos | db_categories |
| compatibility-service | 8085 | Reglas de compatibilidad entre componentes | db_compatibility |
| provider-service | 8086 | Proveedores y precios de referencia | db_providers |
| build-service | 8087 | Builds personalizadas con items | db_builds |
| estimate-service | 8088 | Cálculo de costos de builds | db_estimates |
| hardware-advisor-service | 8089 | Recomendaciones de mejora/upgrade | db_recommendations |
| notification-service | 8090 | Notificaciones y alertas al usuario | db_notifications |

## Modelo de datos

Cada servicio tiene su propia base de datos con esquema independiente. Las relaciones entre servicios se resuelven vía HTTP (RestClient/FeignClient), no mediante joins entre bases de datos.

### Relaciones principales por servicio

**auth-service**: `Credentials` (id, email, password_hash, role, user_id, created_at)
**user-service**: `User` (id, email, name, last_name, phone, password_hash, role, is_active, created_at, updated_at)
**category-service**: `Category` (id, name, slug, description, is_active) → `AttributeDefinition` (id, category_id, attribute_name, value_type, is_required)
**product-service**: `Product` (id, name, brand, category_id, msrp, is_active) → `ProductAttribute` (id, product_id, attribute_name, value)
**compatibility-service**: `CompatibilityRule` (id, source_category, source_attribute_name, target_category, target_attribute_name, operator, incompatibility_reason)
**provider-service**: `Provider` (id, name, contact, phone, email, is_active) → `ProviderProduct` (id, provider_id, product_id, price, url)
**build-service**: `Build` (id, user_id, name, status, created_at) → `BuildItem` (id, build_id, product_id, quantity)
**estimate-service**: `Estimate` (id, build_id, total_cost, currency, created_at)
**hardware-advisor-service**: `Recommendation` (id, user_id, build_id, status, created_at)
**notification-service**: `Notification` (id, user_id, type, message, status, created_at)

## Perfiles de configuración

| Perfil | Uso | Base de datos | Puerto por defecto |
|--------|-----|---------------|-------------------|
| `h2` | Desarrollo local | H2 en memoria | 8080-8090 |
| `mysql` | Docker compose | MySQL 8.0 | 8080-8090 |
| `render` | Despliegue Render | PostgreSQL 16 | 8080-8090 |
| `prod` | Docker compose (producción) | MySQL 8.0 | 8080-8090 |

## Seguridad

1. **Autenticación**: JWT emitido por auth-service, validado por api-gateway
2. **Autorización**: Roles ADMIN/USER vía request matchers y @PreAuthorize
3. **Cabeceras inyectadas**: X-User-Id y X-User-Role por el gateway hacia downstream
4. **Protección de passwords**: BCrypt con Spring Security
5. **Endpoints públicos**: login, register, GET products, GET categories

## Comunicación entre servicios

14 interacciones reales entre servicios:

| Origen | Destino | Método |
|--------|---------|--------|
| api-gateway | auth-service | RestClient |
| auth-service | user-service | RestClient |
| product-service | category-service | RestClient |
| compatibility-service | product-service | RestClient |
| compatibility-service | category-service | RestClient |
| build-service | product-service | FeignClient |
| build-service | compatibility-service | FeignClient |
| estimate-service | build-service | RestClient |
| estimate-service | product-service | RestClient |
| estimate-service | notification-service | RestClient |
| notification-service | user-service | RestClient |
| hardware-advisor-service | product-service | FeignClient |
| hardware-advisor-service | notification-service | FeignClient |
| hardware-advisor-service | build-service | FeignClient |
| hardware-advisor-service | compatibility-service | FeignClient |

Todas las comunicaciones tienen timeouts configurables (5s connect, 10s read) y los FeignClients tienen FallbackFactory implementado.

## Manejo de errores

Formato uniforme de respuesta:
```json
{
  "timestamp": "2026-07-11T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Recurso no encontrado con id 15",
  "path": "/api/recursos/15"
}
```

Códigos HTTP implementados: 200, 201, 204, 400, 401, 403, 404, 405, 409, 500, 503.

## Logging

SLF4J + Logback con configuración por servicio:
- Nivel INFO para cl.tarrobuild
- Nivel WARN para org.hibernate.SQL
- Rolling policy: archivos de 10MB, historial de 10 días
- Archivos en `logs/{service-name}.log`

## Pruebas

204 tests unitarios distribuidos en 11 servicios:
- JUnit 5 + Mockito
- Servicios mockeados con @Mock + @InjectMocks
- Patrón Given-When-Then
- @SpringBootTest contextLoads en todos los servicios

Ver `docs/TEST-STRATEGY.md` para el detalle completo por servicio.

## OpenAPI / Swagger

Springdoc OpenAPI 2.8.6 en los 11 servicios. URLs en Render:
- `https://api-gateway-tzkw.onrender.com/swagger-ui/index.html`
- `https://auth-service-oww8.onrender.com/swagger-ui/index.html`
- (Swagger UI disponible en todos los servicios)

Ver `docs/API-DOCS.md` para la lista completa.

## Estructura del repositorio

```
tarrobuild/
├── README.md
├── .env.example
├── pom.xml
├── compose.yml
├── render.yaml
├── Dockerfile.render
├── docs/
│   ├── ARCHITECTURE.md
│   ├── PROGRESS.md
│   ├── DONE.md
│   ├── matriz-requerimientos.md
│   ├── plan-cierre-feedback.md
│   ├── documentacion-funcional.md
│   ├── documentacion-tecnica.md
│   ├── levantamiento-requerimientos-actualizado.md
│   ├── API-DOCS.md
│   ├── TEST-STRATEGY.md
│   ├── presentacion-defensa-grupal.md
│   └── defensa-individual/
│       ├── apellido-nombre.md
│       └── ...
├── postman/
│   ├── local.postman_environment.json
│   ├── render.postman_environment.json
│   └── collections/
│       ├── Auth Service/
│       ├── Build Service/
│       ├── Category Service/
│       ├── Compatibility Service/
│       ├── Estimate Service/
│       ├── Hardware Advisor Service/
│       ├── Notification Service/
│       ├── Product Service/
│       ├── Provider Service/
│       └── User Service/
├── scripts/
│   ├── gateway-endpoint-test-v2.ps1
│   ├── full-integration-test.ps1
│   ├── curl-integration-tests.ps1
│   ├── build-service-endpoint-test.ps1
│   ├── notification-service-test.ps1
│   ├── gateway-endpoint-test.ps1
│   └── product-service-endpoint-test.ps1
└── services/
    ├── api-gateway/
    ├── auth-service/
    ├── build-service/
    ├── category-service/
    ├── compatibility-service/
    ├── estimate-service/
    ├── hardware-advisor-service/
    ├── notification-service/
    ├── product-service/
    ├── provider-service/
    └── user-service/
```

## Ejecución desde cero

### Requisitos previos
- Java 21 (Eclipse Temurin)
- Maven 3.9+ (incluye `mvnw.cmd` en cada servicio)
- Docker Desktop (para compose)
- Git

### Windows (PowerShell)

```powershell
# 1. Clonar
git clone <url-del-repositorio>
cd tarrobuild

# 2. Configurar perfiles (opcional, default h2)
$env:SPRING_PROFILES_ACTIVE="h2"

# 3. Compilar todo el proyecto (desde la raíz)
mvnw.cmd clean compile -DskipTests

# 4. Ejecutar un servicio específico
cd services\api-gateway
mvnw.cmd spring-boot:run

# 5. Ejecutar pruebas de un servicio
mvnw.cmd test

# 6. Ejecutar todas las pruebas del proyecto (desde la raíz)
cd ..\..
mvnw.cmd test

# 7. Docker Compose (producción con MySQL)
docker compose up --build

# 8. Ver logs de un servicio en Docker
docker compose logs -f api-gateway
```

### Linux/macOS

```bash
# 1. Clonar
git clone <url-del-repositorio>
cd tarrobuild

# 2. Compilar
./mvnw clean compile -DskipTests

# 3. Ejecutar servicio
cd services/api-gateway
./mvnw spring-boot:run

# 4. Pruebas
./mvnw test

# 5. Docker Compose
docker compose up --build
```

### Orden de arranque (local sin Docker)
1. Ejecutar cada servicio individualmente con `mvnw.cmd spring-boot:run` (perfil h2)
2. Los servicios no tienen dependencias de bases de datos externas en perfil h2
3. api-gateway debe iniciarse después de auth-service si se usan endpoints protegidos

### Orden de arranque (Docker Compose)
```bash
docker compose up --build
```
Esto levanta automáticamente: 10 MySQL → 11 servicios Java, con healthchecks y dependencias.

### URLs de Swagger UI (local)
- `http://localhost:{puerto}/swagger-ui/index.html`

### Verificar que el sistema funciona
```bash
# Gateway
curl http://localhost:8080/api/v1/auth/login -X POST -H "Content-Type: application/json" -d "{\"email\":\"admin@tarrobuild.cl\",\"password\":\"admin123\"}"

# Productos (público)
curl http://localhost:8080/api/v1/products
```

## Despliegue en Render

Cada microservicio se despliega como un servicio web independiente en Render. El despliegue usa el perfil `render` con PostgreSQL. Se utiliza `render.yaml` como blueprint para desplegar los 11 servicios.

URLs públicas de servicios:

| Servicio | URL |
|----------|-----|
| api-gateway | https://api-gateway-tzkw.onrender.com |
| auth-service | https://auth-service-oww8.onrender.com |
| user-service | https://user-service-1ycx.onrender.com |
| product-service | https://product-service-e903.onrender.com |
| category-service | https://category-service-91sc.onrender.com |
| compatibility-service | https://compatibility-service-3hfz.onrender.com |
| provider-service | https://provider-service-56hc.onrender.com |
| build-service | https://build-service-lym5.onrender.com |
| estimate-service | https://estimate-service-h0h9.onrender.com |
| hardware-advisor-service | https://hardware-advisor-service-9b8i.onrender.com |
| notification-service | https://notification-service-d4ww.onrender.com |

### Configuración por servicio en Render

Cada servicio requiere:
- **Build command**: `mvn clean compile -pl services/<nombre> -am -DskipTests`
- **Start command**: `mvn spring-boot:run -pl services/<nombre>`
- **Runtime**: Java 21 (Eclipse Temurin)
- **Plan**: Free
- **Branch**: refactor/prod-profiles (o la rama activa)

### Limitaciones del despliegue gratuito
- Cold start: ~5-10s en el primer request después de inactividad
- Los servicios se duermen tras 15 minutos sin uso
- No hay service discovery (Eureka) en el despliegue actual
