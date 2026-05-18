<!-- START OF FILE: docs_lessons_00-git-github_01_objetivo_y_alcance.md -->
# Documento: docs lessons 00-git-github 01 objetivo y alcance
---
# Lección 00 - Git & GitHub: ¿Qué vas a aprender?

## ¿De dónde venimos?

Eres nuevo en programación profesional. Hasta ahora trabajaste solo en tu computadora.

Problema: sin versionado, cuando colaboras con otros:
- No sabes qué cambió
- No puedes revertir errores
- Los cambios se pierden
- No hay historial

---

## ¿Qué vas a construir?

Al terminar esta lección:

1. **Crear un repositorio Git local**
2. **Hacer commits** (fotos del código)
3. **Subir a GitHub** (servidor)
4. **Trabajar con branches** (paralelo a main)
5. **Hacer pull requests** (colaboración)

### Flujo real en equipo

```
Tú creas rama "feature-x"
    ↓
Haces 3 commits con cambios
    ↓
Subes a GitHub (git push)
    ↓
Creas Pull Request
    ↓
Compañero revisa código
    ↓
Si está bien → "Merge a main"
    ↓
main se actualiza automáticamente
```

---

## Requerimientos

| ID | Requerimiento |
|----|---------------|
| **REQ-G01** | Repo local funcional con `.git/` |
| **REQ-G02** | Mínimo 5 commits con mensajes descriptivos |
| **REQ-G03** | Repo público en GitHub |
| **REQ-G04** | Branch secundaria + pull request |
| **REQ-G05** | Entender `.gitignore` |

---

## Estructura

```
Antes:
└── Código local sin historial

Después:
├── .git/                    (historial)
├── .gitignore              (qué ignorar)
├── README.md               (documentación)
└── Tu código               (rastreado)

Servidor (GitHub):
└── Tu repositorio público (código + historial)
```

---

## No cubre esta lección

- Git avanzado (rebase, cherry-pick)
- CI/CD (automatización)
- Git hooks

Foco: **flujo básico para trabajo en equipo**.





<!-- START OF FILE: docs_lessons_00-git-github_02_guion_paso_a_paso.md -->
# Documento: docs lessons 00-git-github 02 guion paso a paso
---
# Lección 00 - Tutorial paso a paso: Git & GitHub

## Paso 1: Instalar Git

**Windows:**
```bash
choco install git
# o descargar de https://git-scm.com
```

**Verificar:**
```bash
git --version
```

## Paso 2: Configuración global

```bash
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"
git config --global core.autocrlf true  # Windows
```

## Paso 3: Crear repositorio local

En tu carpeta del proyecto:

```bash
cd C:\Users\tu\IdeaProjects\DSY1103-FULLSTACK-I-BACKEND\Tickets
git init
```

Resultado: aparece carpeta `.git/` (oculta).

## Paso 4: Crear .gitignore

Archivo `Tickets/.gitignore`:

```
target/
.idea/
*.class
*.jar
.env
.DS_Store
node_modules/
```

## Paso 5: Primer commit

```bash
git add .                    # Preparar todos los cambios
git commit -m "Setup inicial"   # Primer snapshot
```

## Paso 6: Crear cuenta GitHub

1. Ir a https://github.com/signup
2. Crear cuenta
3. Verificar email

## Paso 7: Crear repo en GitHub

1. Click "+New repository"
2. Nombre: `DSY1103-FULLSTACK-I-BACKEND`
3. Descripción: `Curso Spring Boot - Sistema de Tickets`
4. Public (para que vea profesor)
5. Crear

GitHub te da comandos. Ejecuta:

```bash
git remote add origin https://github.com/tu-usuario/DSY1103-FULLSTACK-I-BACKEND.git
git branch -M main
git push -u origin main
```

## Paso 8: Hacer cambios y push

```bash
# Editas algo...
git add .
git commit -m "Agregar endpoint /health"
git push
```

