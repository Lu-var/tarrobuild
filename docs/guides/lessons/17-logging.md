<!-- START OF FILE: docs_lessons_17-logging_01_objetivo_y_alcance.md -->
# Documento: docs lessons 17-logging 01 objetivo y alcance
---
# Lección 17 - Logging: ¿Qué vas a aprender?

## ¿De dónde venimos?

En Lección 16 implementaste autenticación. Ahora sabes quién accede a tu API.

El siguiente paso: registrar qué hace cada usuario (auditoría).

---

## ¿Qué vas a construir?

Al terminar, tu aplicación registrará:

```
[2026-04-16 14:32:10] INFO  cl.duoc.fullstack.tickets.service.TicketService - Ticket creado: #5 "Software falla"
[2026-04-16 14:33:45] INFO  cl.duoc.fullstack.tickets.service.TicketService - Ticket actualizado: #5, estado: NEW → IN_PROGRESS
[2026-04-16 14:35:22] INFO  cl.duoc.fullstack.tickets.service.TicketService - Ticket eliminado: #5 por admin
[2026-04-16 14:36:01] ERROR cl.duoc.fullstack.tickets.service.TicketService - Fallo al actualizar #999: no encontrado
```

### Niveles en tu aplicación

- **DEBUG:** Entrada/salida de métodos, valores de variables (solo dev)
- **INFO:** Eventos de negocio (create, update, delete, login)
- **WARN:** Situaciones inesperadas (usuario no encontrado, reintentos)
- **ERROR:** Excepciones (violación de validación, error de BD)

---

## ¿Qué NO cubre esta lección?

| Tema | Razón |
|------|-------|
| ELK Stack (Elasticsearch + Kibana) | Herramienta externa, nivel producción |
| Distributed Tracing | Requiere correlacion-id complejo |
| Logs centralizados (Splunk, DataDog) | Servicios pagos |
| Structured logging (JSON) | Nivel avanzado |
| Async logging (performance) | Optimización posterior |

El foco: **SLF4J + Logback básico**.

---

## Requerimientos

| ID | Requerimiento |
|----|---------------|
| **REQ-25** | Loguear creación de ticket (INFO level) |
| **REQ-26** | Loguear actualización de ticket (INFO level) |
| **REQ-27** | Loguear eliminación de ticket (INFO level) |
| **REQ-28** | Loguear errores con stack trace (ERROR level) |
| **REQ-29** | Nivel de log configurable por perfil (DEBUG en dev, INFO en prod) |

---

## Estructura antes vs después

```
Antes:
├── controller/TicketController.java
└── service/TicketService.java        (sin logs)

Después:
├── controller/TicketController.java
└── service/TicketService.java        (+ @Slf4j, logger.info/error)

Nuevos archivos:
├── logback-spring.xml                (configuración de logs)
├── logback-spring-dev.xml            (DEBUG para desarrollo)
└── logback-spring-prod.xml           (INFO para producción)
```





<!-- START OF FILE: docs_lessons_17-logging_02_guion_paso_a_paso.md -->
# Documento: docs lessons 17-logging 02 guion paso a paso
---
# Lección 17 - Tutorial paso a paso: Logging con SLF4J

## Paso 1: Entender SLF4J + Logback

Spring Boot incluye ambos por defecto. No necesitas agregar dependencias.

## Paso 2: Agregar @Slf4j en Service

En `TicketService.java`:

```java
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j  // ← Agrega logger automáticamente
public class TicketService {

    public Ticket create(Ticket ticket) {
        log.info("Creando ticket: '{}'", ticket.getTitle());
        
        boolean exists = this.repository.existsByTitle(ticket.getTitle());
        if (exists) {
            log.warn("Título duplicado: '{}'", ticket.getTitle());
            throw new IllegalArgumentException("Ya existe ticket con este título");
        }

        try {
            Ticket saved = this.repository.save(ticket);
            log.info("Ticket creado exitosamente: ID={}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error al crear ticket", e);
            throw e;
        }
    }

    public Ticket updateById(Long id, Ticket ticket) {
        log.info("Actualizando ticket: ID={}", id);
        
        Ticket toUpdate = this.repository.getById(id);
        if (toUpdate == null) {
            log.warn("Ticket no encontrado: ID={}", id);
            return null;
        }

        try {
            toUpdate.setTitle(ticket.getTitle());
            toUpdate.setDescription(ticket.getDescription());
            this.repository.update(toUpdate);
            log.info("Ticket actualizado: ID={}", id);
            return toUpdate;
        } catch (Exception e) {
            log.error("Error al actualizar ticket: ID={}", id, e);
            throw e;
        }
    }

    public Ticket deleteById(Long id) {
        log.info("Eliminando ticket: ID={}", id);
        
        try {
            Ticket found = this.repository.deleteById(id);
            if (found != null) {
                log.info("Ticket eliminado exitosamente: ID={}", id);
            } else {
                log.warn("Ticket a eliminar no encontrado: ID={}", id);
            }
            return found;
        } catch (Exception e) {
            log.error("Error al eliminar ticket: ID={}", id, e);
            throw e;
        }
    }
}
```

