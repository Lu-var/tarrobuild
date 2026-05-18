<!-- START OF FILE: docs_lessons_16-spring-security_01_objetivo_y_alcance.md -->
# Documento: docs lessons 16-spring-security 01 objetivo y alcance
---
# Lección 16 — Spring Security: Objetivo y Alcance

## ¿De dónde venimos?

En la Lección 15 implementaste **migraciones de base de datos con Flyway**: tu esquema está versionado, controlado y reproducible en cualquier entorno.

El siguiente paso natural es proteger esa API: actualmente cualquiera puede crear, modificar o eliminar tickets sin identificarse. Un usuario malicioso podría sabotear todo el sistema con una sola petición HTTP.

> 💡 **Conexión con Flyway:** Los usuarios que se autenticarán existen en la tabla `users`. En esta lección agregarás el campo `password` con una nueva migración Flyway — exactamente como aprendiste en la lección anterior.

---

## ¿Qué vas a construir?

Al terminar esta lección tu API tendrá:

1. **Entidad `User` actualizada:** campo `password` (hash BCrypt) + enum `Role` con tres niveles
2. **Migración V5:** agrega columna `password` a la tabla `users`
3. **Migración V6:** seed de 3 usuarios con contraseñas hasheadas
4. **`CustomUserDetailsService`:** carga credenciales desde `UserRepository` en tiempo real
5. **Autenticación HTTP Basic:** credenciales en el header `Authorization`
6. **Sesión STATELESS:** la API no guarda estado de sesión — cada petición se autentica por sí sola
7. **Tres niveles de autorización:**
   - `GET` tickets/categories/tags/users → público (sin autenticación)
   - `POST` y `PUT` tickets → ROLE_USER, ROLE_AGENT o ROLE_ADMIN
   - `DELETE` tickets + gestión de categorías/tags/usuarios → solo ROLE_ADMIN

### Flujo completo

```
1. POST /tickets — sin Authorization
   → 401 Unauthorized

2. POST /tickets — Authorization: Basic (ana.garcia / user123 = rol USER)
   → 201 Created ✅

3. POST /tickets — Authorization: Basic (carlos.lopez / user123 = rol AGENT)
   → 201 Created ✅

4. DELETE /tickets/by-id/1 — Authorization: Basic (ana.garcia = USER)
   → 403 Forbidden ❌

5. DELETE /tickets/by-id/1 — Authorization: Basic (admin = ADMIN)
   → 204 No Content ✅
```

---

## Los tres roles del sistema

| Rol | Descripción | Puede crear tickets | Puede editar tickets | Puede eliminar tickets |
|-----|-------------|:-------------------:|:--------------------:|:----------------------:|
| `USER` | Usuario final que reporta problemas | ✅ | Solo tickets propios | ❌ |
| `AGENT` | Agente de soporte que gestiona tickets | ✅ | Solo tickets asignados | ❌ |
| `ADMIN` | Administrador del sistema | ✅ | ✅ | ✅ |

---

## ¿Qué NO cubre esta lección? (y por qué)

| Tema | Razón |
|------|-------|
| JWT (JSON Web Tokens) | Requiere entender primero sesiones y autenticación básica |
| OAuth 2.0 (Google, GitHub) | Autenticación externa — primero domina autenticación local |
| Refresh tokens | Nivel siguiente después de dominar los fundamentos |
| Two-Factor Authentication | Nivel avanzado |
| Registro de usuarios desde la API | Se cubre en lecciones posteriores |

El foco de esta lección: **HTTP Basic Auth + usuarios en base de datos + reglas por rol + autorización por recurso + sesión STATELESS**.

---

## Requerimientos que implementamos

| ID | Requerimiento |
|----|---------------|
| **REQ-16-01** | `GET /tickets` y `GET /tickets/by-id/{id}` son públicos |
| **REQ-16-02** | `POST /tickets` requiere ROLE_USER, ROLE_AGENT o ROLE_ADMIN |
| **REQ-16-03** | `DELETE /tickets/by-id/{id}` requiere ROLE_ADMIN |
| **REQ-16-04** | Sin autenticación → 401 Unauthorized |
| **REQ-16-05** | Autenticación con rol insuficiente → 403 Forbidden |
| **REQ-16-06** | Contraseñas almacenadas con BCrypt (cost 10) en la base de datos |
| **REQ-16-07** | `CustomUserDetailsService` carga usuarios desde `UserRepository` |
| **REQ-16-08** | Migración Flyway agrega columna `password` (V5) |
| **REQ-16-09** | Migración Flyway seed de usuarios con hashes (V6) |
| **REQ-16-10** | API STATELESS — sin cookies ni sesiones HTTP |
| **REQ-16-11** | `PUT /tickets/by-id/{id}` permite a USER editar solo tickets propios, a AGENT solo tickets asignados y a ADMIN cualquier ticket |

---

## Estructura inicial vs final

```
Antes (Lección 15):
├── model/User.java                     (sin campo password; role es String)
├── config/DataInitializer.java         (seed sin contraseñas)
└── resources/db/migration/
    └── V4__Add_audit_tables.sql

Después (Lección 16):
├── model/User.java                     ← MODIFICADO: +password, role como enum
├── config/
│   ├── SecurityConfig.java             ← NUEVO
│   ├── CustomUserDetailsService.java   ← NUEVO
│   └── TicketSecurity.java             ← NUEVO: autorización por ticket específico
├── config/DataInitializer.java         ← MODIFICADO: +PasswordEncoder, +passwords, @Profile("h2")
└── resources/db/migration/
    ├── V5__lesson_16_add_password_to_users.sql   ← NUEVO
    └── V6__lesson_16_seed_users_with_auth.sql    ← NUEVO
```

---

## Estructura de la Lección

1. **[Este documento](01_objetivo_y_alcance.md)** — Objetivo y alcance
2. **[Guión Paso a Paso](02_guion_paso_a_paso.md)** ⭐ — Instrucciones prácticas
3. **[Autenticación vs Autorización](03_autenticacion_vs_autorizacion.md)** — Conceptos y arquitectura
4. **[Ejemplos Avanzados](04_ejemplos_practicos.md)** — Código adicional listo para usar
5. **[Cifrado de Contraseñas](05_password_encoding.md)** — BCrypt en detalle
6. **[Troubleshooting](06_troubleshooting.md)** — Errores frecuentes y soluciones
7. **[Checklist](07_checklist_rubrica_minima.md)** — Verificación antes de entregar
8. **[Actividad Individual](08_actividad_individual.md)** — Tu tarea





<!-- START OF FILE: docs_lessons_16-spring-security_02_guion_paso_a_paso.md -->
# Documento: docs lessons 16-spring-security 02 guion paso a paso
---
# Lección 16 — Guion Paso a Paso: Spring Security con Base de Datos

Sigue esta guía en orden. El objetivo es proteger la API con HTTP Basic Auth, cargar usuarios desde la base de datos y autorizar endpoints por rol.

> **Idea central:** no crearás un endpoint `/login`. Spring Security intercepta cada petición, lee el header `Authorization: Basic ...`, autentica al usuario y luego decide si puede ejecutar el endpoint solicitado.

---

## Paso 1: Agregar la dependencia de Spring Security

En `pom.xml`, dentro de `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Ejecuta:

```bash
# Linux / macOS
./mvnw clean install

# Windows
mvnw.cmd clean install
```

---

## Paso 2: Observar el comportamiento por defecto

Después de agregar la dependencia y antes de crear `SecurityConfig`, Spring Boot protege todos los endpoints automáticamente.

Al arrancar la aplicación verás algo parecido a:

```text
Using generated security password: 3f2a1b4c-8d7e-4f3a-9c1b-2d6e8f0a4b5c
```

Si llamas a cualquier endpoint:

```http
GET http://localhost:8080/ticket-app/tickets
```

Resultado esperado en este momento:

```text
401 Unauthorized
```

Esto ocurre porque Spring Security encontró la dependencia y aplicó una configuración temporal. En los siguientes pasos crearás tu propia configuración.

---

## Paso 3: Preparar `User.java` para autenticación

La entidad `User` debe tener:

- `password`: hash BCrypt de la contraseña.
- `role`: enum con `USER`, `AGENT`, `ADMIN`.
- `active`: permite deshabilitar usuarios sin eliminarlos.

Código completo de `model/User.java`:

```java
package cl.duoc.fullstack.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es requerido")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email no tiene un formato válido")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean active = true;

    public enum Role {
        USER,
        AGENT,
        ADMIN
    }
}
```

Puntos clave:

- `@Enumerated(EnumType.STRING)` guarda `USER`, `AGENT` o `ADMIN` como texto, no como número.
- `password` guarda el hash BCrypt, no la contraseña en texto plano.
- Si `active=false`, el usuario existe, pero no debe poder autenticarse.

---

## Paso 4: Preparar la base de datos

### V5: agregar columna `password`

Archivo: `src/main/resources/db/migration/V5__lesson_16_add_password_to_users.sql`

```sql
ALTER TABLE users ADD COLUMN password VARCHAR(255);
```

> Si tu proyecto ya tiene más migraciones, usa el siguiente número disponible. Revisa `flyway_schema_history` si tienes dudas.

### V6: sembrar usuarios con hashes BCrypt

Archivo: `src/main/resources/db/migration/V6__lesson_16_seed_users_with_auth.sql`

```sql
INSERT INTO users (name, email, role, active, password) VALUES
  ('Administrador',  'admin@empresa.com',       'ADMIN', true, '$2a$10$gT.PsFi3xTq9xc3virQAfesYBesY5g53tQ5R7lgJGqgVdVMH0I8qa'),
  ('Ana Garcia',     'ana.garcia@empresa.com',  'USER',  true, '$2a$10$LAK58ME84bgotvy2eL.eWeobSCHMDsaD3BajXq/swyevMwfw8PW/m'),
  ('Carlos Lopez',   'carlos.lopez@empresa.com','AGENT', true, '$2a$10$LAK58ME84bgotvy2eL.eWeobSCHMDsaD3BajXq/swyevMwfw8PW/m');
```

Credenciales de prueba:

| Email | Contraseña | Rol |
|-------|------------|-----|
| `admin@empresa.com` | `pass123` | ADMIN |
| `ana.garcia@empresa.com` | `user123` | USER |
| `carlos.lopez@empresa.com` | `user123` | AGENT |

Nunca guardes contraseñas en texto plano en la base de datos. Spring Security comparará la contraseña enviada por el cliente contra estos hashes usando BCrypt.

### H2: actualizar `DataInitializer`

Si usas H2 con datos en memoria y Flyway está deshabilitado, `DataInitializer` debe insertar usuarios con `PasswordEncoder`.

Fragmento relevante:

```java
@Component
@Profile("h2")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@empresa.com");
            admin.setPassword(passwordEncoder.encode("pass123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);

            User ana = new User();
            ana.setName("Ana Garcia");
            ana.setEmail("ana.garcia@empresa.com");
            ana.setPassword(passwordEncoder.encode("user123"));
            ana.setRole(Role.USER);
            ana.setActive(true);
            userRepository.save(ana);

            User carlos = new User();
            carlos.setName("Carlos Lopez");
            carlos.setEmail("carlos.lopez@empresa.com");
            carlos.setPassword(passwordEncoder.encode("user123"));
            carlos.setRole(Role.AGENT);
            carlos.setActive(true);
            userRepository.save(carlos);
        }
    }
}
```

Puntos clave:

- `@Profile("h2")` evita duplicar datos en MySQL/PostgreSQL/Supabase.
- `passwordEncoder.encode(...)` genera un hash BCrypt distinto en cada ejecución, y eso está bien.

---

## Paso 5: Completar `UserRepository`

Spring Security recibirá un username desde Basic Auth. En esta lección usaremos el email como username, por eso el repositorio debe poder buscar usuarios por email.

Código de `respository/UserRepository.java`:

```java
package cl.duoc.fullstack.tickets.respository;

