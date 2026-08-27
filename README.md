# prestamo-confiable-api

Microservicios de la Plataforma de Préstamos Confiables. API REST en Spring Boot
sobre PostgreSQL (Neon).

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | **21** | `<java.version>21</java.version>` en el `pom.xml` |
| Maven | 3.9+ | el proyecto **no** trae wrapper (`mvnw`), hay que instalarlo |
| PostgreSQL | 15+ | o una base en [Neon](https://neon.tech) |

Verifica con `java -version` y `mvn -v` antes de empezar.

## 1. Base de datos

El schema **no se crea solo**: no hay `spring.sql.init` configurado y el JPA corre
con `ddl-auto: none`. Hay que cargarlo a mano una vez:

```bash
psql "$EC_DB_URL" -f src/main/resources/schema.sql
```

O pegando el contenido de [`schema.sql`](src/main/resources/schema.sql) en el SQL
Editor de Neon.

Después crea al menos un usuario en la tabla `usuarios` — no hay seed ni endpoint
de registro, y sin usuario no se puede hacer login. La columna `password` espera
un hash **BCrypt**:

```sql
INSERT INTO usuarios (username, password, nombre, rol)
VALUES ('admin', '<hash-bcrypt>', 'Administrador', 'ADMIN');
```

Los scripts de migración puntuales viven junto al schema, por ejemplo
[`migration-abono-parcial.sql`](src/main/resources/migration-abono-parcial.sql).
Se ejecutan a mano y **después** de desplegar la versión de la API que los
acompaña: la API vieja hace `EstadoPago.valueOf()` sobre lo que lee y truena si
se topa con un estado que todavía no conoce.

> `pagos.estado` es un **ENUM nativo** de Postgres (tipo `estado_pago`), no un
> `VARCHAR`. Agregar un estado nuevo no es solo tocar el enum de Java: hay que
> correr `ALTER TYPE estado_pago ADD VALUE ...` en la base, en su propia
> transacción, antes de poder usar el valor.

## 2. Configuración

Todo sale de variables de entorno con valores por defecto en
[`application.yml`](src/main/resources/application.yml):

| Variable | Para qué | Default |
|---|---|---|
| `EC_DB_URL` | JDBC de PostgreSQL | apunta a la base de Neon |
| `EC_DB_USER` | usuario de la base | `neondb_owner` |
| `EC_DB_PASSWORD` | contraseña | está en el yml |
| `JWT_SECRET` | firma de los tokens (mín. 32 chars) | valor de desarrollo |
| `PORT` | puerto HTTP | `8080` |

> Los defaults del `application.yml` apuntan a la base real y traen credenciales
> en claro. Para trabajar en local, exporta `EC_DB_*` con tu propia base en vez
> de correr contra Neon sin querer.

```bash
export EC_DB_URL="jdbc:postgresql://localhost:5432/empenya"
export EC_DB_USER="postgres"
export EC_DB_PASSWORD="postgres"
export JWT_SECRET="una-llave-local-de-al-menos-32-caracteres"
```

En PowerShell:

```powershell
$env:EC_DB_URL = "jdbc:postgresql://localhost:5432/empenya"
$env:EC_DB_USER = "postgres"
$env:EC_DB_PASSWORD = "postgres"
$env:JWT_SECRET = "una-llave-local-de-al-menos-32-caracteres"
```

## 3. Levantar

```bash
mvn spring-boot:run
```

Queda en **http://localhost:8080/api** (hay un `context-path: /api`, así que
todas las rutas lo llevan).

| Qué | URL |
|---|---|
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/api-docs |
| Health | http://localhost:8080/api/actuator/health |
| Login | `POST http://localhost:8080/api/auth/login` |

`spring-boot-devtools` está incluido, así que recompilar recarga la app sola.

## Compilar y empaquetar

```bash
mvn clean package          # genera target/empenya-confiable-api-1.0.0.jar
mvn clean package -DskipTests
java -jar target/empenya-confiable-api-1.0.0.jar
```

## Docker

```bash
docker build -t prestamo-confiable-api .
docker run -p 8080:8080 \
  -e EC_DB_URL=... -e EC_DB_USER=... -e EC_DB_PASSWORD=... \
  -e JWT_SECRET=... \
  prestamo-confiable-api
```

## Despliegue

Va a [Fly.io](https://fly.io) con [`fly.toml`](fly.toml) (app
`prestamo-confiable-api`, región `iad`, healthcheck en
`/api/actuator/health`):

```bash
fly deploy
fly secrets set EC_DB_URL=... EC_DB_PASSWORD=... JWT_SECRET=...
```

## Estados de un pago

Cada préstamo genera una corrida de 14 pagos semanales. El estado lo recalcula
`AbonoService.recalcularEstado` cada vez que entra un abono:

| Estado | Significado | Color en la UI |
|---|---|---|
| `PENDIENTE` | futuro, sin vencer | gris |
| `PROXIMO` | el siguiente a cobrar | azul |
| `ABONO_PARCIAL` | tiene abonos que no cubren el monto | amarillo |
| `PAGADO_SIN_CORTE` | cubierto, pendiente del corte semanal | naranja |
| `PAGADO` | cubierto y ya incluido en un corte | verde |
| `ATRASADO` | venció sin un solo abono | rojo |

`ABONO_PARCIAL` gana sobre `ATRASADO` a propósito: un pago vencido con dinero
encima no puede verse igual que uno donde el cliente no ha dado nada. Un job
diario (00:05 America/Mexico_City) en `CorteService` pone al día los vencidos.
