<!-- START OF FILE: docs_lessons_18-global-exception-handling_01_objetivo_y_alcance.md -->
# Documento: docs lessons 18-global-exception-handling 01 objetivo y alcance
---
# Lección 18 - Global Exception Handling: ¿Qué vas a aprender?

## ¿De dónde venimos?

En Lección 17 aprendiste a loguear eventos. Ahora registras qué pasó, pero si una excepción inesperada ocurre, el cliente recibe un `500` genérico sin detalles.

---

## ¿Qué vas a construir?

Al terminar, tu API manejará automáticamente TODAS las excepciones:

```
POST /tickets con título vacío
    ↓
@Valid valida → MethodArgumentNotValidException
    ↓
GlobalExceptionHandler captura
    ↓
Respuesta: 400 + {"message": "Título no puede estar vacío"}
```

### Excepciones capturadas

- `IllegalArgumentException` → 400 Bad Request
- `ConstraintViolationException` → 400 Bad Request
- `MethodArgumentNotValidException` → 400 Bad Request
- `EntityNotFoundException` → 404 Not Found
- `BadCredentialsException` → 401 Unauthorized
- `AccessDeniedException` → 403 Forbidden
- `Exception` genérico → 500 Internal Server Error + log

---

## ¿Qué NO cubre?

| Tema | Razón |
|------|-------|
| Excepciones de Base de Datos | Llegan después de JPA avanzado |
| Custom exceptions propias | Nivel intermedio |
| Retry logic | Patrón avanzado |

El foco: **handler centralizado para excepciones comunes**.

---

## Requerimientos

| ID | Requerimiento |
|----|---------------|
| **REQ-30** | Crear GlobalExceptionHandler con @ControllerAdvice |
| **REQ-31** | Capturar IllegalArgumentException → 400 |
| **REQ-32** | Capturar MethodArgumentNotValidException → 400 |
| **REQ-33** | Capturar Exception genérico → 500 + log |
| **REQ-34** | Stack trace solo en dev, oculto en prod |
| **REQ-35** | Capturar BadCredentialsException → 401 |
| **REQ-36** | Capturar AccessDeniedException → 403 |

---

## Estructura antes vs después

```
Antes:
└── controller/TicketController.java   (try/catch en cada endpoint)

Después:
├── controller/TicketController.java   (sin try/catch innecesario)
├── config/GlobalExceptionHandler.java ← NUEVO (@ControllerAdvice)
└── model/ErrorResponse.java           (reutilizado)
```





<!-- START OF FILE: docs_lessons_18-global-exception-handling_02_guion_paso_a_paso.md -->
# Documento: docs lessons 18-global-exception-handling 02 guion paso a paso
---
# Lección 18 - Tutorial paso a paso

## Paso 1: Crear GlobalExceptionHandler

```java
package cl.duoc.fullstack.tickets.config;

import cl.duoc.fullstack.tickets.model.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Validación fallida: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validación fallida");
        log.warn("Validación de argumentos: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(message));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException e) {
        log.warn("Recurso no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException e) {
        log.warn("Credenciales inválidas: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Credenciales inválidas"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        log.warn("Acceso denegado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("Acceso denegado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception e) {
        log.error("Excepción no capturada", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Error interno del servidor"));
    }
}
```

## Paso 2: Simplificar endpoints

Antes (con try/catch):
```java
@PostMapping
public ResponseEntity<?> create(@RequestBody Ticket ticket) {
    try {
        Ticket saved = service.create(ticket);
        return ResponseEntity.status(201).body(saved);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}
```

Después (GlobalExceptionHandler maneja la excepción):
```java
@PostMapping
public ResponseEntity<?> create(@RequestBody Ticket ticket) {
    Ticket saved = service.create(ticket);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    // Si IllegalArgumentException ocurre → GlobalExceptionHandler la captura
}
```

## Paso 3: Testear

```
POST /tickets con datos inválidos
```

Handler captura → Respuesta: 400 + `{"message": "..."}`

Sin handler, sería: 500 + stack trace