## Paso 3: Configurar niveles en application.yml

```yaml
logging:
  level:
    root: INFO
    cl.duoc.fullstack.tickets: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%-5level] %logger{0} - %msg%n"
  file:
    name: logs/tickets.log
```

## Paso 4: Configurar por perfil

**application-dev.yml:**
```yaml
logging:
  level:
    root: DEBUG
    cl.duoc.fullstack.tickets: DEBUG
```

**application-prod.yml:**
```yaml
logging:
  level:
    root: INFO
    cl.duoc.fullstack.tickets: INFO
```

## Paso 5: Testear

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Salida esperada:
```
14:32:10.123 [INFO] TicketService - Ticket creado: ID=3
14:32:11.456 [DEBUG] TicketRepository - Guardando en lista: Ticket{id=3}
```





<!-- START OF FILE: docs_lessons_17-logging_03_logback_vs_serilog.md -->
# Documento: docs lessons 17-logging 03 logback vs serilog
---
# Lección 17 - Logback vs Serilog

## Comparativa

| Aspecto | Logback (Java) | Serilog (.NET) |
|---------|--------|--------|
| **Lenguaje** | Java | C# |
| **Formato default** | Texto plano | Structured (JSON) |
| **Integración Spring** | Nativa | No aplicable |
| **Almacenamiento** | Archivo + consola | Sink-based (múltiples destinos) |
| **Performance** | Bueno | Excelente |

## Logback (aquí usamos)

**Ventajas:**
- Integrado en Spring Boot
- Configuración XML/YAML simple
- Rotación de archivos automática

**Desventajas:**
- Logs en texto plano (difícil de parsear en prod)
- No es structured logging

## Serilog (alternativa para .NET)

**Ventajas:**
- Structured logging (JSON)
- Enrichment poderoso
- Sinks múltiples

**Desventajas:**
- Solo .NET
- Más complejo que Logback

## Cuándo cada uno

- **Logback:** Desarrollo, testing, aplicaciones Spring pequeñas
- **Serilog:** Aplicaciones .NET, especialmente con ELK Stack
- **JSON + ELK:** Producción con múltiples servicios

## Pattern en Logback

```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss} [%-5level] %logger{36} - %msg%n"
    #       timestamp      level      class name      message
```

**Ejemplo de salida:**
```
14:32:10 [INFO ] TicketService - Ticket creado: #5
14:32:11 [DEBUG] TicketRepository - Guardando: Ticket{id=5}
```





<!-- START OF FILE: docs_lessons_17-logging_04_ejemplos_practicos.md -->
# Documento: docs lessons 17-logging 04 ejemplos practicos
---
# Lección 17 - Ejemplos prácticos

## Loguear en Controller

```java
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/tickets")
@Slf4j
public class TicketController {

    @GetMapping
    public List<Ticket> getAllTickets() {
        log.debug("GET /tickets solicitado");
        return this.service.getTickets();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Ticket ticket) {
        log.info("POST /tickets - creando: {}", ticket.getTitle());
        try {
            Ticket created = this.service.create(ticket);
            log.info("Ticket creado exitosamente: ID={}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body("Ticket Creado");
        } catch (IllegalArgumentException e) {
            log.warn("Validación fallida: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/by-id/{id}")
    public ResponseEntity<?> deleteTicketById(@PathVariable Long id) {
        log.info("DELETE /tickets/{} solicitado", id);
        try {
            Ticket found = this.service.deleteById(id);
            if (found != null) {
                log.info("Ticket eliminado: ID={}", id);
                return ResponseEntity.ok(found);
            }
            log.warn("Ticket a eliminar no encontrado: ID={}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al eliminar ticket: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

## Loguear excepciones

```java
// Captura y loguea con stack trace
try {
    service.delete(id);
} catch (IllegalArgumentException e) {
    log.error("Error de validación en delete: ID={}", id, e);  // ← e incluye stack trace
    throw e;
}

