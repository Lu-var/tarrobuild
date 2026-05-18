<!-- START OF FILE: docs_lessons_INDICE_COMPLETO.md -->
# Documento: docs lessons INDICE COMPLETO
---
# 📚 Índice Completo del Curso — 19 Lecciones

## 🎓 Estructura del Curso

### ⚫ Pre-requisitos (Lección 0)
Herramientas y versionado

| # | Título | Estado |
|---|--------|--------|
| 00 | Git & GitHub | ✅ Completada |

---

### 🟢 Fundamentos (Lecciones 1-4)
Conceptos básicos de web, HTTP y REST

| # | Título | Estado |
|---|--------|--------|
| 01 | Web y HTTP | ✅ Completada |
| 02 | APIs y REST | ✅ Completada |
| 03 | Tu Primera API | ✅ Completada |
| 04 | Responsabilidades | ✅ Completada |

---

### 🟡 Desarrollo Backend (Lecciones 5-10)
Construcción de aplicación completa

| # | Título | Status |
|---|--------|--------|
| 05 | POST y Validación | ✅ Completada |
| 06 | CRUD Completo | ✅ Completada |
| 07 | Manejo de Errores | ✅ Completada |
| 08 | DTOs y Mapeo | ✅ Completada |
| 09 | Repositorio Customizado | ✅ Completada |
| 10 | Introducción a JPA | ✅ Completada |

---

### 🔵 Bases de Datos y Relaciones (Lecciones 11-13)
Persistencia avanzada

| # | Título | Status | Destacado |
|---|--------|--------|-----------|
| 11 | Configuración de BD | ✅ Completada | **Perfiles Spring Boot + Variables de Entorno** |
| 12 | Relaciones JPA | ✅ Completada | **One-to-Many, Many-to-Many** |
| 13 | Historial y Auditoría | ✅ Completada | **Tracking de cambios** |

---

### 🟣 Producción I (Lecciones 14-15)
Migraciones y comunicación entre servicios

| # | Título | Status | Destacado |
|---|--------|--------|-----------|
| 14 | Migraciones Flyway | ✅ Completada | **SQL Versionado (H2=JPA, MySQL/Supabase=Flyway)** |
| 15 | Microservicios | ✅ Completada | **RestTemplate y FeignClient** |

---

### 🔴 Producción II (Lecciones 16-18)
Seguridad, auditoría y manejo de errores

| # | Título | Status | Destacado |
|---|--------|--------|-----------|
| 16 | Spring Security | ✅ Completada | **Autenticación + Autorización, Roles ADMIN/USER** |
| 17 | Logging | ✅ Completada | **SLF4J + Logback, Niveles DEBUG/INFO/WARN/ERROR** |
| 18 | Exception Handling Global | ✅ Completada | **@ControllerAdvice, Respuestas Uniformes** |

---

## 📖 Navegación Recomendada

```
Pre-requisitos
├─ Lección 0 (Git)

Principiante
├─ Lecciones 1-4  (Conceptos básicos)
└─ Lecciones 5-6  (Primera API funcional)

Intermedio
├─ Lecciones 7-10 (Backend completo)
├─ Lección 11     (Múltiples BD)
├─ Lección 12     (Relaciones JPA)
└─ Lección 13     (Auditoría)

Avanzado
├─ Lección 14     (Migraciones profesionales)
├─ Lección 15     (Microservicios)
├─ Lección 16     (Seguridad)
├─ Lección 17     (Logging)
└─ Lección 18     (Manejo global de errores)
```

---

## 🎯 Lección 11 — Configuración de Bases de Datos

**Tema:** Perfiles de Spring Boot + Variables de Entorno

- ✅ 3 perfiles: H2, MySQL, Supabase
- ✅ Configuración segura con `.env`
- ✅ Carga de variables en IntelliJ IDEA
- ✅ No hardcodear credenciales

**Documentos:** 11 archivos (README, índice, guía paso a paso, ejemplos, cheat sheet, etc)

**Habilidades:**
- [ ] Usar perfiles de Spring Boot
- [ ] Gestionar variables de entorno
- [ ] Cambiar BD sin modificar código Java
- [ ] Proteger credenciales

---

## 🎯 Lección 12 — Relaciones JPA

**Tema:** Modelar relaciones entre entidades

**Habilidades:**
- [ ] One-to-Many y Many-to-One
- [ ] Many-to-Many
- [ ] Lazy vs Eager loading
- [ ] Cascade operations

---

## 🎯 Lección 13 — Historial y Auditoría

**Tema:** Tracking de cambios en la BD

**Habilidades:**
- [ ] Registrar cambios automáticamente
- [ ] Auditoría de datos
- [ ] Versionado de entidades

---