## Paso 4: Stack trace en dev

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleGeneric(Exception e) {
    log.error("Excepción no capturada", e);
    
    String message = (isDev() ? e.getStackTrace().toString() : "Error interno");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(message));
}

private boolean isDev() {
    return System.getProperty("app.env", "prod").equals("dev");
}
```





<!-- START OF FILE: docs_lessons_18-global-exception-handling_03_local_vs_global.md -->
# Documento: docs lessons 18-global-exception-handling 03 local vs global
---
# Lección 18 - Try/Catch local vs handler global

## Comparativa

| Aspecto | Try/Catch local | @ControllerAdvice |
|--------|---------|---------|
| **Dónde captura** | Cada endpoint | Una clase centralizada |
| **Repetición de código** | Mucha | Ninguna |
| **Mantenimiento** | Difícil | Fácil |
| **Lógica de error** | Dispersa | Centralizada |
| **Para debugging** | Acceso directo | Log centralizado |

## Ejemplo: Try/Catch local

```java
@PostMapping
public ResponseEntity<?> create(@RequestBody Ticket ticket) {
    try {
        return ResponseEntity.status(201).body(service.create(ticket));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}

@PutMapping("/{id}")
public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Ticket ticket) {
    try {
        Ticket updated = service.updateById(id, ticket);
        return ResponseEntity.ok(updated);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}

@DeleteMapping("/{id}")
public ResponseEntity<?> delete(@PathVariable Long id) {
    try {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}
```

**Problema:** El catch se repite 3 veces.

## Ejemplo: GlobalExceptionHandler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handle(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}

@PostMapping
public ResponseEntity<?> create(@RequestBody Ticket ticket) {
    return ResponseEntity.status(201).body(service.create(ticket));
}

@PutMapping("/{id}")
public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Ticket ticket) {
    Ticket updated = service.updateById(id, ticket);
    return ResponseEntity.ok(updated);
}

@DeleteMapping("/{id}")
public ResponseEntity<?> delete(@PathVariable Long id) {
    service.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

**Ventaja:** Endpoints limpios, lógica centralizada.

## Cuándo cada uno

- **Try/Catch local:** 
  - Excepción específica del endpoint
  - Lógica de recuperación personalizada
  
- **Global handler:**
  - Excepciones comunes (validation, not found)
  - Respuesta uniforme requerida
  - Muchos endpoints





<!-- START OF FILE: docs_lessons_18-global-exception-handling_04_ejemplos_practicos.md -->
# Documento: docs lessons 18-global-exception-handling 04 ejemplos practicos
---
# Lección 18 - Ejemplos prácticos

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Validación fallida: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
            .getAllErrors().stream()
            .map(err -> err.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Error de validación: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .collect(Collectors.joining(", "));
        log.warn("Violación de restricción: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(message));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException e) {
        log.warn("Recurso no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception e) {
        log.error("Excepción no capturada", e);
        String message = isDevelopment() ? 
            e.getStackTrace().toString() : 
            "Error interno del servidor";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(message));
    }

    private boolean isDevelopment() {
        return System.getProperty("app.env", "prod").equals("dev");
    }
}
```

## Endpoints sin try/catch

```java
@RestController
@RequestMapping("/tickets")
public class TicketController {

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Ticket ticket) {
        Ticket saved = service.create(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body("Ticket Creado");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getById(@PathVariable Long id) {
        Ticket found = service.getById(id);
        return ResponseEntity.ok(found);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Flujos de captura

```
1. POST /tickets (sin "title")
   → @Valid valida
   → MethodArgumentNotValidException
   → GlobalExceptionHandler.handleValidation()
   → Respuesta: 400 + {"message": "El titulo es requerido"}

2. DELETE /tickets/999 (no existe)
   → service.deleteById(999)
   → return null
   → EntityNotFoundException lanzada
   → GlobalExceptionHandler.handleNotFound()
   → Respuesta: 404 + {"message": "..."}

3. POST /tickets con creador == asignado
   → service.create()
   → IllegalArgumentException
   → GlobalExceptionHandler.handleIllegalArgument()
   → Respuesta: 400 + {"message": "El creador y asignado no pueden ser iguales"}
```





<!-- START OF FILE: docs_lessons_18-global-exception-handling_05_respuesta_uniforme.md -->
# Documento: docs lessons 18-global-exception-handling 05 respuesta uniforme
---
# Lección 18 - Respuesta uniforme de errores

## Estructura base

```java
public record ErrorResponse(String message) {}
```

## En dev: incluir stack trace

```java
public record ErrorResponse(String message, String details, String timestamp) {}

// Handler
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleGeneric(Exception e) {
    String details = isDevelopment() ? 
        Arrays.toString(e.getStackTrace()) : 
        null;
    
    ErrorResponse response = new ErrorResponse(
        e.getMessage(),
        details,
        LocalDateTime.now().toString()
    );
    
    return ResponseEntity.status(500).body(response);
}
```

## En prod: solo mensaje

```json
{
  "message": "Error interno del servidor",
  "timestamp": "2026-04-16T14:32:10"
}
```

## En dev: con stack trace

```json
{
  "message": "NullPointerException: valor es null",
  "details": "[cl.duoc.fullstack.tickets.service.TicketService.create(...), ...]",
  "timestamp": "2026-04-16T14:32:10"
}
```

## Controlar con propiedad

```yaml
# application.yml
app.environment: ${APP_ENV:dev}

# application-prod.yml
app.environment: prod
```

```java
@Value("${app.environment}")
private String environment;

private boolean isDevelopment() {
    return "dev".equals(environment);
}
```





<!-- START OF FILE: docs_lessons_18-global-exception-handling_06_validacion_integrada.md -->
# Documento: docs lessons 18-global-exception-handling 06 validacion integrada
---
# Lección 18 - Capturar validaciones de Spring

## @Valid y MethodArgumentNotValidException

```java
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody Ticket ticket) {
    // Si validación falla → MethodArgumentNotValidException
    // GlobalExceptionHandler la captura
    return ResponseEntity.status(201).body(service.create(ticket));
}
```

## Handler para MethodArgumentNotValidException

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<?> handleMethodArgumentNotValid(
        MethodArgumentNotValidException e) {
    
    String message = e.getBindingResult()
        .getAllErrors().stream()
        .map(ObjectError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    
    log.warn("Error de validación: {}", message);
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(message));
}
```

## Handler para ConstraintViolationException

```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<?> handleConstraintViolation(
        ConstraintViolationException e) {
    
    String message = e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    
    log.warn("Violación de restricción: {}", message);
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(message));
}
```

## Ejemplos de validaciones capturadas

```
1. POST /tickets sin "title"
   @NotBlank(message = "El titulo es requerido")
   → Capturado: "El titulo es requerido"

2. POST /tickets con title.length() > 50
   @Size(max = 50)
   → Capturado: "Debe tener máximo 50 caracteres"

3. PUT /tickets/999 (no existe)
   → Capturado: "Ticket con ID 999 no encontrado"
```





<!-- START OF FILE: docs_lessons_18-global-exception-handling_07_troubleshooting.md -->
# Documento: docs lessons 18-global-exception-handling 07 troubleshooting
---
# Lección 18 - Troubleshooting

## Problema 1: Handler no se ejecuta

**Causa:** `@ControllerAdvice` mal declarado.

**Solución:**
```java
@ControllerAdvice  // ← Falta esta anotación
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handle(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(...);
    }
}
```

## Problema 2: Spring Security lanza excepción antes que handler

**Causa:** Orden de ejecución (autenticación antes de lógica).

**Síntoma:** 401/403 sin pasar por handler.

**Solución:** Crear handler separado para excepciones de Security:

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("Acceso denegado"));
}
```

## Problema 3: Varios handlers pero solo uno se ejecuta

**Causa:** Order de herencia (mas específico primero).

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<?> handleValidation(...) { }  // Primero (más específico)

@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleGeneric(...) { }      // Último (genérico)
```