// Salida:
// 14:32:10 [ERROR] TicketService - Error de validación en delete: ID=999
// java.lang.IllegalArgumentException: Ticket no encontrado
//   at cl.duoc.fullstack.tickets.service.TicketService.deleteById(...)
//   ...
```

## Patrones útiles

```java
// Con más contexto
log.info("Usuario {} intenta eliminar ticket {}", username, id);

// Con condición (evita concatenación si no se loguea)
if (log.isDebugEnabled()) {
    log.debug("Variable compleja: {}", complexObject.toString());
}

// Con nivel apropiado
log.debug("Valor variable x={}", x);      // Dev only
log.info("Operación exitosa");            // Todos los ambientes
log.warn("Reintentando conexión BD");     // Situación inesperada
log.error("Excepción no capturada", e);   // Error crítico
```





<!-- START OF FILE: docs_lessons_17-logging_05_configuracion_per_perfil.md -->
# Documento: docs lessons 17-logging 05 configuracion per perfil
---
# Lección 17 - Configuración por perfil

**application.yml** (base):
```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} [%-5level] %logger{0} - %msg%n"
  file:
    name: logs/tickets.log
    max-size: 10MB
    max-history: 10
```

**application-dev.yml:**
```yaml
logging:
  level:
    root: DEBUG
    cl.duoc.fullstack.tickets: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
```

**application-prod.yml:**
```yaml
logging:
  level:
    root: WARN
    cl.duoc.fullstack.tickets: INFO
    org.springframework: WARN
  file:
    name: /var/log/tickets/tickets.log
    max-size: 100MB
    max-history: 30
```

## Explicación

- **root: DEBUG** (dev) — todo es verbose
- **root: WARN** (prod) — solo warnings y errores
- **cl.duoc.fullstack.tickets: DEBUG** — package específico es verbose
- **max-size: 100MB** — rotar cuando archivo alcanza 100MB
- **max-history: 30** — guardar últimos 30 días





<!-- START OF FILE: docs_lessons_17-logging_06_troubleshooting.md -->
# Documento: docs lessons 17-logging 06 troubleshooting
---
# Lección 17 - Troubleshooting

## Problema 1: Logs no aparecen en consola

**Causa:** Nivel de log muy alto.

**Solución:**
```yaml
logging:
  level:
    cl.duoc.fullstack.tickets: DEBUG  # Baja el nivel
```

## Problema 2: Demasiados logs (ruido)

**Causa:** Nivel DEBUG para librerías externas.

**Solución:**
```yaml
logging:
  level:
    root: WARN
    cl.duoc.fullstack.tickets: DEBUG
    org.springframework: WARN
    org.hibernate: WARN
```

## Problema 3: Archivo de log no se crea

**Causa:** Carpeta logs/ no existe o sin permisos.

**Solución:**
```bash
mkdir -p logs/
chmod 755 logs/
```

## Problema 4: Stack trace incompleto

**Causa:** Loguear sin pasar la excepción.

```java
// ❌ INCORRECTO
log.error("Error: " + e.getMessage());

// ✅ CORRECTO
log.error("Error", e);  // ← Incluye stack trace completo
```

## Problema 5: Performance degradada

**Causa:** Logueo síncrono + archivos lentos.

**Solución (avanzada):**
```xml
<!-- logback.xml -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <encoder>
        <pattern>...</pattern>
    </encoder>
    <rollingPolicy class="...">
        <fileNamePattern>logs/tickets-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    </rollingPolicy>
</appender>
```

## Problema 6: Logs de Security ausentes

**Causa:** Nivel de Spring Security muy alto.

**Solución:**
```yaml
logging:
  level:
    org.springframework.security: DEBUG
