<!-- START OF FILE: docs_lessons_levantamiento.pdf.md -->
# Documento: docs lessons levantamiento.pdf
---
# Evaluacion Parcial 2 — Fase de Levantamiento

**Proyecto Semestral · Arquitectura de Microservicios · DSY1103 Desarrollo FullStack 1**

| | |
|---|---|
| **Entrega** | Semana 10 |
| **Equipos** | 2–3 integrantes |
| **Formato** | Documento de analisis previo al desarrollo |

---

## 0. Contexto del encargo

Antes de escribir una sola linea de codigo, todo proyecto de software profesional comienza con un proceso de analisis y diseno. Este documento de levantamiento es la base que justificara todas las decisiones tecnicas que tome el equipo durante el desarrollo. Debe ser entregado junto al repositorio GitHub como parte del `README.md` o como documento adjunto.

> **IMPORTANTE**
> Esta fase es evaluada dentro de la dimension grupal (30%). Un levantamiento debil o generico se notara directamente en la coherencia de la arquitectura y afectara la defensa individual.

---

## 1. Identificacion del problema

Describe el problema real o simulado que la aplicacion busca resolver. Debe estar redactado desde la perspectiva del usuario afectado, no desde la tecnologia. Responde: ¿que situacion genera ineficiencia, perdida o frustracion?, ¿a quien afecta?, ¿por que es relevante?

**Entregables:**
- Parrafo de contexto del dominio (2–3 parrafos)
- Declaracion del problema principal (1 oracion clara)
- Identificacion de los actores/usuarios involucrados

> **Tips para redactarlo bien**
>
> - Evita decir "el sistema no existe" — enfocate en el dolor que genera la ausencia o ineficiencia actual.
>   *Ej: "Los encargados de bodega registran manualmente el stock en planillas compartidas, lo que genera inconsistencias y perdidas de inventario no detectadas a tiempo."*
> - Nombra roles concretos: administrador, cliente, cajero, medico, etc. No uses "el usuario" de forma generica.
> - Acota el dominio — no intentes resolver todo. Un sistema de reservas de cancha, no "un sistema deportivo completo".
>
> **Dominios sugeridos:** reserva de horas medicas · gestion de inventario · arriendo de vehiculos · e-commerce de productos · gestion de proyectos internos

---

## 2. Requerimientos del sistema

Lista las capacidades que el sistema debe tener. Se dividen en **funcionales** (que debe hacer) y **no funcionales** (como debe comportarse). Esta seccion es el contrato entre el equipo y el sistema a construir.

### 2.1 Requerimientos funcionales

Acciones concretas que el sistema debe permitir. Redacta cada uno comenzando con un verbo en infinitivo.

**Minimo 15 requerimientos funcionales, numerados (RF-01, RF-02...)**

> **Tips**
>
> - Cada requerimiento debe ser **verificable**. En lugar de "gestionar usuarios", escribe: *"Registrar un nuevo usuario con nombre, correo y contrasena encriptada."*
> - Agrupa por entidad o actor: primero los de Cliente, luego los de Administrador, etc. Esto te ayudara a identificar microservicios mas adelante.
> - Asegurate de incluir operaciones de creacion, consulta, modificacion y eliminacion para las entidades principales — cada una puede volverse un endpoint REST.

### 2.2 Requerimientos no funcionales

**Al menos 5 requerimientos no funcionales (RNF-01, RNF-02...)**

> **Tips**
>
> - En el contexto de esta evaluacion son relevantes: manejo adecuado de errores HTTP, validacion de datos de entrada, trazabilidad mediante logs, independencia entre microservicios, uso de convenciones REST.
> - Evita escribir "el sistema debe ser rapido" — prefiere *"cada endpoint debe retornar respuesta en menos de 500 ms bajo condiciones normales."*

---

## 3. Solucion propuesta

Describe, a nivel conceptual, como el equipo planea resolver el problema usando una arquitectura de microservicios. No entres en detalles de codigo — explica la logica de diseno y por que una arquitectura distribuida es apropiada para este dominio.

**Entregables:**
- Descripcion general de la solucion (2–3 parrafos)
- Justificacion del uso de microservicios en este dominio
- Diagrama conceptual de la arquitectura (bloques de microservicios y sus relaciones)