## Problema 4: Stack trace no aparece en dev

**Causa:** Chequeo de environment incorrecto.

**Solución:**
```java
@Value("${app.environment:dev}")
private String environment;

private boolean isDev() {
    return "dev".equals(environment);
}
```

## Problema 5: Handler ignora validaciones @Valid

**Causa:** `@Valid` pero sin handler para `MethodArgumentNotValidException`.

**Solución:**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<?> handle(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getAllErrors()
        .stream().map(ObjectError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse(msg));
}
```

## Problema 6: Mensaje de error muy genérico

**Causa:** `catch (Exception e) { return error("Algo falló"); }`

**Solución:** Log primero, respuesta después:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handle(Exception e) {
    log.error("Excepción: ", e);  // ← Log con stack
    return ResponseEntity.status(500)
        .body(new ErrorResponse(e.getMessage()));
}
```





<!-- START OF FILE: docs_lessons_18-global-exception-handling_08_actividad_individual.md -->
# Documento: docs lessons 18-global-exception-handling 08 actividad individual
---
# Lección 18 - Actividad individual

## Objetivo

Crear un GlobalExceptionHandler que capture todas las excepciones de tu API y devuelva respuestas uniformes.

---

## Requisitos

1. **Crear GlobalExceptionHandler**
   - Anotación `@ControllerAdvice`
   - Mínimo 4 handlers para diferentes excepciones

