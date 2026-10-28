# cine-docker

Levanta el sistema completo desde las carpetas hermanas. Cómo ponerlo a andar y qué hacer si algo falla:
[`COMO-LEVANTARLO.md`](../_other/COMO-LEVANTARLO.md).

| Archivo | Qué es |
|---|---|
| `docker-compose.yml` | Los cuatro servicios, las dos redes y el volumen de la base |
| `.env.example` | Las variables, comentadas una por una. Se copia a `.env`, que no se versiona. Si falta una obligatoria, Compose no arranca y dice cuál |
| `seed/02-admin.sql` | Los dos usuarios de demo, al crear la base |

Los scripts (`setup`, `seed/datos-de-ejemplo.sh`, que siembra por la API salas, candy, seis películas con una semana de grilla y una promo, y `aiven/`)
están en la [tabla de scripts](../README.md#scripts).

## Servicios

| Servicio | Imagen | Puerto en el host |
|---|---|---|
| frontend | nginx (construido desde `cine-frontend/`) | **8080** (`PUERTO_WEB`) |
| backend | eclipse-temurin 21 (construido desde `cine-backend/`) | — |
| mysql | mysql:8.4 | — |
| adminer | adminer:5 | 8081 (`PUERTO_ADMINER`), solo en `127.0.0.1` |

Un solo puerto sale al host: nginx reenvía `/api` y Swagger al backend, así que todo sale del mismo origen y
no hace falta CORS. Dos redes: **web** (frontend y backend) y **datos** (backend, adminer y mysql). El
frontend no tiene ruta hasta la base. Arrancan en cadena: `mysql` sano → `backend` sano → `frontend`.
Diagramas en el [manual](../_other/docs/manual/index.html#correr).

## La base

MySQL corre `db/schema.sql` y `seed/02-admin.sql` solo con el volumen vacío. Sobre una base ya creada hay que
aplicar a mano las de `cine-backend/src/main/resources/db/migraciones/` que falten, en el orden del número
(`01-programaciones.sql` … `20-candy-retirado.sql`).

```sh
docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < ../cine-backend/src/main/resources/db/migraciones/09-bloqueos.sql
```

Las comillas simples hacen que las variables las resuelva el contenedor, no la terminal. Si falta una
migración que cambia tablas, el backend no arranca (`ddl-auto: validate`) y el log dice qué no coincide.

Para Workbench o DBeaver (Adminer: [Cómo levantarlo](../_other/COMO-LEVANTARLO.md#usarlo)), publicar el puerto en un `docker-compose.override.yml`, que Compose lee solo y no se versiona:

```yaml
services:
  mysql:
    ports:
      - "127.0.0.1:3306:3306"
```

## Render y Aiven

En Render `ddl-auto: validate` no crea tablas y nadie corre `schema.sql`, así que la base de Aiven se prepara
una vez a mano:

```sh
./aiven/cargar-aiven.sh
```

Pide host, puerto, usuario y contraseña de Aiven, y la clave nueva de `encargado@` y `puerta@` (sin eco, dos
veces, mínimo 8: no quedan en el historial). Corre el cliente de la imagen `mysql:8.4` por `docker run --rm`,
carga `schema.sql` y los empleados, y se niega a correr si `defaultdb` ya tiene tablas. `cine2026` es solo
de demo local y no puede quedar en internet.

Los datos de ejemplo contra el sitio público van por la misma API:

```sh
API=https://cine-uade.onrender.com/api ADMIN='encargado@cine.uade.ar:<clave>' ./seed/datos-de-ejemplo.sh
```

`ADMIN` es `email:clave` del encargado, la clave nueva de Aiven. Las imágenes viven en la tabla `imagen`, no en disco: ni el
compose ni Render llevan volumen de imágenes. Qué variables carga Render: [`cine-backend`](../cine-backend/README.md#variables-de-entorno).

