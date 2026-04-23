# Guia de Contribucion

## Conventional Commits

Todos los commits de este repositorio siguen el formato **Conventional Commits**. Esto permite generar CHANGELOGs automaticamente con `release-please`.

### Formato

```
<tipo>[scope opcional]: <descripcion breve en minusculas, sin punto final>

[cuerpo opcional]

[footer opcional — ej. BREAKING CHANGE: ...]
```

### Tipos permitidos

| Tipo | Descripcion | Impacto en version |
|------|-------------|-------------------|
| `feat` | Nueva funcionalidad | MINOR |
| `fix` | Correccion de bug | PATCH |
| `perf` | Mejora de rendimiento | PATCH |
| `revert` | Revertir commit anterior | PATCH |
| `docs` | Solo documentacion | ninguno |
| `style` | Formato, espacios (sin cambio logico) | ninguno |
| `refactor` | Refactor sin cambio de comportamiento | ninguno |
| `test` | Agregar o corregir tests | ninguno |
| `chore` | Tareas de mantenimiento (deps, build) | ninguno |
| `ci` | Cambios en CI/CD | ninguno |
| `build` | Cambios en sistema de build | ninguno |

### Breaking Changes

Para cambios que rompen compatibilidad, agregar `!` despues del tipo o incluir `BREAKING CHANGE:` en el footer:

```
feat!: cambiar esquema de autenticacion JWT

BREAKING CHANGE: El campo `user_id` en el token cambia de INT a UUID.
```

### Ejemplos validos

```
feat(farming): agregar endpoint de reporte de siembra masivo
fix(auth): corregir validacion de token expirado en Keycloak 24
docs: actualizar ADR-009 con decision de Flutter
chore(deps): actualizar Quarkus a 3.10.0
ci: agregar job de deteccion de cambios por paths
```

### Scope recomendados por area

- `backend`, `mobile`, `web`, `projection-engine`
- `auth`, `farming`, `market`, `weather`, `reputation`, `notification`
- `infra`, `db`, `ci`, `deps`

## Flujo de desarrollo

1. Crear branch desde `main`: `git checkout -b feat/mi-funcionalidad`
2. Commits con formato Conventional Commits
3. Abrir PR con template completo
4. CI debe pasar (lint + tests + security scan)
5. Requiere 1 review del area correspondiente (ver CODEOWNERS)
6. Merge via squash — el mensaje del squash debe ser un Conventional Commit valido