import cl.duoc.fullstack.tickets.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

Spring Data JPA implementa automáticamente `findByEmail` por convención de nombre.

---

## Paso 6: Crear el `PasswordEncoder`

Spring Security necesita un `PasswordEncoder` para comparar la contraseña plana enviada por el cliente contra el hash BCrypt guardado en la base de datos.

Este bean irá en `SecurityConfig`, pero conviene entenderlo antes:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Qué hace:

- `encode("user123")`: genera un hash BCrypt para guardar o sembrar usuarios.
- `matches("user123", hashBCrypt)`: valida una contraseña plana contra un hash.
- Spring Security llama internamente a `matches(...)` durante la autenticación.

No hagas esto:

```java
password.equals(user.getPassword())
```

Eso compara texto plano contra hash y siempre fallará. Además, no es seguro.

---

## Paso 7: Crear `CustomUserDetailsService`

`UserDetailsService` es el puente entre Spring Security y tu tabla `users`.

Cuando llega este header:

```text
Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
```

Spring Security lo decodifica como:

```text
ana.garcia@empresa.com:user123
```

Luego llama a:

```java
loadUserByUsername("ana.garcia@empresa.com")
```

Crea `config/CustomUserDetailsService.java`:

```java
package cl.duoc.fullstack.tickets.config;

import cl.duoc.fullstack.tickets.model.User;
import cl.duoc.fullstack.tickets.respository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .filter(u -> u.getPassword() != null && !u.getPassword().isBlank())
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .disabled(!user.isActive())
            .build();
    }
}
```

Qué hace cada parte:

```text
userRepository.findByEmail(email)
    Busca al usuario por email.

.filter(u -> u.getPassword() != null && !u.getPassword().isBlank())
    Rechaza usuarios que existen, pero no tienen contraseña configurada.

.password(user.getPassword())
    Entrega a Spring Security el hash BCrypt guardado en la BD.

.roles(user.getRole().name())
    Recibe USER, AGENT o ADMIN. Spring agrega el prefijo ROLE_ automáticamente.

.disabled(!user.isActive())
    Si active=false, la autenticación falla con 401.
```

> **Importante:** `.roles("ADMIN")` produce internamente `ROLE_ADMIN`. Por eso después puedes usar `.hasRole("ADMIN")` y `@PreAuthorize("hasRole('ADMIN')")`.

Alternativa válida, pero no la mezcles con `.roles(...)` en el código principal:

```java
.authorities("ROLE_" + user.getRole().name())
```

---

## Paso 8: Crear `SecurityConfig`

`SecurityConfig` define el `SecurityFilterChain`: la cadena de filtros que se ejecuta antes de los controladores.

Crea `config/SecurityConfig.java`:

```java
package cl.duoc.fullstack.tickets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/tickets", "/tickets/by-id/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/categories", "/categories/by-id/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/tags", "/tags/by-id/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/users", "/users/by-id/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/tickets/{id}/history").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/tickets").hasAnyRole("USER", "AGENT", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/tickets/by-id/**").hasAnyRole("USER", "AGENT", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/tickets/by-id/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/categories/by-id/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/categories/by-id/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/tags").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/tags/by-id/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/tags/by-id/**").hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Explicación por bloque:

```text
@EnableMethodSecurity
    Habilita @PreAuthorize en controladores y servicios.

csrf(csrf -> csrf.disable())
    Deshabilita CSRF. Es correcto para esta API REST stateless sin cookies de sesión.

SessionCreationPolicy.STATELESS
    Spring no crea sesión. Cada request debe traer Authorization: Basic ...

authorizeHttpRequests(...)
    Define autorización por método HTTP y ruta.

httpBasic(Customizer.withDefaults())
    Activa BasicAuthenticationFilter, el filtro que lee Authorization: Basic ...

PasswordEncoder
    Define BCrypt como algoritmo de comparación de contraseñas.
```

> **Rutas sin context-path:** en `SecurityConfig` se escribe `/tickets`, no `/ticket-app/tickets`. El context path `/ticket-app` ya fue procesado antes.

---

## Paso 9: Entender el flujo interno de autenticación

Este es el recorrido completo cuando un cliente envía una petición protegida:

```text
Cliente
  Authorization: Basic base64(email:password)
        |
        v
BasicAuthenticationFilter
        |
        v
AuthenticationManager
        |
        v
DaoAuthenticationProvider
        |
        v
CustomUserDetailsService.loadUserByUsername(email)
        |
        v
PasswordEncoder.matches(passwordPlano, hashBCrypt)
        |
        v
SecurityContext: Authentication autenticado con ROLE_...
        |
        v
AuthorizationFilter / @PreAuthorize
        |
        v
Controlador
```

Responsabilidades:

- `BasicAuthenticationFilter`: lee y decodifica el header Basic.
- `CustomUserDetailsService`: carga usuario, hash y rol desde la base de datos.
- `PasswordEncoder`: valida la contraseña enviada contra el hash.
- `AuthorizationFilter`: revisa reglas de `SecurityConfig`.
- `@PreAuthorize`: revisa reglas declaradas sobre métodos.

Respuestas esperadas:

- Sin header, header inválido, usuario inexistente, contraseña incorrecta o usuario inactivo: `401 Unauthorized`.
- Usuario autenticado, pero sin rol suficiente: `403 Forbidden`.
- Usuario autenticado y autorizado: se ejecuta el controlador.

---

## Paso 10: Autorizar con `@PreAuthorize`

Las reglas de `SecurityConfig` sirven bien para permisos generales por ruta. `@PreAuthorize` es útil cuando quieres que la regla quede cerca del caso de uso o dependa de parámetros del método.

Ejemplo para eliminar tickets solo con ADMIN:

```java
import org.springframework.security.access.prepost.PreAuthorize;