En GitHub verás los cambios automáticamente.

## Paso 9: Crear rama de feature

```bash
git checkout -b feature/nuevaFuncion
# Haces cambios...
git add .
git commit -m "Implementar login"
git push origin feature/nuevaFuncion
```

## Paso 10: Pull Request

En GitHub:
1. Click "Compare & pull request"
2. Escribe descripción
3. Click "Create pull request"
4. Revisor aprueba
5. Click "Merge pull request"

main ahora incluye tus cambios.





<!-- START OF FILE: docs_lessons_00-git-github_03_flujo_completo.md -->
# Documento: docs lessons 00-git-github 03 flujo completo
---
# Lección 00 - Flujo completo: Local → GitHub

## Scenario: Trabajar en equipo

```
Tú: trabajas en "feature-security"
Compañero: trabaja en "feature-logging"
Main: siempre estable

    main (v1.0)
     |
     +--- feature-security  (tú)
     |     └─ commit: agregar @PreAuthorize
     |     └─ commit: BCrypt password
     |     └─ push origin feature-security
     |
     +--- feature-logging   (compañero)
           └─ commit: agregar @Slf4j
           └─ push origin feature-logging

    Cuando ambos terminen:
    feature-security → PR → Review → Merge a main
    feature-logging  → PR → Review → Merge a main
    
    main tiene ambas features
```

## Comandos esenciales

```bash
# Ver estado
git status

# Ver historial
git log --oneline

# Ver cambios no commiteados
git diff

# Deshacer cambio (cuidado!)
git checkout -- archivo.java

# Cambiar entre ramas
git checkout main
git checkout feature-x

# Traer cambios del servidor
git pull

# Revertir último commit (sin borrar cambios)
git reset --soft HEAD~1
```

## Buenas prácticas

✅ **DO:**
- Commits pequeños y con propósito
- Mensajes claros: "Agregar validación de email"
- Push al menos 1x por día
- PR antes de merge a main
- Revisar código de otros

❌ **DON'T:**
- Commits gigantes (10 cambios distintos)
- Mensaje vacío: "."
- Trabajar solo en main
- Merge sin revisar
- Commitear .class, .jar, .env

## Conflictos (cuando pasa)

```
Tú cambias línea 10 de UserService.java
Compañero también cambia línea 10

Result: CONFLICT
Solution:
1. Abre archivo
2. Ve <<< CONFLICT >>> y ===
3. Elige cuál quieres (o ambos)
4. git add .
5. git commit -m "Resolver conflicto"
6. git push
```





<!-- START OF FILE: docs_lessons_00-git-github_04_troubleshooting.md -->
# Documento: docs lessons 00-git-github 04 troubleshooting
---
# Lección 00 - Troubleshooting

## Problema 1: "fatal: not a git repository"

**Causa:** Ejecutaste `git status` en carpeta sin `.git/`

**Solución:**
```bash
cd Tickets/
git init
```

## Problema 2: "error: src refspec main does not match any"

**Causa:** No hay commits aún

**Solución:**
```bash
git add .
git commit -m "Setup inicial"
git push -u origin main
```

## Problema 3: Cambios perdidos

**Causa:** Editaste archivo pero no commiteaste, cambio de rama

**Solución:**
```bash
git reflog                    # Ver historial
git checkout <commit-hash>    # Recuperar
```

## Problema 4: Quiero deshacer último commit

**Opción 1:** Sin borrar cambios
```bash
git reset --soft HEAD~1
```

**Opción 2:** Borrando cambios
```bash
git reset --hard HEAD~1
```

## Problema 5: Conflicto en merge

**Síntoma:** Archivos con `<<<<<<< HEAD`

**Solución:**
1. Abre el archivo
2. Resuelve manualmente (elige cuál código mantener)
3. `git add .`
4. `git commit -m "Resolver conflicto"`

## Problema 6: Acidental commit de .env

