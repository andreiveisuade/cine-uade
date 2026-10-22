# cine-backend

API REST y reglas de negocio. Java 21, Spring Boot 3.5, JPA, Spring Security y MySQL 8.4. Es lo único que
habla con la base: la web, Swing y Android son clientes de esta misma API y no deciden nada. El contrato
HTTP está en [`API.md`](../cine-frontend/API.md); las reglas, en el [manual](../_other/docs/manual/index.html).

## Correrlo

Normalmente con [`cine-docker`](../cine-docker/README.md). A mano, con una MySQL local:

```bash
mvn spring-boot:run            # contra localhost:3306 (variables DB_*)
timeout 600 mvn test           # perfil test, base H2 en memoria, sin MySQL
```

## Perfiles

| Perfil | Base | Para qué |
|---|---|---|
| (ninguno) | MySQL, por `DB_*` o `SPRING_DATASOURCE_*` | Docker local y Render |
| `test` | H2 en memoria (`application-test.yml`) | Los tests lo activan solos |

`ddl-auto: validate`: el backend no crea tablas. `db/schema.sql` y las `db/migraciones/NN-*.sql` se aplican a mano
(en el orden del número; comando en [`cine-docker`](../cine-docker/README.md#la-base)) y, si algo no coincide, no arranca y el log dice qué.

## Variables de entorno

Sin valor por defecto sensible: en producción se cargan en el panel de Render (`sync: false` en `render.yaml`).

| Variable | Para qué |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | La base de producción (Aiven); pisan a `DB_*` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | La base en desarrollo y en el compose |
| `DB_POOL` | Conexiones del pool; 10 por defecto, 5 en Render (Aiven free) |
| `MP_ACCESS_TOKEN` | Mercado Pago, credenciales de prueba. Vacío, pasarela emulada |
| `CINE_URL_PUBLICA` | A dónde vuelve el comprador de Mercado Pago, `https://cine-uade.onrender.com` en producción |
| `TMDB_TOKEN`, `TMDB_REGION` | Importador de cartelera. Sin token el resto anda igual |
| `CINE_SWAGGER` | `false` apaga Swagger UI y `/v3/api-docs`; `render.yaml` lo fija en `false` |
| `PORT` | Puerto HTTP (8080); lo pone Render |
| `TZ` | Zona horaria del proceso; la hora del cine es fija en Buenos Aires aunque el contenedor esté en UTC |
| `INCAA_RAZON_SOCIAL`, `INCAA_CUIT`, `INCAA_NUMERO_EXHIBIDOR` | Encabezado de la declaración jurada (ficticios por defecto) |
| `CINE_TICKETS` | Carpeta de los tickets `.txt` |

## Dockerfile

El de esta carpeta arma solo el backend (build con Maven, runtime con JRE) y lo usa el compose, que pone nginx
adelante. El [`Dockerfile` de la raíz](../Dockerfile) es la imagen de Render: compila también el front y lo
sirve el backend, así web y API comparten origen `https`. Las imágenes de películas viven en la tabla `imagen`,
no en disco.

## Capas

`controller` → `service` → `repository`, con `dto` en el borde y `model` sin lógica de HTTP. Las reglas
rechazan con `Rechazo`, que el controlador convierte en `{"error": "…"}`. Convenciones completas en el
`CLAUDE.md` del repo.