@DeleteMapping("/by-id/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    ticketService.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

Ejemplo para crear tickets con cualquier usuario autenticado de los roles permitidos:

```java
import org.springframework.security.access.prepost.PreAuthorize;

@PostMapping
@PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request));
}
```

Regla práctica:

- Usa `SecurityConfig` para reglas simples y globales por método HTTP y ruta.
- Usa `@PreAuthorize` para reglas específicas del caso de uso.
- No dupliques reglas contradictorias. Si `SecurityConfig` bloquea antes, el método anotado nunca se ejecuta.

Expresiones frecuentes:

| Expresión | Significado |
|-----------|-------------|
| `hasRole('ADMIN')` | Requiere `ROLE_ADMIN` |
| `hasAnyRole('USER', 'AGENT', 'ADMIN')` | Requiere cualquiera de esos roles |
| `isAuthenticated()` | Requiere cualquier usuario autenticado |
| `authentication.name == 'admin@empresa.com'` | Requiere un usuario específico |

---

## Paso 11: Autorizar edición según el ticket específico

La regla para editar tickets no depende solo del rol. También depende del ticket específico:

- `USER`: puede editar solo tickets que él creó (`ticket.createdBy.email == authentication.name`).
- `AGENT`: puede editar solo tickets que tiene asignados (`ticket.assignedTo.email == authentication.name`).
- `ADMIN`: puede editar cualquier ticket.

Esta regla no se puede expresar correctamente solo con `hasRole(...)`, porque `hasRole(...)` no sabe quién creó el ticket ni a quién está asignado. Para resolverlo, delegaremos la decisión a un bean de seguridad.

### Crear `TicketSecurity`

Crea `config/TicketSecurity.java`:

```java
package cl.duoc.fullstack.tickets.config;

import cl.duoc.fullstack.tickets.model.Ticket;
import cl.duoc.fullstack.tickets.respository.TicketRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("ticketSecurity")
public class TicketSecurity {

    private final TicketRepository ticketRepository;

    public TicketSecurity(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public boolean canEdit(Long ticketId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }

        String email = authentication.getName();

        return ticketRepository.findById(ticketId)
            .map(ticket -> canEditTicket(ticket, email, authentication))
            .orElse(false);
    }

    private boolean canEditTicket(Ticket ticket, String email, Authentication authentication) {
        if (hasRole(authentication, "ROLE_USER")) {
            return ticket.getCreatedBy() != null
                && email.equals(ticket.getCreatedBy().getEmail());
        }

        if (hasRole(authentication, "ROLE_AGENT")) {
            return ticket.getAssignedTo() != null
                && email.equals(ticket.getAssignedTo().getEmail());
        }

        return false;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
```

Qué hace:

```text
@Component("ticketSecurity")
    Registra el bean con ese nombre para poder llamarlo desde @PreAuthorize.

canEdit(Long ticketId, Authentication authentication)
    Recibe el id del ticket y el usuario autenticado.

authentication.getName()
    Devuelve el username autenticado. En esta lección es el email.

ticketRepository.findById(ticketId)
    Carga el ticket para revisar createdBy y assignedTo.

ROLE_ADMIN
    Puede editar sin revisar propietario ni asignación.

ROLE_USER
    Solo puede editar si ticket.createdBy.email coincide con authentication.name.

ROLE_AGENT
    Solo puede editar si ticket.assignedTo.email coincide con authentication.name.
```

### Aplicar la regla en el controlador

Actualiza el método `PUT /tickets/by-id/{id}` en `TicketController`:

```java
import org.springframework.security.access.prepost.PreAuthorize;

@PutMapping("/by-id/{id}")
@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")
public ResponseEntity<Object> updateTicketById(
    @PathVariable Long id,
    @Valid @RequestBody TicketRequest request) {
    try {
        Optional<TicketResult> updated = this.service.updateById(id, request);
        if (updated.isPresent()) {
            return ResponseEntity.ok(updated.get());
        }
        return ResponseEntity.notFound().build();
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }
}
```

La expresión SpEL se lee así:

```text
@ticketSecurity
    Bean llamado ticketSecurity.

canEdit(#id, authentication)
    Ejecuta el método canEdit usando el @PathVariable id y el usuario autenticado.

#id
    Parámetro id del método updateTicketById.

authentication
    Objeto Authentication actual creado por Spring Security después de validar Basic Auth.
```

### Mantener la regla general en `SecurityConfig`

Deja esta regla en `SecurityConfig`:

```java
.requestMatchers(HttpMethod.PUT, "/tickets/by-id/**").hasAnyRole("USER", "AGENT", "ADMIN")
```

Esto produce dos niveles de autorización:

| Nivel | Pregunta | Ejemplo |
|-------|----------|---------|
| `SecurityConfig` | ¿Este rol puede intentar editar tickets? | USER, AGENT y ADMIN sí |
| `@PreAuthorize` | ¿Este usuario puede editar este ticket específico? | USER solo si lo creó; AGENT solo si lo tiene asignado |

---

## Paso 12: Tabla de autorización esperada

| Endpoint | Sin auth | USER | AGENT | ADMIN |
|----------|----------|------|-------|-------|
| `GET /tickets` | 200 | 200 | 200 | 200 |
| `GET /tickets/by-id/{id}` | 200 | 200 | 200 | 200 |
| `GET /tickets/{id}/history` | 401 | 403 | 403 | 200 |
| `POST /tickets` | 401 | 201 | 201 | 201 |
| `PUT /tickets/by-id/{id}` propio/permitido | 401 | 200 | 200 | 200 |
| `PUT /tickets/by-id/{id}` ajeno/no asignado | 401 | 403 | 403 | 200 |
| `DELETE /tickets/by-id/{id}` | 401 | 403 | 403 | 204 |
| `GET /users` | 200 | 200 | 200 | 200 |
| `POST /users` | 401 | 403 | 403 | 200/201 |

> Los códigos `200`, `201` y `204` pueden variar si falla una validación de negocio o si el recurso no existe. Lo importante para seguridad es distinguir `401` de `403`.

---

## Paso 13: Probar con Postman, Thunder Client o curl

### Generar Base64 en PowerShell

```powershell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("ana.garcia@empresa.com:user123"))
```

Resultado esperado:

```text
YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
```

### Generar Base64 en Linux / macOS

```bash
echo -n "ana.garcia@empresa.com:user123" | base64
```

### Credenciales disponibles

| Email | Contraseña | Rol | Base64 |
|-------|------------|-----|--------|
| `admin@empresa.com` | `pass123` | ADMIN | `YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw==` |
| `ana.garcia@empresa.com` | `user123` | USER | `YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz` |
| `carlos.lopez@empresa.com` | `user123` | AGENT | `Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=` |

### Casos mínimos

```bash
# 1. GET público: debe devolver 200
curl -i http://localhost:8080/ticket-app/tickets

# 2. POST sin autenticación: debe devolver 401
curl -i -X POST http://localhost:8080/ticket-app/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","description":"test"}'

# 3. POST con password incorrecta: debe devolver 401
curl -i -X POST http://localhost:8080/ticket-app/tickets \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTptYWxh" \
  -d '{"title":"Test","description":"test"}'

# 4. POST con USER válido: debe devolver 201 si el payload es válido
curl -i -X POST http://localhost:8080/ticket-app/tickets \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz" \
  -d '{"title":"Nuevo ticket","description":"descripcion"}'

# 5. DELETE con USER válido: debe devolver 403
curl -i -X DELETE http://localhost:8080/ticket-app/tickets/by-id/1 \
  -H "Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz"

# 6. DELETE con ADMIN válido: debe devolver 204 si el ticket existe
curl -i -X DELETE http://localhost:8080/ticket-app/tickets/by-id/1 \
  -H "Authorization: Basic YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw=="
```

### Casos de edición por propietario/asignado

Usa tickets existentes donde conozcas `createdBy.email` y `assignedTo.email`.

```bash
# USER edita un ticket creado por él: debe devolver 200 si el payload es válido
curl -i -X PUT http://localhost:8080/ticket-app/tickets/by-id/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz" \
  -d '{"title":"Ticket actualizado","description":"descripcion","status":"OPEN"}'

# USER edita un ticket creado por otro usuario: debe devolver 403
curl -i -X PUT http://localhost:8080/ticket-app/tickets/by-id/2 \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz" \
  -d '{"title":"Intento no permitido","description":"descripcion","status":"OPEN"}'

# AGENT edita un ticket asignado a él: debe devolver 200 si el payload es válido
curl -i -X PUT http://localhost:8080/ticket-app/tickets/by-id/3 \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=" \
  -d '{"title":"Ticket gestionado","description":"descripcion","status":"IN_PROGRESS"}'

# AGENT edita un ticket no asignado a él: debe devolver 403
curl -i -X PUT http://localhost:8080/ticket-app/tickets/by-id/4 \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=" \
  -d '{"title":"Intento no permitido","description":"descripcion","status":"IN_PROGRESS"}'
```

### Caso opcional: usuario inactivo

En la base de datos:

```sql
UPDATE users SET active = false WHERE email = 'ana.garcia@empresa.com';
```

Luego repite el POST con Ana. Debe responder `401 Unauthorized`.

---

## Resumen de pasos

```text
1. Agregar spring-boot-starter-security
        ↓
2. Observar la seguridad por defecto
        ↓
3. Preparar User: password, role, active
        ↓
4. Crear migraciones o actualizar DataInitializer
        ↓
5. Agregar UserRepository.findByEmail
        ↓
6. Declarar PasswordEncoder BCrypt
        ↓
7. Crear CustomUserDetailsService
        ↓
8. Crear SecurityConfig con httpBasic y STATELESS
        ↓
9. Entender BasicAuthenticationFilter y el flujo interno
        ↓
10. Aplicar @PreAuthorize donde corresponda
        ↓
11. Crear TicketSecurity para edición por propietario/asignado
        ↓
12. Probar 200, 201, 204, 401 y 403
```

---

*[← Volver a Objetivo](01_objetivo_y_alcance.md) | [Conceptos: Autenticación vs Autorización →](03_autenticacion_vs_autorizacion.md)*





<!-- START OF FILE: docs_lessons_16-spring-security_03_autenticacion_vs_autorizacion.md -->
# Documento: docs lessons 16-spring-security 03 autenticacion vs autorizacion
---
# Lección 16 — Autenticación vs Autorización

## La confusión más común en seguridad

- **Autenticación:** ¿Quién eres? → verifica identidad
- **Autorización:** ¿Qué puedes hacer? → verifica permisos

Son dos pasos **secuenciales**: primero se autentica, después se autoriza. No puedes autorizar a alguien que no se ha autenticado.

---

## Comparativa

| Aspecto | Autenticación | Autorización |
|--------|---------------|--------------|
| **Pregunta** | ¿Quién eres? | ¿Qué puedes hacer? |
| **Respuesta** | Usuario validado | Rol/permiso asignado |
| **Ejemplo** | Email `admin@empresa.com` + contraseña `pass123` correcta | Usuario ADMIN puede eliminar tickets |
| **Si falla** | `401 Unauthorized` | `403 Forbidden` |
| **En Spring Security** | `UserDetailsService` + `PasswordEncoder` | Reglas en `SecurityConfig` o `@PreAuthorize` |

---

## Cómo funciona HTTP Basic Auth

En cada petición, el cliente envía las credenciales en el header `Authorization`:

```
Authorization: Basic <base64(email:contraseña)>
```

Por ejemplo, para `admin@empresa.com:pass123`:
```
echo -n "admin@empresa.com:pass123" | base64
→ YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw==
```

Spring Security intercepta la petición con `BasicAuthenticationFilter`, decodifica el Base64, llama a `CustomUserDetailsService.loadUserByUsername(email)`, compara la contraseña con el hash BCrypt de la BD usando `PasswordEncoder.matches(...)` y, si coincide, construye un `Authentication` autenticado.

> **Nota de seguridad:** Base64 NO es cifrado. Las credenciales son recuperables si alguien intercepta el header. En producción, siempre usa HTTPS para que el header viaje cifrado.

---

## Arquitectura: el Security Filter Chain

Spring Security funciona como una **cadena de filtros** que intercepta cada petición HTTP antes de que llegue al controlador.

```
Petición HTTP
      │
      ▼
┌─────────────────────────────────────────────────────┐
│               Security Filter Chain                 │
│                                                     │
│  1. SecurityContextHolderFilter                     │
│     └─ Prepara el contexto de seguridad             │
│                                                     │
│  2. BasicAuthenticationFilter                       │
│     └─ Lee el header Authorization: Basic ...       │
│     └─ Llama a CustomUserDetailsService             │
│     └─ Verifica contraseña con BCryptPasswordEncoder│
│     └─ Si es válido → guarda Authentication         │
│                                                     │
│  3. ExceptionTranslationFilter                      │
│     └─ Captura errores de autenticación → 401       │
│     └─ Captura errores de autorización → 403        │
│                                                     │
│  4. AuthorizationFilter                             │
│     └─ Verifica las reglas de tu SecurityConfig     │
│     └─ .permitAll() → pasa sin autenticación        │
│     └─ .hasRole("ADMIN") → verifica el rol          │
│                                                     │
└─────────────────────────────────────────────────────┘
      │
      ▼ (si todos los filtros pasan)
   Controlador Spring MVC
      │
      ▼
   Respuesta HTTP
```

**Puntos clave de este flujo:**
1. Los filtros se ejecutan en orden, antes de que tu código de controlador corra
2. Si un filtro rechaza la petición, los siguientes filtros **no se ejecutan**
3. Tu `SecurityConfig` activa filtros como `BasicAuthenticationFilter` con `httpBasic(...)` y define reglas que aplicará `AuthorizationFilter`

---

## Flujo interno con HTTP Basic

Este es el camino completo de una petición protegida:

```text
Cliente
  Authorization: Basic base64(email:password)
        |
        v
BasicAuthenticationFilter
        |
        v
AuthenticationManager
        |
        v
DaoAuthenticationProvider
        |
        v
CustomUserDetailsService.loadUserByUsername(email)
        |
        v
PasswordEncoder.matches(passwordPlano, hashBCrypt)
        |
        v
SecurityContext: Authentication autenticado con ROLE_...
        |
        v
AuthorizationFilter / @PreAuthorize
        |
        v
Controlador
```

Responsabilidades principales:

- `BasicAuthenticationFilter`: captura el header `Authorization: Basic ...`. Este filtro lo aporta Spring Security; no se implementa manualmente.
- `CustomUserDetailsService`: carga el usuario desde `UserRepository.findByEmail(email)`.
- `PasswordEncoder`: compara la contraseña enviada contra el hash BCrypt guardado.
- `SecurityContext`: guarda el usuario autenticado durante la petición actual.
- `AuthorizationFilter`: aplica las reglas de `SecurityConfig`.
- `@PreAuthorize`: aplica reglas a nivel de método cuando `@EnableMethodSecurity` está habilitado.

Respuestas esperadas:

- `401 Unauthorized`: no se pudo autenticar. Falta header, el header es inválido, el usuario no existe, la contraseña es incorrecta o el usuario está inactivo.
- `403 Forbidden`: el usuario sí se autenticó, pero su rol no tiene permiso para ejecutar el endpoint.

---

## STATELESS vs STATEFUL

### STATEFUL (sesiones HTTP — no lo que usamos aquí)

En aplicaciones web tradicionales, el servidor guarda la sesión del usuario:

```
Login:
  Cliente → POST /login (usuario:contraseña)
  Servidor → crea Session ID → guarda en memoria
  Servidor → devuelve Cookie: JSESSIONID=abc123

Petición posterior:
  Cliente → GET /dashboard (Cookie: JSESSIONID=abc123)
  Servidor → busca la sesión abc123 → el usuario está autenticado ✅
```

**Problema para APIs REST:** Las cookies de sesión no funcionan bien con clientes móviles, microservicios, ni con múltiples instancias del servidor.

### STATELESS (lo que usamos en esta lección)

Con `SessionCreationPolicy.STATELESS`, el servidor **nunca guarda estado**:

```
Cada petición:
  Cliente → GET /tickets (Authorization: Basic YWRtaW5...)
  Servidor → verifica el header Authorization → autentica → responde
  (No guarda nada en memoria)

Siguiente petición:
  Cliente → POST /tickets (Authorization: Basic YWRtaW5...)
  Servidor → verifica el header Authorization otra vez → autentica → responde
```

**Ventajas para APIs REST:**
- Funciona igual con cualquier cliente (web, móvil, Postman, microservicio)
- No hay problemas al escalar horizontalmente (múltiples instancias)
- Más fácil de testear

---

## Flujo completo: autenticación + autorización

```mermaid
sequenceDiagram
    participant C as Cliente
    participant F as Security Filter Chain
    participant S as CustomUserDetailsService
    participant DB as Base de Datos
    participant CTRL as Controlador

    rect rgb(240, 248, 255)
        Note over C,DB: 1. AUTENTICACIÓN
        C->>F: POST /tickets<br/>Authorization: Basic YW5h...
        F->>F: Decodifica Base64<br/>→ ana.garcia@empresa.com:user123
        F->>S: loadUserByUsername("ana.garcia@empresa.com")
        S->>DB: SELECT * FROM users WHERE email = ?
        DB-->>S: {email, password=$2a$10$..., role=USER}
        S-->>F: UserDetails con ROLE_USER y hash BCrypt
        F->>F: encoder.matches("user123", hashBD) → true ✅
    end

    rect rgb(255, 248, 240)
        Note over C,CTRL: 2. AUTORIZACIÓN
        F->>F: ¿Tiene permiso para POST /tickets?
        F->>F: Regla: hasAnyRole("USER", "AGENT", "ADMIN")
        F->>F: Usuario tiene ROLE_USER → ✅ permitido
        F->>CTRL: Ejecuta el método del controlador
        CTRL-->>C: 201 Created
    end
```

---

## Roles en Spring Security

Spring Security usa el prefijo `ROLE_` internamente. En la implementación base de esta lección usamos `.roles(...)` para entregar el rol sin prefijo y dejar que Spring agregue `ROLE_` automáticamente:

```java
// Al construir UserDetails (en CustomUserDetailsService)
return org.springframework.security.core.userdetails.User
    .withUsername(user.getEmail())
    .password(user.getPassword())
    .roles(user.getRole().name())
    .disabled(!user.isActive())
    .build();

// En SecurityConfig (Spring agrega ROLE_ automáticamente)
.hasRole("ADMIN")        // Busca "ROLE_ADMIN" en el UserDetails
.hasAnyRole("USER", "AGENT", "ADMIN")  // Busca cualquiera de los tres

// En anotaciones de controladores
@PreAuthorize("hasRole('ADMIN')")       // Busca "ROLE_ADMIN"
@PreAuthorize("hasAnyRole('USER','AGENT','ADMIN')")
```

También existe esta alternativa:

```java
.authorities("ROLE_" + user.getRole().name())
```

> **Regla práctica:** en `.roles(...)`, `.hasRole(...)` y `@PreAuthorize("hasRole(...)")`, escribe el rol **sin** el prefijo `ROLE_`. Si usas `.authorities(...)` o `hasAuthority(...)`, escribe el valor completo, por ejemplo `ROLE_ADMIN`.

---

## Autorización por datos del recurso

Algunas reglas no dependen solo del rol. También dependen del recurso que se quiere modificar.

Ejemplo para editar tickets:

| Rol | Regla |
|-----|-------|
| `USER` | Puede editar solo tickets donde `ticket.createdBy.email` sea igual a `authentication.name` |
| `AGENT` | Puede editar solo tickets donde `ticket.assignedTo.email` sea igual a `authentication.name` |
| `ADMIN` | Puede editar cualquier ticket |

Esta autorización se conoce como autorización por recurso, por propiedad o por datos. `hasRole('USER')` no alcanza, porque solo responde si el usuario tiene el rol, pero no sabe si el ticket fue creado por él.

Para estos casos se usa un bean invocado desde `@PreAuthorize`:

```java
@PutMapping("/by-id/{id}")
@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")
public ResponseEntity<Object> updateTicketById(
    @PathVariable Long id,
    @Valid @RequestBody TicketRequest request) {
    // ...
}
```

La regla queda dividida en dos niveles:

| Nivel | Pregunta | Herramienta |
|-------|----------|-------------|
| Ruta y rol | ¿Este rol puede intentar editar tickets? | `SecurityConfig` con `hasAnyRole(...)` |
| Recurso específico | ¿Este usuario puede editar este ticket? | `@PreAuthorize` con `@ticketSecurity.canEdit(...)` |

Si el usuario está autenticado pero intenta editar un ticket ajeno o no asignado, Spring Security debe responder `403 Forbidden`.

---

## Escenarios reales

### Escenario 1: Crear ticket

```
GET  /tickets/by-id/1          → Público ✅ (sin auth → 200 OK)
POST /tickets (sin auth)        → ❌ 401 Unauthorized
POST /tickets (USER)            → ✅ ROLE_USER puede crear → 201 Created
POST /tickets (AGENT)           → ✅ ROLE_AGENT puede crear → 201 Created
POST /tickets (ADMIN)           → ✅ ROLE_ADMIN puede crear → 201 Created
```

### Escenario 2: Eliminar ticket

```
DELETE /tickets/by-id/1 (sin auth) → ❌ 401 Unauthorized
DELETE /tickets/by-id/1 (USER)     → ❌ ROLE_USER no puede eliminar → 403 Forbidden
DELETE /tickets/by-id/1 (AGENT)    → ❌ ROLE_AGENT no puede eliminar → 403 Forbidden
DELETE /tickets/by-id/1 (ADMIN)    → ✅ ROLE_ADMIN puede eliminar → 204 No Content
```

### Escenario 3: Editar ticket

```
PUT /tickets/by-id/1 (sin auth)             → ❌ 401 Unauthorized
PUT /tickets/by-id/1 (USER creador)         → ✅ ROLE_USER puede editar → 200 OK
PUT /tickets/by-id/1 (USER no creador)      → ❌ ROLE_USER autenticado, pero no es dueño → 403 Forbidden
PUT /tickets/by-id/1 (AGENT asignado)       → ✅ ROLE_AGENT puede editar → 200 OK
PUT /tickets/by-id/1 (AGENT no asignado)    → ❌ ROLE_AGENT autenticado, pero no está asignado → 403 Forbidden
PUT /tickets/by-id/1 (ADMIN)                → ✅ ROLE_ADMIN puede editar → 200 OK
```

### Escenario 4: Gestionar categorías

```
GET    /categories (sin auth)    → ✅ Público → 200 OK
POST   /categories (sin auth)    → ❌ 401 Unauthorized
POST   /categories (USER)        → ❌ 403 Forbidden (no tiene permiso)
POST   /categories (AGENT)       → ❌ 403 Forbidden (no tiene permiso)
POST   /categories (ADMIN)       → ✅ 201 Created
```

---

## Tabla completa de permisos

| Endpoint | Método | Sin auth | USER | AGENT | ADMIN |
|----------|--------|:--------:|:----:|:-----:|:-----:|
| `/tickets` | GET | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| `/tickets/by-id/{id}` | GET | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| `/tickets` | POST | ❌ 401 | ✅ 201 | ✅ 201 | ✅ 201 |
| `/tickets/by-id/{id}` propio/asignado | PUT | ❌ 401 | ✅ 200 | ✅ 200 | ✅ 200 |
| `/tickets/by-id/{id}` ajeno/no asignado | PUT | ❌ 401 | ❌ 403 | ❌ 403 | ✅ 200 |
| `/tickets/by-id/{id}` | DELETE | ❌ 401 | ❌ 403 | ❌ 403 | ✅ 204 |
| `/tickets/{id}/history` | GET | ❌ 401 | ❌ 403 | ❌ 403 | ✅ 200 |
| `/categories` | GET | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| `/categories` | POST | ❌ 401 | ❌ 403 | ❌ 403 | ✅ 201 |
| `/categories/by-id/{id}` | PUT/DELETE | ❌ 401 | ❌ 403 | ❌ 403 | ✅ 200/204 |





<!-- START OF FILE: docs_lessons_16-spring-security_04_ejemplos_practicos.md -->
# Documento: docs lessons 16-spring-security 04 ejemplos practicos
---
# Lección 16 — Ejemplos Avanzados

Esta sección contiene código adicional que extiende la implementación base. No es obligatorio para la actividad, pero es útil para entender el potencial de Spring Security.

---

## 1. Obtener el usuario autenticado en un controlador

Muchas veces necesitas saber **quién está haciendo la petición** dentro del método del controlador. Puedes inyectar el `Principal` o el `Authentication` directamente:

```java
import org.springframework.security.core.Authentication;

@GetMapping("/auth/me")
public ResponseEntity<Map<String, String>> whoAmI(Authentication authentication) {
    Map<String, String> info = new HashMap<>();
    info.put("email", authentication.getName());
    info.put("role", authentication.getAuthorities().iterator().next().getAuthority());
    return ResponseEntity.ok(info);
}
```

**Ejemplo de respuesta:**
```json
{
    "email": "ana.garcia@empresa.com",
    "role": "ROLE_USER"
}
```

También puedes obtener el usuario completo de la BD inyectando `UserRepository`:

```java
@GetMapping("/auth/me")
public ResponseEntity<User> whoAmI(Authentication authentication) {
    String email = authentication.getName();
    return userRepository.findByEmail(email)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

> **¿Cuándo usar esto?** Cuando un usuario solo debe ver sus propios tickets, o cuando quieres registrar quién realizó una acción.

---

## 2. Autorización con `@PreAuthorize` en el controlador

En lugar de (o además de) las reglas en `SecurityConfig`, puedes anotar métodos individuales del controlador:

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    // Solo ADMIN puede ver el historial
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketHistory>> getHistory(@PathVariable Long id) {
        // ...
    }

    // USER, AGENT o ADMIN pueden crear tickets
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<String> create(@Valid @RequestBody TicketRequest dto) {
        // ...
    }
}
```

**Expresiones SpEL disponibles en `@PreAuthorize`:**

| Expresión | Descripción |
|-----------|-------------|
| `hasRole('ADMIN')` | Tiene el rol ADMIN |
| `hasAnyRole('USER', 'AGENT')` | Tiene cualquiera de los roles |
| `isAuthenticated()` | Está autenticado (cualquier rol) |
| `isAnonymous()` | No está autenticado |
| `authentication.name == 'admin@empresa.com'` | Es exactamente este usuario |
| `#id == authentication.principal.id` | El parámetro `id` coincide con el id del usuario autenticado |

> **`SecurityConfig` vs `@PreAuthorize`:** Para proyectos simples, las reglas en `SecurityConfig` son suficientes. `@PreAuthorize` es mejor cuando las condiciones dependen de los datos de la petición (ej: solo el creador puede editar su propio ticket).

---

## 3. Autorización por recurso con un bean de seguridad

Cuando la autorización depende del ticket específico, delega la decisión a un bean llamado desde `@PreAuthorize`.

Regla de ejemplo:

- `USER`: solo edita tickets creados por él.
- `AGENT`: solo edita tickets asignados a él.
- `ADMIN`: edita cualquier ticket.

```java
import cl.duoc.fullstack.tickets.model.Ticket;
import cl.duoc.fullstack.tickets.respository.TicketRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("ticketSecurity")
public class TicketSecurity {

    private final TicketRepository ticketRepository;

    public TicketSecurity(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public boolean canEdit(Long ticketId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }

        String email = authentication.getName();

        return ticketRepository.findById(ticketId)
            .map(ticket -> canEditTicket(ticket, email, authentication))
            .orElse(false);
    }

    private boolean canEditTicket(Ticket ticket, String email, Authentication authentication) {
        if (hasRole(authentication, "ROLE_USER")) {
            return ticket.getCreatedBy() != null
                && email.equals(ticket.getCreatedBy().getEmail());
        }

        if (hasRole(authentication, "ROLE_AGENT")) {
            return ticket.getAssignedTo() != null
                && email.equals(ticket.getAssignedTo().getEmail());
        }

        return false;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
```

Uso en el controlador:

```java
@PutMapping("/by-id/{id}")
@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")
public ResponseEntity<Object> updateTicketById(
    @PathVariable Long id,
    @Valid @RequestBody TicketRequest request) {
    // ...
}
```

> **Por qué usar un bean:** `hasRole('USER')` solo sabe el rol. `TicketSecurity` puede consultar el ticket y comparar `createdBy.email` o `assignedTo.email` con `authentication.name`.

---

## 4. Alternativa: usar `authorities(...)` en lugar de `roles(...)`

La implementación base del guion usa `.roles(user.getRole().name())`, que agrega automáticamente el prefijo `ROLE_`. También puedes construir la authority completa manualmente:

```java
@Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
        .filter(u -> u.getPassword() != null && !u.getPassword().isBlank())
        .orElseThrow(() -> new UsernameNotFoundException(
            "Usuario no encontrado o sin contraseña: " + email));

    GrantedAuthority authority = () -> "ROLE_" + user.getRole().name();

    return org.springframework.security.core.userdetails.User
        .withUsername(user.getEmail())
        .password(user.getPassword())
        .authorities(authority)
        .disabled(!user.isActive())
        .build();
}
```

Si usas esta alternativa, recuerda que `hasRole("ADMIN")` busca `ROLE_ADMIN`. Por eso la authority debe incluir el prefijo `ROLE_`.

---

## 5. Configurar CORS para desarrollo con frontend

Si tienes un frontend (React, Angular, etc.) corriendo en `localhost:3000`, necesitas habilitar CORS en `SecurityConfig`:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // ← Agrega esto
        .csrf(csrf -> csrf.disable())
        // ... resto de la configuración igual
        ;
    return http.build();
}

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