**Prevención:**
```bash
echo ".env" >> .gitignore
```

**Arreglo (si ya lo commitiste):**
```bash
git rm --cached .env
git commit -m "Remove .env from tracking"
```





<!-- START OF FILE: docs_lessons_00-git-github_05_actividad_individual.md -->
# Documento: docs lessons 00-git-github 05 actividad individual
---
# Lección 00 - Actividad individual

## Objetivo

Dominar Git y GitHub para trabajar en equipo profesionalmente.

---

## Requisitos

1. **Repo local funcional**
   - [ ] `git init` ejecutado
   - [ ] `.gitignore` creado
   - [ ] Mínimo 5 commits

2. **Repo en GitHub**
   - [ ] Cuenta creada
   - [ ] Repositorio público
   - [ ] Push exitoso a main

3. **Branch de feature**
   - [ ] Rama creada desde main
   - [ ] Pull request hecha
   - [ ] Mergeado a main

4. **Buenas prácticas**
   - [ ] Mensajes de commit descriptivos
   - [ ] `.env` no committeado
   - [ ] README.md con descripción

---

## Pasos

### Paso 1: Setup local (10 min)

```bash
cd Tickets
git init
git config user.name "Tu Nombre"
git config user.email "tu@email.com"

# Crear .gitignore con:
# target/, .idea/, *.class, .env, etc

git add .
git commit -m "Setup inicial del proyecto"
```

### Paso 2: GitHub (10 min)

1. Crear repo en GitHub
2. Ejecutar:
```bash
git remote add origin https://github.com/tu-usuario/repo.git
git branch -M main
git push -u origin main
```

### Paso 3: Feature branch (15 min)

```bash
git checkout -b feature/mejora-seguridad
# Haces cambios
git add .
git commit -m "Agregar validación de email"
git push origin feature/mejora-seguridad
```

### Paso 4: Pull request (10 min)

En GitHub:
- Click "Compare & pull request"
- Escribe descripción
- Merge a main

---

## Checklist entrega

- [ ] Repo local con `.git/`
- [ ] Mínimo 5 commits con mensajes claros
- [ ] Repositorio público en GitHub
- [ ] Branch secundaria creada
- [ ] Pull request mergeada
- [ ] `.env` en `.gitignore`
- [ ] README.md presente

---

## Desafío extra

- Crear 3 branches diferentes de features
- Hacer 2 PRs simultáneas
- Resolver un conflicto de merge manualmente





<!-- START OF FILE: docs_lessons_00-git-github_README.md -->
# Documento: docs lessons 00-git-github README
---
# Lección 00 - Git & GitHub: Control de Versiones

## ¿Qué es Git?

Sistema de control de versiones que registra cambios en tu código. Sin Git:

```
proyecto.zip
proyecto_v2.zip
proyecto_v2_final.zip
proyecto_v3_REAL_FINAL.zip  ← ¿Cuál es la verdadera?
```

Con Git:

```
commit 1: Setup inicial
commit 2: Agregar funcionalidad X
commit 3: Bugfix en X
commit 4: Revertir cambios de X

Todo rastreable, reversible, colaborativo.
```

---

## Quick Start

### Instalación

```bash
# Windows
choco install git
# o descargar: https://git-scm.com

# Linux/macOS
brew install git
```

### Configuración inicial

```bash
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"
```

---

## Conceptos clave

- **Repository:** Carpeta con historial de cambios
- **Commit:** Foto del código en un momento específico
- **Branch:** Línea paralela de desarrollo
- **Push:** Enviar cambios a servidor (GitHub)
- **Pull:** Traer cambios del servidor

---

## Flujo básico

```
1. git init          ← Inicializar repo local
2. git add .         ← Preparar cambios
3. git commit -m "msg"  ← Grabar cambios
4. git push          ← Enviar a GitHub
```

---

## Próxima: Lección 1

**L01 - Web y HTTP:** Conceptos fundamentales de la web