2. **Handlers requeridos**
   - `IllegalArgumentException` → 400
   - `MethodArgumentNotValidException` → 400
   - `EntityNotFoundException` → 404
   - `Exception` genérico → 500

3. **Simplificar endpoints**
   - Remover try/catch innecesario
   - Dejar que GlobalExceptionHandler maneje errores

4. **Testear múltiples escenarios**
   - POST sin datos requeridos (validación)
   - GET con ID inexistente
   - POST con creador == asignado (negocio)
   - Error genérico (500)

---

## Instrucciones paso a paso

### Paso 1: Crear GlobalExceptionHandler

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegal(IllegalArgumentException e) {
        log.warn("Validación: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(e.getMessage()));
    }
    // ... resto de handlers
}
```

### Paso 2: Simplificar TicketController

Remueve try/catch y deja que handler los capture.

### Paso 3: Testear

```
POST /tickets (sin title)
→ 400 + {"message": "El titulo es requerido"}

GET /tickets/999
→ 404 + {"message": "Ticket no encontrado"}

POST /tickets (creador == asignado)
→ 400 + {"message": "No pueden ser iguales"}

Excepción inesperada
→ 500 + {"message": "Error interno"}
```

---

## Desafío extra

1. Incluir timestamp en respuesta
2. Stack trace solo en dev
3. Diferentes mensajes para dev vs prod





<!-- START OF FILE: docs_lessons_18-global-exception-handling_README.md -->
# Documento: docs lessons 18-global-exception-handling README
---
# Lección 18 - Global Exception Handling

## El problema

```java
// ❌ Código repetitivo
@PostMapping
public ResponseEntity<?> create(@RequestBody Ticket ticket) {
    try {
        Ticket saved = service.create(ticket);
        return ResponseEntity.status(201).body(saved);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("Error interno"));
    }
}
```

Cada endpoint repite el mismo pattern. Con `@ControllerAdvice`, un solo handler captura TODAS las excepciones.

---

## Quick Start

### Concepto

`@ControllerAdvice` = handler global de excepciones

```
Usuario → Request con datos inválidos
    ↓
Spring valida → MethodArgumentNotValidException
    ↓
GlobalExceptionHandler.handle() ← CAPTURA AQUÍ
    ↓
Respuesta: 400 + {"message": "Título no puede estar vacío"}
```

---

## Lo que construirás

1. Crear `GlobalExceptionHandler`
2. Agregar handlers para:
   - `IllegalArgumentException` → 400 Bad Request
   - `EntityNotFoundException` → 404 Not Found
   - `ValidationException` → 400 Bad Request
   - `Exception` genérico → 500 Internal Server Error
3. Diferencia entre dev (stack trace) y prod (solo mensaje)

---

## Lecturas recomendadas

- Lección 07: Manejo de errores local (base)
- Lección 17: Logging (registrar excepciones)