**Imports necesarios:**
```java
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
```

---

## 6. Generar hashes BCrypt manualmente

Si necesitas generar nuevos hashes para las migraciones, puedes hacerlo con una clase main temporal:

```java
// Crea este archivo temporalmente, ejecuta, copia los hashes y luego bórralo
public class GenerateHashes {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("user456:  " + encoder.encode("user456"));
    }
}
```

**Salida de ejemplo:**
```
admin123: $2a$10$5Cx.xR2y7...Qz3KaB1mP
user456:  $2a$10$9Dw.qR4z8...Rp4LcC2nQ
```

Copia esos hashes directamente en el SQL de la migración.

> **Importante:** Cada ejecución produce hashes diferentes (por el salt aleatorio), pero todos son válidos. Elige uno y ponlo en la migración.

---

## 7. Probar la seguridad con `curl`

```bash
# Test 1: GET público (debe funcionar sin auth)
curl -i http://localhost:8080/ticket-app/tickets

# Test 2: POST sin auth (debe devolver 401)
curl -i -X POST http://localhost:8080/ticket-app/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","description":"test"}'

# Test 3: POST con credenciales USER
curl -i -X POST http://localhost:8080/ticket-app/tickets \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz" \
  -d '{"title":"Nuevo ticket","description":"descripcion"}'

# Test 4: DELETE con ADMIN
curl -i -X DELETE http://localhost:8080/ticket-app/tickets/by-id/1 \
  -H "Authorization: Basic YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw=="

# Generar el Base64 de cualquier credencial
echo -n "mi.email@empresa.com:micontraseña" | base64
```





