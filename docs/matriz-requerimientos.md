# Matriz de Requerimientos — TarroBuild

## Requerimientos Funcionales

| ID | Requerimiento | Tipo | Estado | Endpoint o evidencia | Prueba asociada |
|----|--------------|------|--------|---------------------|-----------------|
| RF-01 | Registrar usuario con nombre, correo y contraseña encriptada (BCrypt) | Funcional | Implementado | `POST /api/auth/register` → `POST /api/v1/auth/register` | `AuthServiceTest.crearUsuario_exitoso` |
| RF-02 | Autenticar usuario mediante correo y contraseña, retornando JWT | Funcional | Implementado | `POST /api/auth/login` → `POST /api/v1/auth/login` | `AuthServiceTest.login_exitoso` |
| RF-03 | Listar componentes del catálogo con categoría y especificaciones | Funcional | Implementado | `GET /api/products` → `GET /api/v1/products` | `ProductServiceTest.listarProductos_retornaTodos` |
| RF-04 | Obtener detalle técnico completo de un componente | Funcional | Implementado | `GET /api/products/{id}` → `GET /api/v1/products/{id}` | `ProductServiceTest.obtenerProducto_porId_retornaProducto` |
| RF-05 | Filtrar componentes por categoría, marca y rango de precio | Funcional | Implementado | `GET /api/products?category=&brand=&minPrice=&maxPrice=` | `ProductServiceTest.filtrarProductos_porCategoria` |
| RF-06 | Crear build personalizada asociada a un usuario autenticado | Funcional | Implementado | `POST /api/builds` → `POST /api/v1/builds` | `BuildServiceTest.crearBuild_exitoso` |
| RF-07 | Agregar, actualizar o eliminar componentes dentro de una build activa | Funcional | Implementado | `POST/PUT/DELETE /api/builds/{id}/items` | `BuildServiceTest.crearItem_exitoso`, `BuildServiceTest.actualizarItem_exitoso`, `BuildServiceTest.eliminarItem_exitoso` |
| RF-08 | Verificar compatibilidad entre todos los componentes de una build | Funcional | Implementado | `POST /api/compatibility/check` → `POST /api/v1/compatibility/check` | `CompatibilityServiceTest.check_MultipleRules_AllPass_ReturnsTrue` |
| RF-09 | Calcular costo total de una build y validar consumo energético | Funcional | Implementado | `POST /api/estimate` → `POST /api/v1/estimate` | `EstimateServiceTest.calcular_estimate_exitoso` |
| RF-10 | Obtener referencias de vendedores registrados para componentes | Funcional | Implementado | `GET /api/providers` → `GET /api/v1/providers` | `ProviderServiceTest.listarProveedores_retornaTodos` |
| RF-11 | Generar análisis consolidado de build (compatibilidad + costo + advertencias) | Funcional | No implementado | — | — |
| RF-12 | Guardar builds favoritas e historial de configuraciones | Funcional | No implementado | — | — |
| RF-13 | Generar recomendaciones de mejora o upgrade | Funcional | No implementado | — | — |
| RF-14 | Crear/editar componentes y atributos técnicos del catálogo | Funcional | Implementado | `POST/PUT /api/products`, `POST/PUT /api/categories` | `ProductServiceTest.crearProducto_exitoso`, `CategoryServiceTest.crearCategoria_exitoso` |
| RF-15 | Crear/editar reglas de compatibilidad entre categorías | Funcional | Implementado | `POST/PUT /api/compatibility/rules` | `CompatibilityServiceTest.check_MultipleRules_AllPass_ReturnsTrue` |
| RF-16 | Registrar/actualizar referencias de precios de mercado | Funcional | Parcial | `PUT /api/providers/products/{id}` | `ProviderServiceTest.actualizarProductoProveedor_exitoso` |
| RF-17 | Configurar alertas para cambios de precio o disponibilidad | Funcional | No implementado | — | — |
| RF-18 | Enviar notificaciones automáticas al usuario sobre alertas en builds | Funcional | Parcial | `POST /api/notifications/send` | `NotificationServiceTest.enviar_notificacion_exitoso` |

## Requerimientos No Funcionales

| ID | Requerimiento | Tipo | Estado | Evidencia |
|----|--------------|------|--------|-----------|
| RNF-01 | Las respuestas de la API deben responder en menos de 500ms | No funcional | No implementado | Pendiente de pruebas de performance |
| RNF-02 | Los microservicios deben usar Feign Client y RestClient para comunicación HTTP | No funcional | Implementado | 6 FeignClients + 9 RestClients en el proyecto |
| RNF-03 | El sistema debe incluir un mecanismo de logout para invalidar tokens JWT | No funcional | Implementado | `POST /api/auth/logout` |
| RNF-04 | No exponer credenciales en GitHub | No funcional | Implementado | `.env.example` por servicio, variables de entorno |
| RNF-05 | El sistema debe incluir logging con SLF4J y Logback en todos los servicios | No funcional | Implementado | Configuración logging en todos los servicios |
| RNF-06 | Cada microservicio debe tener su propia base de datos | No funcional | Implementado | 10 bases de datos independientes (MySQL/PostgreSQL/H2) |