> **Tips**
>
> - Justifica por que cada bloque funcional merece ser un servicio independiente: separacion de responsabilidades, bases de datos propias, escalabilidad diferenciada.
> - El diagrama puede ser simple: cajas con nombre del servicio y flechas de comunicacion. Herramientas como draw.io, Excalidraw o incluso papel escaneado son validas.
> - Menciona que tecnologia usaran para la comunicacion entre servicios: **WebClient** o **Feign Client** (ambas validas segun la rubrica).

---

## 4. Casos de uso o historias de usuario

Documenta como los distintos actores interactuan con el sistema. Puedes elegir entre **casos de uso** (mas formal, con flujo principal y alternativo) o **historias de usuario** (mas agil, con criterios de aceptacion).

- **Minimo 8** casos de uso o historias de usuario
- Cada uno debe incluir: actor, objetivo, flujo / criterios de aceptacion

### Formato de historia de usuario (recomendado)

```
Como [rol/actor], quiero [funcionalidad], para [beneficio/objetivo].

Criterios de aceptacion:
- Dado que [contexto], cuando [accion], entonces [resultado esperado].
- El sistema debe retornar un error 400 si el campo requerido no esta presente.
- La respuesta debe incluir el ID generado del recurso creado.
```

**Ejemplo:**
> *Como cliente registrado, quiero reservar una hora con un medico, para asegurar mi atencion sin llamar por telefono.*
>
> Cada criterio de aceptacion se traduce directamente en un endpoint o validacion del backend.

> **OJO**
> Las historias de usuario deben poder mapearse a requerimientos funcionales y luego a endpoints REST. Si una historia no genera ningun endpoint, probablemente es demasiado vaga.

---

## 5. Definicion de microservicios

Esta es la seccion mas tecnica del levantamiento y la que mas impacta directamente en el desarrollo. Debes definir cada microservicio que el equipo implementara, con sus responsabilidades, entidades, endpoints y relaciones con otros servicios.

- **Minimo 10 microservicios** definidos (sin limite maximo)
- Para cada microservicio: nombre, responsabilidad, entidades JPA, endpoints REST y dependencias

### Plantilla por microservicio

| Campo | Descripcion |
|---|---|
| **Nombre** | Sustantivo representativo del dominio (ej: `reservation-service`, `patient-service`) |
| **Responsabilidad** | Una sola oracion que describe que gestiona este servicio y nada mas |
| **Entidades JPA** | Clases con sus atributos principales y relaciones (`@OneToMany`, `@ManyToOne`, etc.) |
| **Endpoints REST** | Metodo HTTP + ruta + descripcion breve. Ej: `POST /api/reservations` — crear nueva reserva |
| **Comunica con** | Que otros servicios consulta o llama, y por que |
| **Base de datos** | Nombre del esquema o BD propia (cada microservicio debe tener la suya) |

> **Tips para definir bien los microservicios**
>
> - El nombre del servicio debe reflejar **una sola responsabilidad**. Si sientes que necesitas "y" para describirlo (ej: "gestiona usuarios y notificaciones"), dividelo en dos.
> - Cada servicio tiene su **propia base de datos** — nunca dos servicios comparten la misma tabla directamente. La comunicacion entre servicios se hace via API REST.
> - Piensa en tus requerimientos funcionales: agrupalos por entidad o contexto. Cada agrupacion coherente puede convertirse en un microservicio.
> - Asegurate de que al menos **2–3 servicios se comunican entre si** (requisito de la rubrica). Ej: `order-service` llama a `product-service` para verificar stock.
> - Identifica cuales son los servicios **"core"** (sin ellos nada funciona) y cuales son de **soporte**. Los core deben estar completamente implementados con CRUD y validaciones.
>
> **Ejemplo para sistema de arriendo:** `vehicle-service` · `customer-service` · `rental-service` · `payment-service` · `branch-service` · `reservation-service` · `insurance-service` · `maintenance-service` · `category-service` · `review-service`

---

## 6. Checklist de entrega

- [ ] Identificacion del problema con actores definidos
- [ ] Minimo 15 RF y 5 RNF numerados y verificables
- [ ] Solucion propuesta con diagrama de arquitectura
- [ ] Minimo 8 historias de usuario con criterios de aceptacion
- [ ] Minimo 10 microservicios definidos con plantilla completa
- [ ] Al menos 2–3 servicios con comunicacion inter-servicio identificada
- [ ] Documento incluido en el repositorio GitHub (`README.md` o PDF adjunto)

---

*DSY1103 Desarrollo FullStack 1 · Subdireccion de Diseno Instruccional · 2025*