<!-- START OF FILE: docs_lessons_16-spring-security_05_password_encoding.md -->
# Documento: docs lessons 16-spring-security 05 password encoding
---
# Lección 16 — Cifrado de Contraseñas con BCrypt

## ¿Por qué no guardar contraseñas en texto plano?

```
Base de datos filtrada (texto plano):
  admin@empresa.com  → pass123   ← atacante puede entrar inmediatamente
  ana@empresa.com    → user123   ← y en todos los otros sistemas donde usen la misma contraseña
```

```
Base de datos filtrada (BCrypt):
  admin@empresa.com  → $2a$10$gT.PsFi3xTq9xc3virQAf...  ← inútil sin la contraseña original
  ana@empresa.com    → $2a$10$LAK58ME84bgotvy2eL.eWe...  ← requiere fuerza bruta por cada usuario
```

**Solución:** Usar hashing irreversible con salt aleatorio.

---

## ¿Qué es un hash de contraseña?

Un hash es una función de una sola vía:

```
"pass123"  →  función hash  →  "$2a$10$gT.PsFi3xTq9xc3virQAf..."
                     ↑
          No existe función inversa
```

No puedes recuperar `"pass123"` a partir del hash. Lo único que puedes hacer es verificar si una contraseña candidata produce el mismo hash.

---

## BCryptPasswordEncoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();  // cost factor 10 por defecto
}
```

### Cómo funciona

```java
PasswordEncoder encoder = new BCryptPasswordEncoder(10);  // cost factor = 10

// Al registrar/seedear un usuario (operación lenta, ~100ms)
String hash = encoder.encode("pass123");
// → $2a$10$gT.PsFi3xTq9xc3virQAfesYBesY5g53tQ5R7lgJGqgVdVMH0I8qa

// Llama otra vez con la misma contraseña → hash DIFERENTE (salt aleatorio)
String otrohash = encoder.encode("pass123");
// → $2a$10$SomeOtherHashBecauseTheSaltIsRandom...

// Al verificar en cada login (operación lenta, ~100ms)
boolean ok = encoder.matches("pass123", hash);  // true
boolean ko = encoder.matches("malpass", hash);  // false
```

### Anatomía de un hash BCrypt

```
$2a$10$gT.PsFi3xTq9xc3virQAfesYBesY5g53tQ5R7lgJGqgVdVMH0I8qa
 │   │  │                  │                               │
 │   │  └─ Salt (22 chars) └─ Hash de la contraseña (31 chars)
 │   └─ Cost factor: 2^10 = 1024 iteraciones
 └─ Versión del algoritmo BCrypt
```

---

## El cost factor: velocidad vs seguridad

BCrypt es **intencionalmente lento**. El cost factor controla cuántas iteraciones realiza:

| Cost | Iteraciones | Tiempo (~) | Uso |
|------|-------------|------------|-----|
| 4 | 16 | < 1ms | Tests automáticos |
| 10 | 1,024 | ~100ms | **Producción (defecto)** |
| 12 | 4,096 | ~400ms | Alta seguridad |
| 14 | 16,384 | ~1.5s | Máxima seguridad (lento para el usuario) |

**¿Por qué queremos que sea lento?**

Un atacante que roba la BD con hashes BCrypt no puede hacer fuerza bruta eficientemente:
- Con SHA-256 (rápido): puede probar ~10,000,000,000 contraseñas por segundo
- Con BCrypt cost=10: puede probar ~100 contraseñas por segundo

Para el usuario final, 100ms por login es imperceptible. Para un atacante, probar millones de contraseñas toma años.

---

## El salt: por qué cada hash es diferente

El salt es un valor aleatorio que se agrega a la contraseña antes de hashear:

```
Sin salt:
  "pass123" → siempre produce el mismo hash
  Si dos usuarios tienen la misma contraseña → mismo hash en la BD
  → Un atacante puede usar tablas arcoíris (rainbow tables) precomputadas

Con salt (BCrypt):
  "pass123" + salt1 → hash1
  "pass123" + salt2 → hash2 (completamente diferente)
  El salt se guarda dentro del propio hash ($2a$10$SALT_AQUI...)
  → Las rainbow tables no funcionan porque cada hash tiene su propio salt
```

**Consecuencia práctica:** No puedes comparar dos hashes BCrypt directamente. Solo puedes verificar con `encoder.matches(password, hash)`.

---

## Flujo completo de verificación en Spring Security

```
Cliente envía:
  Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz

Spring Security:
  1. Decodifica Base64
     → email = "ana.garcia@empresa.com", password = "user123"

  2. Llama a CustomUserDetailsService.loadUserByUsername("ana.garcia@empresa.com")
     → Consulta la BD → devuelve UserDetails con hash BCrypt

  3. Compara con BCryptPasswordEncoder.matches("user123", hashDeLaBD)
     → true → autentica al usuario
     → false → devuelve 401 Unauthorized

(Spring Security hace la comparación automáticamente.
 Tú nunca llamas a encoder.matches() manualmente para el login.)
```

---

## Comparativa de algoritmos

| Algoritmo | Seguridad | Velocidad | ¿Usar en producción? |
|-----------|-----------|-----------|----------------------|
| Texto plano | ❌ Crítico | ⚡⚡⚡ | ❌ Nunca |
| MD5 | ❌ Roto | ⚡⚡⚡ | ❌ Nunca |
| SHA-1 | ❌ Roto | ⚡⚡⚡ | ❌ Nunca |
| SHA-256 sin salt | ⚠️ Vulnerable | ⚡⚡⚡ | ❌ No (rainbow tables) |
| SHA-256 + salt | ⚠️ Aceptable | ⚡⚡ | ⚠️ Solo si BCrypt no está disponible |
| **BCrypt** | ✅ Excelente | 🐢 Lento intencional | ✅ **SÍ (esta lección)** |
| Argon2 | ✅✅ Superior | 🐢🐢 Muy lento | ✅ Para máxima seguridad |

**Recomendación:** BCrypt para proyectos Spring Boot estándar. Es el estándar de la industria y Spring Security lo soporta nativamente.

---

## Reglas de oro

1. **Nunca** guardes contraseñas en texto plano — ni en la BD, ni en logs, ni en variables de entorno
2. **Nunca** construyas tu propio algoritmo de hashing — usa BCrypt o Argon2
3. **Siempre** usa `encoder.matches()` para comparar — nunca `hashAlmacenado.equals(hashNuevo)`
4. **Siempre** hashea en el backend — nunca confíes en un hash enviado desde el cliente
5. **Nunca** pongas el `PasswordEncoder` en el `CustomUserDetailsService` — ponlo en `SecurityConfig` como `@Bean` para evitar dependencias circulares





<!-- START OF FILE: docs_lessons_16-spring-security_06_troubleshooting.md -->
# Documento: docs lessons 16-spring-security 06 troubleshooting
---
# Lección 16 — Troubleshooting

Errores más frecuentes al implementar Spring Security con base de datos.

---

## Error 1: `401 Unauthorized` aunque las credenciales son correctas

**Síntoma:** Envías el header `Authorization: Basic ...` pero recibes 401.

**Causa más común:** El Base64 está mal generado.

**Diagnóstico:**
```bash
# Genera el Base64 correcto en la terminal
echo -n "admin@empresa.com:pass123" | base64
# → YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw==