```





<!-- START OF FILE: docs_lessons_17-logging_07_checklist_rubrica_minima.md -->
# Documento: docs lessons 17-logging 07 checklist rubrica minima
---
# Lección 17 - Checklist y rúbrica

## Checklist

- [ ] @Slf4j agregado en TicketService
- [ ] @Slf4j agregado en TicketController
- [ ] log.info() en create(), updateById(), deleteById()
- [ ] log.error() captura excepciones con stack trace
- [ ] logging.level.root configurado en application.yml
- [ ] application-dev.yml con DEBUG
- [ ] application-prod.yml con INFO/WARN
- [ ] Logs aparecen en consola al ejecutar
- [ ] Archivo logs/tickets.log se crea

## Rúbrica (50 pts)

| Criterio | Pts |
|----------|-----|
| @Slf4j presente en Service y Controller | 10 |
| Mínimo 5 logs INFO en operaciones principales | 15 |
| log.error() captura excepciones | 10 |
| Configuración YAML por perfil | 10 |
| Archivo log se genera correctamente | 5 |

**Total: 50 puntos**

Red flags:
❌ Ningún @Slf4j presente
❌ Logs con System.out.println()
❌ Sin configuración YAML





<!-- START OF FILE: docs_lessons_17-logging_08_actividad_individual.md -->
# Documento: docs lessons 17-logging 08 actividad individual
---
# Lección 17 - Actividad individual

## Objetivo

Agregar logging completo a tu API de Tickets usando SLF4J + Logback.

---

## Requisitos

1. **Agregar @Slf4j** a TicketService y TicketController
2. **Loguear eventos principales:**
   - Ticket creado (INFO)
   - Ticket actualizado (INFO)
   - Ticket eliminado (INFO)
   - Error al crear/actualizar/eliminar (ERROR)
3. **Configurar niveles por perfil:**
   - Dev: DEBUG
   - Prod: INFO/WARN
4. **Generar archivo de log** en `logs/tickets.log`

---

## Instrucciones

### Paso 1: Agregar @Slf4j

En TicketService:
```java
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TicketService {
    // ...
}
```

### Paso 2: Loguear operaciones CRUD

```java
public Ticket create(Ticket ticket) {
    log.info("Creando ticket: '{}'", ticket.getTitle());
    // ... resto del código
    log.info("Ticket creado: ID={}", saved.getId());
    return saved;
}
```

### Paso 3: Configurar YAML

En application.yml:
```yaml
logging:
  level:
    cl.duoc.fullstack.tickets: DEBUG
  file:
    name: logs/tickets.log
```

### Paso 4: Testear

```bash
mvn spring-boot:run
# Crear, actualizar, eliminar un ticket
# Verificar: logs/tickets.log se generó
```

---

## Desafío extra

1. Agregar correlation-id para rastrear operaciones
2. Loguear quién realizó cada acción (desde Spring Security)
3. Crear dashboard con logs en tiempo real





<!-- START OF FILE: docs_lessons_17-logging_README.md -->
# Documento: docs lessons 17-logging README
---
# Lección 17 - Logging: Auditoría e Investigación

## El problema

Sin logs, cuando algo falla en producción no tienes pista de qué pasó.

```
Cliente: "Mi ticket desapareció"
Tú: "¿Quién lo eliminó? ¿Cuándo? ¿Accidentalmente?"
Sin logs: "No sé." 😕
```

Con logs:

```
[2026-04-16 10:34:22] INFO  TicketService - Ticket #5 creado por admin
[2026-04-16 10:45:15] INFO  TicketService - Ticket #5 asignado a maria
[2026-04-16 11:02:47] ERROR TicketService - Error al eliminar #5: acceso denegado (user)
```

---

## Quick Start

### Concepto

Logging = registrar eventos con:
- **Timestamp:** cuándo
- **Level:** importancia (DEBUG, INFO, WARN, ERROR)
- **Mensaje:** qué pasó

### Niveles (del menos al más grave)

```
DEBUG   → Detalles técnicos (valores de variables)
INFO    → Eventos importantes (create, update, delete)
WARN    → Advertencias (recurso no encontrado, retry)
ERROR   → Errores (excepción lanzada)
```

---

## Lo que construirás

1. Agregar SLF4J + Logback (ya incluidos en Spring Boot)
2. Loguear en `create()`, `updateById()`, `deleteById()`
3. Diferenciar niveles por perfil (DEBUG en dev, INFO en prod)
4. Ver logs en consola + guardar en archivo

---

## Lecturas recomendadas

- Lección 11: Perfiles (DEBUG vs INFO por ambiente)
- Lección 16: Spring Security (loguear quién accede)