## 🎯 Lección 14 — Migraciones con Flyway

**Tema:** Versionado profesional de cambios de BD

- ✅ Flyway para MySQL y Supabase
- ✅ JPA automático para H2
- ✅ SQL versionado (V1, V2, V3...)
- ✅ Tabla de control `flyway_schema_history`

**Documentos:** 9 archivos (guión, configuración, ejemplos SQL, JPA vs Flyway, troubleshooting)

**Habilidades:**
- [ ] Crear migraciones SQL versionadas
- [ ] Entender diferencia JPA vs Flyway
- [ ] Aplicar migraciones automáticamente
- [ ] Manejar errores de migración

---

## 🎯 Lección 15 — Comunicación entre Microservicios

**Tema:** HTTP entre servicios independientes

- ✅ RestTemplate (simple y flexible)
- ✅ FeignClient (automático y elegante)
- ✅ Manejo de errores y timeouts
- ✅ Fallbacks y resilencia

**Documentos:** 9 archivos (guión, comparación, ejemplos, manejo de errores, debugging)

**Habilidades:**
- [ ] Implementar comunicación HTTP entre servicios
- [ ] Usar RestTemplate y FeignClient
- [ ] Configurar timeouts y reintentos
- [ ] Implementar fallbacks
- [ ] Debuggear problemas de comunicación

---

## 🛠️ Stack Tecnológico (Completo)

```
Spring Boot 4.0.3
├─ Spring Web MVC (HTTP, REST)
├─ Spring Data JPA (ORM)
├─ Hibernate (JPA implementation)
├─ Flyway (Migraciones)
├─ Lombok (Boilerplate reduction)
├─ Jakarta Validation (@Valid, @NotBlank)
├─ Spring Cloud OpenFeign (Microservicios)
└─ Spring Boot DevTools (Hot reload)

Databases
├─ H2 (En memoria, tests)
├─ MySQL 8.0 (Local vía XAMPP)
└─ PostgreSQL (Supabase en la nube)

Tools
├─ Maven (Builds)
├─ Git (Versionado)
├─ IntelliJ IDEA (IDE)
└─ Docker (Opcional)
```

---

## 📊 Progresión de Complejidad

```
Lección 1-4:    Fundamentos         ░░░░░░░░░░ (7%)
Lección 5-6:    API simple          ░░░░░░░░░░░░░░░ (20%)
Lección 7-10:   Backend práctico    ░░░░░░░░░░░░░░░░░░░░░░░░░░░ (47%)
Lección 11:     BD múltiples        ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ (60%)
Lección 12-13:  Relaciones/Auditría ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ (73%)
Lección 14:     Migraciones Flyway  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ (87%)
Lección 15:     Microservicios      ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ (100%)
```

---

## ✅ Requisitos Mínimos por Lección

### Lección 11
- ✅ 4 archivos YAML configurados (application + h2/mysql/supabase)
- ✅ `.env.example` creado
- ✅ Variables de entorno funcionales
- ✅ App arranca con todos los perfiles

### Lección 12-13
- ✅ (Relaciones y auditoría - según lecciones originales)

### Lección 14
- ✅ Flyway en `pom.xml`
- ✅ Migraciones V1, V2, V3 creadas (mysql y supabase)
- ✅ Tabla `flyway_schema_history` con 3+ registros
- ✅ Logs muestran "Successfully applied N migrations"

### Lección 15
- ✅ Cliente HTTP implementado (RestTemplate o FeignClient)
- ✅ Comunicación entre 2 servicios funcional
- ✅ Manejo de errores y fallback
- ✅ Timeouts configurados
- ✅ Tests con mocks

---

## 🚀 Próximos Pasos (Opcional)

Después de las 13 lecciones, puedes explorar:

- 🔐 **Autenticación:** JWT, OAuth, Spring Security
- 📊 **Logging:** SLF4J, Logback
- 🔍 **Monitoreo:** Prometheus, Grafana
- 🐳 **Containerización:** Docker, Docker Compose
- ☸️ **Orquestación:** Kubernetes
- 📈 **CI/CD:** GitHub Actions, GitLab CI
- 🔄 **Message Queues:** RabbitMQ, Kafka
- 📚 **API Documentation:** Swagger/OpenAPI

---

## 📞 Soporte

- 📖 Documentación oficial: https://spring.io/projects/spring-boot
- 🔧 Maven Central: https://mvnrepository.com
- 💬 Stack Overflow: Tag `spring-boot`
- 🤝 Comunidad: r/SpringBoot, Spring Community

---

**Última actualización:** Abril 2026  
**Versión:** 2.0 (15 lecciones completas)  
**Estado:** ✅ Listo para producción

---

*[← Volver a Lecciones](../)*