# Si tu sistema añade un salto de línea al final, el Base64 será incorrecto.
# El flag -n evita eso. En Windows (PowerShell):
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("admin@empresa.com:pass123"))
```

**Otras causas:**
1. La contraseña en la BD no corresponde al hash BCrypt del texto que usas
2. El email no existe en la BD
3. El campo `password` del usuario es `null` o vacío
4. La migración V6 no se ejecutó y los usuarios no tienen contraseña
5. `active=false`, por lo que Spring Security considera el usuario deshabilitado
6. Falta `UserRepository.findByEmail(email)` o no está siendo usado por `CustomUserDetailsService`

**Verificación:**
```sql
-- Comprueba que los usuarios tienen password en la BD
SELECT email, role, active, LEFT(password, 20) AS password_preview FROM users;
-- Debe mostrar: $2a$10$... en la columna password
```

---

## Error 2: `403 Forbidden` aunque estoy autenticado

**Síntoma:** El login funciona (no es 401), pero el endpoint devuelve 403.

**Causa:** El rol del usuario no tiene permiso para ese endpoint.

**Diagnóstico:**
```bash
# Verifica el rol del usuario con:
# GET /auth/me (si lo implementaste) o consultando la BD directamente:
SELECT email, role FROM users WHERE email = 'ana.garcia@empresa.com';
```

**Verifica en `SecurityConfig`:**
```java
// ¿Está el rol del usuario en la regla correcta?
.requestMatchers(HttpMethod.DELETE, "/tickets/by-id/**").hasRole("ADMIN")
// → USER y AGENT reciben 403. Solo ADMIN puede eliminar.
```

**Verifica cómo construyes los roles:**
```java
// Opción recomendada en el guion: Spring agrega ROLE_ automáticamente.
.roles(user.getRole().name())

// Alternativa válida: tú agregas ROLE_ manualmente.
.authorities("ROLE_" + user.getRole().name())

// Incorrecto si luego usas hasRole("ADMIN"):
.authorities(user.getRole().name())
```

---

## Error 3: El compilador falla con `user.getRole()` — tipo incorrecto

**Síntoma:** Error de compilación en `CustomUserDetailsService`:
```
error: incompatible types: Role cannot be converted to String
```

**Causa:** El campo `role` en `User` es un **enum** (`Role`), no un `String`.

**Solución:** Llamar a `.name()` para convertir el enum a String:
```java
// ❌ INCORRECTO
.roles(user.getRole())

