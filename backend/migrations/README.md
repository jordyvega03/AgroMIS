# Migraciones de Base de Datos (Flyway)

## Requisitos

- JDK 21
- PostgreSQL corriendo (local: `./scripts/dev-up.sh`)

## Comandos

```bash
# Aplicar migraciones
./gradlew :migrations:flywayMigrate

# Ver estado de migraciones
./gradlew :migrations:flywayInfo

# Validar checksums
./gradlew :migrations:flywayValidate

# Limpiar (solo dev — DESTRUYE DATOS)
./gradlew :migrations:flywayClean
```

## Variables de entorno

| Variable | Default | Descripcion |
|----------|---------|-------------|
| `FLYWAY_URL` | `jdbc:postgresql://localhost:5432/agromis` | URL JDBC |
| `FLYWAY_USER` | `agromis` | Usuario Postgres |
| `FLYWAY_PASSWORD` | `agromis_dev_pass` | Password Postgres |
| `APP_PASSWORD` | `agromis_app_dev_pass` | Password para el rol `agromis_app` |

## Convencion de nombres

```
V{numero_secuencial}__{descripcion_snake_case}.sql
```

Ejemplo: `V006__add_farming_reports_table.sql`

**Reglas:**
- El numero es de 3 digitos con ceros a la izquierda.
- NUNCA modificar una migracion ya aplicada.
- Para corregir, crear una nueva migracion.
