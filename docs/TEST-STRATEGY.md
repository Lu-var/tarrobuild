# Estrategia de Pruebas Unitarias — TarroBuild

## Resumen

154 tests unitarios distribuidos en 10 microservicios (sin contar api-gateway). Cada servicio tiene 1 archivo de test que cubre su clase `Service` principal, más un `ApplicationTests` para validar el contexto de Spring.

## Stack de testing

- **Framework**: JUnit 5 (Spring Boot Starter Test)
- **Mocking**: Mockito (`@Mock`, `@InjectMocks`, `ArgumentCaptor`)
- **Carga de contexto**: `@SpringBootTest` en `ApplicationTests` de cada servicio

## Patrón de tests

Cada `*ServiceTest.java` sigue la misma estructura:

```
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {

    @Mock      private XxxRepository xxxRepository;
    @InjectMocks private XxxService xxxService;

    @Test
    void methodName_ExpectedBehavior() { ... }
}
```

### Capas

| Capa | Cobertura | Herramienta |
|------|-----------|-------------|
| **Service** | Tests unitarios (10 servicios) | JUnit + Mockito |
| **Context** | Application carga correctamente | `@SpringBootTest` |
| **Controller** | No cubierta (defensa: Postman) | — |

## Tests por servicio

### auth-service (12 tests)

Testea `AuthService`: registro (`createUser`), login (`authenticate`), validación JWT (`validateToken`), y manejo de errores (email duplicado → `EntityExistsException`, credenciales inválidas → `BadCredentialsException`).

Mocks: `CredentialRepository`, `UserRestClient`, `PasswordEncoder`, `JwtUtil`.

Patrón clave: `Mockito.anyString()`, `Mockito.when().thenReturn()`, `assertThrows`.

### user-service (18 tests)

Testea `UserService`: CRUD completo (create, read, update, delete, PATCH partial update), y manejo de búsquedas (por ID, email, listado).

Mocks: `UserRepository`.

Casos edge: usuario no encontrado → `EntityNotFoundException`, email duplicado → `EntityExistsException`, actualización parcial con solo algunos campos.

### product-service (20 tests)

Testea `ProductService`: CRUD de productos, filtrado por categoría/marca/rango de precio, manejo de atributos (add, update, delete, get by ID), y activación/desactivación de productos.

Mocks: `ProductRepository`, `ProductAttributeRepository`, `CategoryRestClient`.

Casos edge: producto inactivo, categoría inválida, atributo no encontrado.

### category-service (12 tests)

Testea `CategoryService`: CRUD de categorías, manejo de atributos anidados (`AttributeDefinition`), y slug único.

Mocks: `CategoryRepository`.

### compatibility-service (12 tests)

Testea `CompatibilityService` + `RuleEvaluator`: evaluación de reglas con operadores `EQUALS`, `GTE`, `CONTAINS`. Verifica compatibilidad entre componentes de una build.

Mocks: `CompatibilityRuleRepository`, `CompatibilityCheckRepository`, `ProductRestClient`.

Casos clave: regla pasa, regla falla, múltiples reglas donde una falla, categoría origen sin productos en catálogo.

### build-service (27 tests)

Testea `BuildService`: CRUD de builds e items, además de validación de productos activos vía `ProductFeignClient`.

Mocks: `BuildRepository`, `BuildItemRepository`, `ProductFeignClient`, `CompatibilityFeignClient`.

Casos clave: crear build, agregar/quitar items, validar producto activo/inactivo, producto no encontrado (Feign `NotFoundException`), trigger de compatibilidad al modificar items.

### provider-service (16 tests)

Testea `ProviderService`: CRUD de proveedores y productos de proveedor (`ProviderProduct`). Verifica relaciones `@OneToMany` entre Provider y ProviderProduct.

Mocks: `ProviderRepository`, `ProviderProductRepository`.

### estimate-service (11 tests)

Testea `EstimateService`: cálculo de estimación de costo (`calculateEstimate`), consulta por build ID, y manejo de errores (build no encontrada, producto sin precio).

Mocks: `EstimateRepository`, `BuildRestClient`, `ProductRestClient`.

### hardware-advisor-service (8 tests)

Testea `RecommendationService`: generación de recomendaciones (`generate`), consulta por build ID, y lógica de reglas de mejora.

Mocks: `RecommendationRepository`, `BuildFeignClient`, `ProductFeignClient`, `CompatibilityFeignClient`, `NotificationFeignClient`.

### notification-service (8 tests)

Testea `NotificationService`: envío de notificaciones (`sendNotification`), consulta de logs, y manejo de estados (SUCCESS, WARNING, ERROR).

Mocks: `NotificationLogRepository`, `UserRestClient`.

## Mocks externos

Cuando un servicio depende de otro (ej: build-service → product-service), se mockea el FeignClient/RestClient. Esto aísla el test de la dependencia remota.

```java
@Mock
private ProductFeignClient productFeignClient;

// En el test:
when(productFeignClient.getProductById(1L)).thenReturn(new ProductClientResponse(...));
```

## Exclusión de H2

Para evitar que `@SpringBootTest` configure H2 automáticamente, cada service POM excluye `spring-boot-starter-test` del parent y declara su propia dependencia:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Cobertura

| Servicio | Tests | ContextLoads |
|----------|-------|-------------|
| auth-service | 12 | ✅ |
| user-service | 18 | ✅ |
| product-service | 20 | ✅ |
| category-service | 12 | ✅ |
| compatibility-service | 12 | ✅ |
| provider-service | 16 | ✅ |
| build-service | 27 | ✅ |
| estimate-service | 11 | ✅ |
| hardware-advisor-service | 8 | ✅ |
| notification-service | 8 | ✅ |
| **Total** | **144** | **10/10** |

> Los 10 `ApplicationTests` (contextLoads) suman 10 tests adicionales, total 154.
