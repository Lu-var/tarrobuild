# Documentación Funcional — TarroBuild

## Problema que resuelve

Los usuarios que desean armar o actualizar un computador no disponen de un sistema centralizado que permita validar automáticamente la compatibilidad técnica entre componentes, estimar requerimientos energéticos, comparar precios de referencia y recibir recomendaciones de mejora antes de tomar una decisión de compra. Actualmente este proceso requiere investigación manual en múltiples sitios, conocimiento técnico especializado y verificación cruzada de especificaciones.

## Actores o perfiles

| Actor | Descripción |
|-------|-------------|
| Usuario no registrado | Puede navegar el catálogo de componentes, consultar compatibilidad y estimar costos |
| Usuario registrado | Puede crear y gestionar builds personalizadas, guardar favoritos y recibir recomendaciones |
| Administrador | Puede gestionar el catálogo de componentes, reglas de compatibilidad y proveedores |
| Sistema | Ejecuta procesos automáticos de validación, notificaciones y alertas |

## Requerimientos funcionales

Ver `docs/matriz-requerimientos.md` para la matriz completa con trazabilidad a código y pruebas.

### Resumen por área

**Gestión de usuarios y autenticación (RF-01, RF-02)**
- Registro de usuarios con validación de email único y contraseña encriptada
- Inicio de sesión con JWT
- Cierre de sesión

**Catálogo de componentes (RF-03, RF-04, RF-05, RF-14)**
- CRUD completo de componentes con especificaciones técnicas
- Filtros por categoría, marca y rango de precio
- Categorías con atributos personalizados

**Builds personalizadas (RF-06, RF-07, RF-08, RF-09)**
- Creación y gestión de builds asociadas a un usuario
- Agregar, actualizar y eliminar componentes dentro de una build
- Validación automática de compatibilidad entre componentes
- Cálculo de costo total y estimación energética

**Proveedores y precios (RF-10, RF-16)**
- Consulta de vendedores registrados
- Precios de referencia por componente

**Notificaciones y alertas (RF-18)**
- Notificaciones automáticas sobre cambios de estado en builds
- Manejo de fallos del servicio de notificaciones sin afectar el flujo principal

## Flujos principales

### Flujo 1: Registro y autenticación
```
Usuario → POST /api/auth/register → AuthService → UserRestClient → UserService → BD
Usuario → POST /api/auth/login → AuthService → valida BCrypt → retorna JWT
```

### Flujo 2: Crear build con verificación de compatibilidad
```
Usuario autenticado → POST /api/builds → BuildService → BD
                      → PUT /api/builds/{id}/items → BuildService → CompatibilityFeignClient → CompatibilityService
```

### Flujo 3: Estimar costo de build
```
Usuario → POST /api/estimate → EstimateService → BuildRestClient → BuildService (obtener items)
                                                   → ProductRestClient → ProductService (obtener precios)
                                                   → NotificationRestClient → NotificationService (opcional)
```

### Flujo 4: Recomendación de hardware
```
Usuario → POST /api/recommendations/generate → HardwareAdvisorService → BuildFeignClient (builds del usuario)
                                                                       → ProductFeignClient (catálogo)
                                                                       → CompatibilityFeignClient (validación)
                                                                       → NotificationFeignClient (resultado)
```

## Reglas de negocio

1. **Email único**: No pueden existir dos usuarios con el mismo email
2. **Compatibilidad**: Una build es válida solo si todos sus componentes son compatibles según las reglas definidas
3. **Propiedad de builds**: Un usuario solo puede modificar sus propias builds (los administradores pueden ver todas)
4. **Notificación resiliente**: Si el servicio de notificaciones falla, el flujo principal no se interrumpe
5. **Encriptación de contraseñas**: Todas las contraseñas se almacenan con BCrypt
6. **Roles de acceso**: Los endpoints de administración requieren rol ADMIN

## Estados relevantes

**Build**: `DRAFT` → `VALIDATED` | `INCOMPATIBLE`

**Notificación**: `PENDING` → `SENT` | `FAILED`

## Datos de prueba

| Tipo | Datos |
|------|-------|
| Admin | admin@tarrobuild.cl / admin123 |
| Usuario | user@test.com / test123 |
| Categorías | Procesadores, Placas Madre, GPU, RAM, Almacenamiento, Fuentes de Poder, Gabinetes, Refrigeración |
| Productos | 36 productos en 8 categorías con especificaciones técnicas |
| Builds | 9 builds de ejemplo con diferentes configuraciones |
| Reglas de compatibilidad | CPU Socket ↔ Motherboard Socket, GPU Power Draw ↔ PSU Wattage, Motherboard Form Factor ↔ Case Form Factor |