// ✅ CORRECTO
.roles(user.getRole().name())
// user.getRole() → Role.ADMIN (enum)
// .name()         → "ADMIN"  (String)
```

---

## Error 4: Los endpoints GET también requieren autenticación

**Síntoma:** `GET /tickets` devuelve 401 en lugar de 200.

**Causa:** Falta la regla `permitAll()` en `SecurityConfig`, o las reglas están en el orden incorrecto.

**Solución:**
```java
// En filterChain(), las reglas se evalúan en orden. El GET debe ir ANTES de anyRequest():
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/tickets", "/tickets/by-id/**").permitAll()  // ← PRIMERO
    .requestMatchers(HttpMethod.GET, "/categories", "/categories/by-id/**").permitAll()
    // ... otras reglas ...
    .anyRequest().authenticated()   // ← SIEMPRE al final
);
```

> Si `.anyRequest().authenticated()` estuviera antes de las reglas `permitAll()`, Spring Security nunca llegaría a evaluarlas.

---

## Error 5: `NullPointerException` en `loadUserByUsername`

**Síntoma:**
```
java.lang.NullPointerException: Cannot invoke "cl.duoc.fullstack.tickets.model.User$Role.name()"
because the return value of "User.getRole()" is null
```

**Causa:** Un usuario en la BD tiene el campo `role` como `NULL`.

**Solución:**
```sql
-- Verifica usuarios sin rol asignado
SELECT id, email, role FROM users WHERE role IS NULL;

-- Corrige con un UPDATE
UPDATE users SET role = 'USER' WHERE role IS NULL;
```

También puedes agregar el filtro en el servicio:
```java
User user = userRepository.findByEmail(email)
    .filter(u -> u.getPassword() != null && !u.getPassword().isBlank())
    .filter(u -> u.getRole() != null)   // ← Previene NullPointerException
    .orElseThrow(() -> new UsernameNotFoundException("..."));
```

---

## Error 6: Spring Security no encontró `UserDetailsService`

**Síntoma:**
```
java.lang.IllegalStateException: No AuthenticationProvider found for
org.springframework.security.authentication.UsernamePasswordAuthenticationToken
```

**Causa:** `CustomUserDetailsService` no está siendo detectado por Spring.

**Verificación:**
1. ¿Tiene la anotación `@Service`?
   ```java
   @Service   // ← obligatorio
   public class CustomUserDetailsService implements UserDetailsService {
   ```
2. ¿Está en un paquete que Spring escanea? El paquete `config` dentro del paquete base de la aplicación está escaneado automáticamente.
3. ¿Hay más de una clase que implementa `UserDetailsService`? Spring no sabe cuál usar.

---

## Error 7: `@PreAuthorize` no funciona (siempre pasa o siempre falla)

**Síntoma:** La anotación `@PreAuthorize("hasRole('ADMIN')")` es ignorada.

**Causa:** Falta `@EnableMethodSecurity` en `SecurityConfig`.

**Solución:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ← Agrega esto para habilitar @PreAuthorize
public class SecurityConfig {
    // ...
}
```

---

## Error 8: Rutas con context-path no matchean

**Síntoma:** Las reglas de `SecurityConfig` no se aplican; todos los endpoints quedan sin protección o todos quedan bloqueados.

**Causa:** Incluir el context-path (`/ticket-app`) en las rutas de `requestMatchers`.

**Solución:** Los `requestMatchers` se escriben **sin** el context-path. Spring Security evalúa la ruta después de que el context-path es procesado:

```java
// ❌ INCORRECTO: incluye el context-path
.requestMatchers(HttpMethod.GET, "/ticket-app/tickets").permitAll()

// ✅ CORRECTO: solo la ruta del endpoint
.requestMatchers(HttpMethod.GET, "/tickets", "/tickets/by-id/**").permitAll()
```

El `context-path` se configura en `application.yml`:
```yaml
server:
  servlet:
    context-path: /ticket-app  # Spring Security no lo ve en requestMatchers
```

---

## Error 9: POST devuelve 403 aunque tengo rol correcto (CSRF)

**Síntoma:** GET funciona con autenticación, pero POST/PUT/DELETE devuelve 403 con el cuerpo:
```json
{"error": "Forbidden", "message": "...CSRF..."}
```

**Causa:** La protección CSRF está habilitada (es el comportamiento por defecto).

**Solución:** Deshabilitarla explícitamente en `SecurityConfig`:
```java
http
    .csrf(csrf -> csrf.disable())   // ← Agregar esto
    // ... resto de la configuración
```

> **¿Por qué deshabilitamos CSRF?** Las API REST STATELESS no usan cookies de sesión, por lo que la protección CSRF no aplica. CSRF solo es necesario en aplicaciones web que usan sesiones (formularios HTML).

---

## Error 10: `DataInitializer` duplica los datos en MySQL

**Síntoma:** La BD MySQL tiene el doble de usuarios esperados — los de la migración V6 y los del `DataInitializer`.

**Causa:** `DataInitializer` no tiene `@Profile("h2")` y se ejecuta en todos los perfiles.

**Solución:**
```java
@Component
@Profile("h2")   // ← Solo para H2; MySQL y Supabase usan las migraciones Flyway
public class DataInitializer implements CommandLineRunner {
    // ...
}
```

Después de agregar `@Profile("h2")`, con perfil MySQL el `DataInitializer` no se ejecuta y los datos solo vienen de las migraciones V5 y V6.

---

## Error 11: Error de dependencia circular con `PasswordEncoder`

**Síntoma:** La aplicación no arranca y muestra un error de ciclo de dependencias entre configuración, servicios y seguridad.

**Causa común:** Crear o inyectar dependencias de seguridad en lugares incorrectos, por ejemplo intentar construir el encoder dentro de `CustomUserDetailsService` o declarar beans duplicados.

**Solución:** Declara un único bean `PasswordEncoder`, normalmente en `SecurityConfig`, e inyéctalo solo donde necesites generar hashes, como `DataInitializer`.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

`CustomUserDetailsService` no necesita inyectar `PasswordEncoder`. Solo debe cargar el usuario y entregar a Spring Security el hash guardado.

---

## Error 12: H2 funciona, pero MySQL/PostgreSQL no autentica

**Síntoma:** Con H2 puedes autenticar, pero con otro perfil recibes `401`.

**Causas probables:**

1. Flyway no ejecutó la migración que agrega `password`.
2. Los usuarios de la BD externa no tienen hash BCrypt.
3. El seed de datos de H2 no existe en MySQL/PostgreSQL.
4. Estás probando contra una base de datos antigua sin la columna `active` o sin roles válidos.

**Verificación:**

```sql
SELECT email, role, active, password FROM users;
```

La columna `password` debe contener valores que comiencen con `$2a$`, `$2b$` o `$2y$`.

---

## Error 13: `@ticketSecurity` no se encuentra

**Síntoma:** La aplicación falla al evaluar `@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")`.

**Causa:** El bean no existe con el nombre `ticketSecurity` o está fuera del paquete escaneado por Spring.

**Solución:** Declara el componente con nombre explícito dentro del paquete base de la aplicación:

```java
@Component("ticketSecurity")
public class TicketSecurity {
    // ...
}
```

---

## Error 14: USER o AGENT siempre reciben `403` al editar

**Síntoma:** El usuario está autenticado, tiene rol `USER` o `AGENT`, pero `PUT /tickets/by-id/{id}` siempre devuelve `403`.

**Causas probables:**

1. El ticket no existe y `ticketRepository.findById(id)` retorna vacío.
2. El ticket no tiene `createdBy` o `assignedTo`.
3. `authentication.getName()` no coincide con el email guardado en `createdBy.email` o `assignedTo.email`.
4. El usuario tiene una authority mal construida, por ejemplo `USER` en vez de `ROLE_USER`.
5. El `@PathVariable` se llama distinto y la expresión SpEL usa `#id`.

**Verificación:**

```sql
SELECT t.id, creator.email AS created_by, agent.email AS assigned_to
FROM tickets t
LEFT JOIN users creator ON creator.id = t.created_by_id
LEFT JOIN users agent ON agent.id = t.assigned_to_id
WHERE t.id = 1;
```

Y revisa que el método tenga el mismo nombre de parámetro usado por SpEL:

```java
@PutMapping("/by-id/{id}")
@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")
public ResponseEntity<Object> updateTicketById(@PathVariable Long id, ...) {
    // ...
}
```

---

## Error 15: ADMIN recibe `403` al editar tickets

**Síntoma:** Un usuario ADMIN autenticado no puede editar tickets.

**Causa:** `TicketSecurity` no está reconociendo la authority `ROLE_ADMIN`.

**Solución:** Verifica que `CustomUserDetailsService` construya roles con `.roles(user.getRole().name())` o authorities con el prefijo completo:

```java
.roles(user.getRole().name())
```

También verifica que el usuario admin tenga `role = 'ADMIN'` en la base de datos.





<!-- START OF FILE: docs_lessons_16-spring-security_07_checklist_rubrica_minima.md -->
# Documento: docs lessons 16-spring-security 07 checklist rubrica minima
---
# Lección 16 — Checklist y Rúbrica Mínima

## ✅ Checklist antes de entregar

### Dependencia y compilación
- [ ] `pom.xml` incluye `spring-boot-starter-security`
- [ ] El proyecto compila sin errores (`mvnw.cmd clean install`)

### Entidad y Base de Datos
- [ ] `User.java` tiene campo `password` anotado con `@Column(length = 255)` — nullable
- [ ] `User.java` tiene campo `role` como **enum** (`Role.USER`, `Role.AGENT`, `Role.ADMIN`) con `@Enumerated(EnumType.STRING)`
- [ ] `User.java` tiene campo `active` con valor por defecto `true`
- [ ] Migración V5 (`V5__lesson_16_add_password_to_users.sql`) — agrega columna `password` con `ALTER TABLE`
- [ ] Migración V6 (`V6__lesson_16_seed_users_with_auth.sql`) — inserta 3 usuarios con hashes BCrypt reales (no placeholders)
- [ ] `DataInitializer` tiene `@Profile("h2")` para no ejecutarse en MySQL/Supabase
- [ ] `DataInitializer` inyecta `PasswordEncoder` y llama a `passwordEncoder.encode()`

### Autenticación
- [ ] `CustomUserDetailsService.java` existe en el paquete `config/`
- [ ] Implementa `UserDetailsService` con `@Service`
- [ ] `loadUserByUsername` usa `UserRepository.findByEmail(email)`
- [ ] Filtra usuarios sin contraseña (`!= null && !isBlank()`)
- [ ] Construye roles con `.roles(user.getRole().name())` o authorities con `"ROLE_" + user.getRole().name()`
- [ ] Usa `.disabled(!user.isActive())` para bloquear usuarios inactivos
- [ ] `PasswordEncoder` declarado como `@Bean` y usando `BCryptPasswordEncoder`
- [ ] `httpBasic(Customizer.withDefaults())` está habilitado
- [ ] La autenticación funciona: POST con credenciales correctas → no es 401

### Autorización
- [ ] `SecurityConfig.java` existe en el paquete `config/`
- [ ] Tiene `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`
- [ ] Tiene `SessionCreationPolicy.STATELESS`
- [ ] `csrf(csrf -> csrf.disable())` incluido
- [ ] GET `/tickets` y `/tickets/by-id/**` son `permitAll()`
- [ ] GET `/categories`, `/tags`, `/users` son `permitAll()`
- [ ] POST `/tickets` y PUT `/tickets/by-id/**` requieren `hasAnyRole("USER", "AGENT", "ADMIN")` como regla general
- [ ] DELETE `/tickets/by-id/**` requiere `hasRole("ADMIN")`
- [ ] Si se usa `@PreAuthorize`, `@EnableMethodSecurity` está presente
- [ ] Existe bean `TicketSecurity` con `@Component("ticketSecurity")`
- [ ] `PUT /tickets/by-id/{id}` usa `@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")`
- [ ] `TicketSecurity` permite a USER editar solo tickets donde `createdBy.email` coincide con `authentication.name`
- [ ] `TicketSecurity` permite a AGENT editar solo tickets donde `assignedTo.email` coincide con `authentication.name`
- [ ] `TicketSecurity` permite a ADMIN editar cualquier ticket

### Pruebas funcionales
- [ ] `GET /tickets` sin auth → `200 OK`
- [ ] `POST /tickets` sin auth → `401 Unauthorized`
- [ ] `POST /tickets` con USER → `201 Created`
- [ ] `POST /tickets` con AGENT → `201 Created`
- [ ] `PUT /tickets/by-id/{id}` con USER creador → `200 OK`
- [ ] `PUT /tickets/by-id/{id}` con USER no creador → `403 Forbidden`
- [ ] `PUT /tickets/by-id/{id}` con AGENT asignado → `200 OK`
- [ ] `PUT /tickets/by-id/{id}` con AGENT no asignado → `403 Forbidden`
- [ ] `DELETE /tickets/by-id/{id}` con USER → `403 Forbidden`
- [ ] `DELETE /tickets/by-id/{id}` con ADMIN → `204 No Content`
- [ ] Usuario `active=false` → `401 Unauthorized`

---

## 🎓 Rúbrica de Evaluación

### Entidad y Migración (20 pts)

| Criterio | Pts | Evidencia |
|----------|-----|-----------|
| Campo `password` en `User.java` (`@Column`, nullable) | 5 | Campo presente con anotación correcta |
| Campo `role` como enum con tres valores | 3 | `Role.USER`, `Role.AGENT`, `Role.ADMIN` |
| Migración V5 agrega columna `password` | 5 | Archivo `V5__lesson_16_add_password_to_users.sql` con `ALTER TABLE` |
| Migración V6 inserta usuarios con hashes BCrypt reales | 7 | Hashes con formato `$2a$10$...`, no texto plano ni placeholders |

### Autenticación (25 pts)

| Criterio | Pts | Evidencia |
|----------|-----|-----------|
| `CustomUserDetailsService` implementa `UserDetailsService` | 5 | `implements UserDetailsService` + `@Service` |
| `loadUserByUsername` carga usuario desde `UserRepository` | 10 | Usa `userRepository.findByEmail(email)` |
| Rol usa `.name()` para el enum | 5 | `.roles(user.getRole().name())` o `"ROLE_" + user.getRole().name()` |
| Login funciona con credenciales de la BD | 5 | POST con auth correcta → no es 401 |

### Autorización (25 pts)

| Criterio | Pts | Evidencia |
|----------|-----|-----------|
| GET endpoints son públicos (`permitAll`) | 5 | GET sin auth → 200 OK |
| POST `/tickets` permite USER y AGENT | 5 | USER → 201; AGENT → 201 |
| PUT aplica propiedad/asignación | 8 | USER propio → 200; USER ajeno → 403; AGENT asignado → 200; AGENT no asignado → 403 |
| DELETE solo para ADMIN | 7 | USER → 403; AGENT → 403; ADMIN → 204 |
| `SessionCreationPolicy.STATELESS` configurado | 5 | Presente en `SecurityConfig` |

### Seguridad (15 pts)

| Criterio | Pts | Evidencia |
|----------|-----|-----------|
| `PasswordEncoder` es `BCryptPasswordEncoder` | 5 | Bean declarado en `SecurityConfig` |
| Contraseñas almacenadas como hash | 5 | BD y migraciones guardan hashes BCrypt, no texto plano |
| `csrf(csrf -> csrf.disable())` incluido | 5 | POST/PUT/DELETE no devuelven 403 por CSRF |

### Documentación (15 pts)

| Criterio | Pts | Evidencia |
|----------|-----|-----------|
| README o comentarios explican los roles y credenciales | 8 | Email, contraseña y rol de cada usuario documentados |
| Explica flujo de autenticación | 7 | Descripción: email → `CustomUserDetailsService` → BD → BCrypt |

---

## 🚩 Red Flags (Falla automática)

- ❌ Contraseñas almacenadas en texto plano en la BD o en migraciones SQL
- ❌ GET endpoints requieren autenticación (rompe la especificación)
- ❌ `CustomUserDetailsService` no existe (usa `InMemoryUserDetailsManager` u otro)
- ❌ El campo `password` no está en la entidad `User`
- ❌ `csrf(csrf -> csrf.disable())` ausente (POST/PUT/DELETE fallan con 403)
- ❌ El proyecto no compila
- ❌ `user.getRole()` sin `.name()` en `UserDetails` → error de compilación o roles incorrectos
- ❌ Migración V6 con hashes en texto plano o con placeholder `$2a$10$...hash...`
- ❌ USER puede editar tickets creados por otro usuario
- ❌ AGENT puede editar tickets que no tiene asignados

**Total: 100 puntos**





<!-- START OF FILE: docs_lessons_16-spring-security_08_actividad_individual.md -->
# Documento: docs lessons 16-spring-security 08 actividad individual
---
# Lección 16 — Actividad Individual

## Objetivo

Proteger tu API REST de Tickets con autenticación basada en base de datos, autorización por roles y autorización por datos del ticket, usando Spring Security y Flyway.

> **Conexión con Lecciones 10–15:** En lecciones anteriores creaste la entidad `User` con JPA y migraciones Flyway. En esta actividad extenderás esa entidad para soportar autenticación real: los usuarios se cargarán desde la base de datos, con contraseñas guardadas como hashes BCrypt.

---

## Tabla de permisos esperada

| Endpoint | Método | Sin auth | USER | AGENT | ADMIN |
|----------|--------|:--------:|:----:|:-----:|:-----:|
| `/tickets` | GET | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| `/tickets/by-id/{id}` | GET | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| `/tickets` | POST | ❌ 401 | ✅ 201 | ✅ 201 | ✅ 201 |
| `/tickets/by-id/{id}` propio/asignado | PUT | ❌ 401 | ✅ 200 | ✅ 200 | ✅ 200 |
| `/tickets/by-id/{id}` ajeno/no asignado | PUT | ❌ 401 | ❌ 403 | ❌ 403 | ✅ 200 |
| `/tickets/by-id/{id}` | DELETE | ❌ 401 | ❌ 403 | ❌ 403 | ✅ 204 |

---

## Instrucciones paso a paso

### Paso 1: Agregar dependencia

En `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Paso 2: Actualizar `User.java`

La entidad debe tener el campo `password`, el rol como enum y el campo `active`:

```java
@Column(length = 255)
private String password;   // BCrypt hash; nullable para usuarios sin acceso al sistema

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.USER;

@Column(nullable = false)
private boolean active = true;

public enum Role {
    USER,
    AGENT,
    ADMIN
}
```

> Si tu entidad ya tiene `role` como `String`, migra a enum: es más seguro y evita valores inválidos.

### Paso 3: Crear migración para la columna `password`

Archivo: `V{n}__lesson_16_add_password_to_users.sql`

```sql
-- Agrega columna password a la tabla users para Spring Security
ALTER TABLE users ADD COLUMN password VARCHAR(255);
```

Donde `{n}` es el número que sigue a tu última migración existente.

### Paso 4: Crear migración seed con hashes BCrypt

Archivo: `V{n+1}__lesson_16_seed_users_with_auth.sql`

Los hashes a continuación corresponden a las contraseñas `pass123` y `user123`. Puedes generar los tuyos con:

```java
// Ejecuta esto una vez para generar tus propios hashes:
System.out.println(new BCryptPasswordEncoder(10).encode("tu_contraseña"));
```

```sql
-- Seed de usuarios con contraseñas BCrypt (cost factor 10)
-- admin@empresa.com  → pass123
-- ana.garcia@empresa.com → user123
-- carlos.lopez@empresa.com → user123
INSERT INTO users (name, email, role, active, password) VALUES
  ('Administrador',  'admin@empresa.com',         'ADMIN', true,
   '$2a$10$gT.PsFi3xTq9xc3virQAfesYBesY5g53tQ5R7lgJGqgVdVMH0I8qa'),
  ('Ana Garcia',     'ana.garcia@empresa.com',     'USER',  true,
   '$2a$10$LAK58ME84bgotvy2eL.eWeobSCHMDsaD3BajXq/swyevMwfw8PW/m'),
  ('Carlos Lopez',   'carlos.lopez@empresa.com',   'AGENT', true,
   '$2a$10$LAK58ME84bgotvy2eL.eWeobSCHMDsaD3BajXq/swyevMwfw8PW/m');
```

> **No uses placeholders.** Los hashes deben ser valores BCrypt reales. Puedes usar los de arriba (que ya están verificados) o generar los tuyos propios.

### Paso 5: Actualizar `DataInitializer` para H2

Agrega `@Profile("h2")` e inyecta `PasswordEncoder`:

```java
@Component
@Profile("h2")   // Solo corre con H2; MySQL/Supabase usan la migración Flyway
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@empresa.com");
            admin.setPassword(passwordEncoder.encode("pass123"));
            admin.setRole(User.Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);

            User ana = new User();
            ana.setName("Ana Garcia");
            ana.setEmail("ana.garcia@empresa.com");
            ana.setPassword(passwordEncoder.encode("user123"));
            ana.setRole(User.Role.USER);
            ana.setActive(true);
            userRepository.save(ana);

            User carlos = new User();
            carlos.setName("Carlos Lopez");
            carlos.setEmail("carlos.lopez@empresa.com");
            carlos.setPassword(passwordEncoder.encode("user123"));
            carlos.setRole(User.Role.AGENT);
            carlos.setActive(true);
            userRepository.save(carlos);
        }
    }
}
```

### Paso 6: Crear `CustomUserDetailsService`

Archivo: `src/main/java/.../config/CustomUserDetailsService.java`

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .filter(u -> u.getPassword() != null && !u.getPassword().isBlank())
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuario no encontrado o sin contraseña: " + email));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .disabled(!user.isActive())
            .build();
    }
}
```

> **Recuerda:** `user.getRole()` devuelve el enum. Llama a `.name()` para obtener `USER`, `AGENT` o `ADMIN`. El builder `.roles(...)` agrega internamente el prefijo `ROLE_`.

### Paso 7: Crear `SecurityConfig`

Archivo: `src/main/java/.../config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/tickets", "/tickets/by-id/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/tickets").hasAnyRole("USER", "AGENT", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/tickets/by-id/**").hasAnyRole("USER", "AGENT", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/tickets/by-id/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> `CustomUserDetailsService` está anotado con `@Service`, así que Spring lo detecta automáticamente. No necesitas declararlo como bean aquí.

### Paso 8: Restringir edición por propietario/asignado

`PUT /tickets/by-id/{id}` debe aplicar una regla más fina que solo rol:

- `USER` solo edita tickets donde `createdBy.email` coincide con el usuario autenticado.
- `AGENT` solo edita tickets donde `assignedTo.email` coincide con el usuario autenticado.
- `ADMIN` edita cualquier ticket.

Crea `config/TicketSecurity.java`:

```java
package cl.duoc.fullstack.tickets.config;

import cl.duoc.fullstack.tickets.model.Ticket;
import cl.duoc.fullstack.tickets.respository.TicketRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("ticketSecurity")
public class TicketSecurity {

    private final TicketRepository ticketRepository;

    public TicketSecurity(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public boolean canEdit(Long ticketId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (hasRole(authentication, "ROLE_ADMIN")) {
            return true;
        }

        String email = authentication.getName();

        return ticketRepository.findById(ticketId)
            .map(ticket -> canEditTicket(ticket, email, authentication))
            .orElse(false);
    }

    private boolean canEditTicket(Ticket ticket, String email, Authentication authentication) {
        if (hasRole(authentication, "ROLE_USER")) {
            return ticket.getCreatedBy() != null
                && email.equals(ticket.getCreatedBy().getEmail());
        }

        if (hasRole(authentication, "ROLE_AGENT")) {
            return ticket.getAssignedTo() != null
                && email.equals(ticket.getAssignedTo().getEmail());
        }

        return false;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
```

Anota el método de actualización en `TicketController`:

```java
@PutMapping("/by-id/{id}")
@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")
public ResponseEntity<Object> updateTicketById(
    @PathVariable Long id,
    @Valid @RequestBody TicketRequest request) {
    // implementación existente
}
```

### Paso 9: Testear con Postman/Thunder Client

Usa el modo **Basic Auth** de Postman (pestaña Authorization → Type: Basic Auth) o el header manual:

| Email | Contraseña | Rol | Base64 |
|-------|-----------|-----|--------|
| admin@empresa.com | pass123 | ADMIN | `YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw==` |
| ana.garcia@empresa.com | user123 | USER | `YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz` |
| carlos.lopez@empresa.com | user123 | AGENT | `Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=` |

#### Test 1: GET sin auth → debe ser 200
```
GET http://localhost:8080/ticket-app/tickets
```

#### Test 2: POST sin auth → debe ser 401
```
POST http://localhost:8080/ticket-app/tickets
Content-Type: application/json

{ "title": "Test", "description": "test" }
```

#### Test 3: POST con USER → debe ser 201
```
POST http://localhost:8080/ticket-app/tickets
Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
Content-Type: application/json

{ "title": "Test User", "description": "test" }
```

#### Test 4: POST con AGENT → debe ser 201
```
POST http://localhost:8080/ticket-app/tickets
Authorization: Basic Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=
Content-Type: application/json

{ "title": "Test Agent", "description": "test" }
```

#### Test 5: DELETE con USER → debe ser 403
```
DELETE http://localhost:8080/ticket-app/tickets/by-id/1
Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
```

#### Test 6: DELETE con ADMIN → debe ser 204
```
DELETE http://localhost:8080/ticket-app/tickets/by-id/1
Authorization: Basic YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw==
```

#### Test 7: PUT con USER creador → debe ser 200
```
PUT http://localhost:8080/ticket-app/tickets/by-id/{ticketCreadoPorAna}
Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
Content-Type: application/json

{ "title": "Editado por creador", "description": "test", "status": "OPEN" }
```

#### Test 8: PUT con USER no creador → debe ser 403
```
PUT http://localhost:8080/ticket-app/tickets/by-id/{ticketCreadoPorOtroUsuario}
Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
Content-Type: application/json

{ "title": "Intento ajeno", "description": "test", "status": "OPEN" }
```

#### Test 9: PUT con AGENT asignado → debe ser 200
```
PUT http://localhost:8080/ticket-app/tickets/by-id/{ticketAsignadoACarlos}
Authorization: Basic Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=
Content-Type: application/json

{ "title": "Editado por agente", "description": "test", "status": "IN_PROGRESS" }
```

#### Test 10: PUT con AGENT no asignado → debe ser 403
```
PUT http://localhost:8080/ticket-app/tickets/by-id/{ticketNoAsignadoACarlos}
Authorization: Basic Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=
Content-Type: application/json

{ "title": "Intento no asignado", "description": "test", "status": "IN_PROGRESS" }
```

---

## Desafío extra

1. **Endpoint de perfil:** `GET /auth/me` que devuelva el email y rol del usuario autenticado
2. **Respetar `active`:** Modifica `CustomUserDetailsService` para rechazar usuarios con `active = false` (usa `.disabled(!user.isActive())`)
3. **Agregar CORS:** Configura CORS en `SecurityConfig` para permitir peticiones desde `localhost:3000`
4. **Rol VIEWER:** Agrega un cuarto rol `VIEWER` que solo pueda hacer GET, ni POST ni PUT

---

## Checklist de entrega

- [ ] `User.java` tiene campo `password` (nullable) y `role` como enum con AGENT
- [ ] Migración Flyway agrega columna `password` a tabla `users`
- [ ] Migración Flyway seed inserta 3 usuarios con hashes BCrypt **reales**
- [ ] `DataInitializer` tiene `@Profile("h2")` e inyecta `PasswordEncoder`
- [ ] `CustomUserDetailsService` carga usuarios desde `UserRepository`
- [ ] `loadUserByUsername` usa `.name()` para convertir el enum a `String`
- [ ] `SecurityConfig` tiene `STATELESS`, `csrf.disable()` y `@EnableMethodSecurity`
- [ ] `PasswordEncoder` es `BCryptPasswordEncoder` declarado como `@Bean`
- [ ] GET `/tickets` y GET `/tickets/by-id/{id}` son públicos
- [ ] POST `/tickets` permite USER, AGENT y ADMIN
- [ ] PUT `/tickets/by-id/{id}` usa `@PreAuthorize("@ticketSecurity.canEdit(#id, authentication)")`
- [ ] USER solo puede editar tickets creados por él
- [ ] AGENT solo puede editar tickets asignados a él
- [ ] ADMIN puede editar cualquier ticket
- [ ] DELETE `/tickets/by-id/{id}` requiere ADMIN
- [ ] Los 10 tests pasan correctamente





<!-- START OF FILE: docs_lessons_16-spring-security_README.md -->
# Documento: docs lessons 16-spring-security README
---
# Lección 16 — Spring Security: Autenticación y Autorización

**Protege tu API REST con autenticación basada en base de datos y autorización por roles. Los usuarios se cargan desde MySQL/H2, las contraseñas se almacenan con BCrypt, y cada endpoint define quién puede acceder.**

---

## 📚 Contenidos

| Documento | Duración | Para |
|-----------|----------|------|
| **01. Objetivo y Alcance** | 5 min | Entender qué aprenderás |
| **02. Guión Paso a Paso** ⭐ | 25 min | Instrucciones prácticas |
| **03. Autenticación vs Autorización** | 10 min | Conceptos clave y arquitectura |
| **04. Ejemplos Avanzados** | 15 min | Código adicional listo para usar |
| **05. Cifrado de Contraseñas** | 10 min | BCrypt en detalle |
| **06. Troubleshooting** | 10 min | Errores frecuentes y soluciones |
| **07. Checklist** | 5 min | Verificación antes de entregar |
| **08. Actividad Individual** | — | Tu tarea |

---

## 🎯 Quick Start

### Concepto

- **Autenticación:** Verificar identidad (¿quién eres?) → 401 si falla
- **Autorización:** Verificar permisos (¿qué puedes hacer?) → 403 si falla

### Flujo con HTTP Basic Auth

```
Cliente → GET /tickets
        ← 200 OK (endpoint público, sin autenticación)

Cliente → POST /tickets (sin header Authorization)
        ← 401 Unauthorized

Cliente → POST /tickets
          Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
        ← 201 Created  (USER puede crear)

Cliente → DELETE /tickets/by-id/1
          Authorization: Basic YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz
        ← 403 Forbidden  (USER no puede eliminar)

Cliente → DELETE /tickets/by-id/1
          Authorization: Basic YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw==
        ← 204 No Content  (ADMIN puede eliminar)
```

> **HTTP Basic Auth:** Las credenciales viajan codificadas en Base64 en el header `Authorization`. No hay endpoint `/login`; Spring Security intercepta cada petición automáticamente.

---

## Lo que construirás

1. Agregar `spring-boot-starter-security` al `pom.xml`
2. Actualizar `User.java`: campo `password` + enum `Role` (USER / AGENT / ADMIN)
3. Crear migración Flyway `V5` — agrega columna `password` a la tabla `users`
4. Crear migración Flyway `V6` — seed de 3 usuarios con hashes BCrypt
5. Completar `UserRepository.findByEmail` — el email será el username de Basic Auth
6. Crear `PasswordEncoder` — BCrypt para validar contraseñas hasheadas
7. Crear `CustomUserDetailsService` — carga usuarios desde la BD y expone roles
8. Crear `SecurityConfig` — HTTP Basic, sesión STATELESS y reglas por rol
9. Aplicar `@PreAuthorize` cuando la autorización pertenezca al caso de uso
10. Crear `TicketSecurity` — USER edita solo tickets propios; AGENT solo tickets asignados; ADMIN cualquiera
11. Testear: sin auth → 401; rol insuficiente → 403; credenciales correctas → 200/201/204

---

## Credenciales de prueba

| Email | Contraseña | Rol | Base64 para Basic Auth |
|-------|-----------|-----|------------------------|
| admin@empresa.com | pass123 | ADMIN | `YWRtaW5AZW1wcmVzYS5jb206cGFzczEyMw==` |
| ana.garcia@empresa.com | user123 | USER | `YW5hLmdhcmNpYUBlbXByZXNhLmNvbTp1c2VyMTIz` |
| carlos.lopez@empresa.com | user123 | AGENT | `Y2FybG9zLmxvcGV6QGVtcHJlc2EuY29tOnVzZXIxMjM=` |

---

## 🚀 Sigue el Guión

Comienza con **[02. Guión Paso a Paso](02_guion_paso_a_paso.md)** para instrucciones detalladas.

---

*Lección 16 de 18 — [← Volver a Lecciones](../)*



