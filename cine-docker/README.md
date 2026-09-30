# cine-docker

Levanta el sistema completo desde las carpetas hermanas. Cómo ponerlo a andar y qué hacer si algo falla:
[`COMO-LEVANTARLO.md`](../_other/COMO-LEVANTARLO.md).

| Archivo | Qué es |
|---|---|
| `docker-compose.yml` | Los cuatro servicios, las dos redes y el volumen de la base |
| `.env.example` | Las variables, comentadas. Se copia a `.env`, que no se versiona |
| `setup.sh`, `setup.ps1` | Arman el `.env`, levantan, esperan a que esté sano y siembran. Repetibles |
| `seed/02-admin.sql` | Los dos usuarios de demo, al crear la base |
| `seed/datos-de-ejemplo.sh` | Seis salas, la carta del candy y una promoción, por la API y no por SQL, para que pasen por las reglas |

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

## Variables

Todas en `.env`, explicadas en `.env.example`:

| Variable | Para qué |
|---|---|
| `MYSQL_ROOT_PASSWORD` | Solo para inicializar MySQL; la app nunca entra con root |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | La base y el usuario de la app |
| `PUERTO_WEB`, `PUERTO_ADMINER` | Los puertos del host |
| `TZ` | Zona horaria; en UTC la caja del día pierde las funciones de la noche |
| `TMDB_TOKEN`, `TMDB_REGION` | El importador de cartelera; sin token el resto anda igual |

Si falta una obligatoria, Compose no arranca y dice cuál. Cambiar una clave no alcanza a una base ya creada.

## La base

MySQL corre `schema.sql` y `seed/02-admin.sql` solo con el volumen vacío. Sobre una base ya creada hay que
aplicar a mano los `migracion-*.sql` que falten, en este orden: `programaciones`, `grilla-abierta`,
`limpieza`, `staging`, `puntaje`, `votos`, `importaciones`, `bcrypt`, `bloqueos`, `version-reserva`,
`ahorro-congelado` y `email-minusculas`.

```sh
docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < ../cine-backend/src/main/resources/migracion-bloqueos.sql
```

Las comillas simples hacen que las variables las resuelva el contenedor, no la terminal. Si falta una
migración que cambia tablas, el backend no arranca (`ddl-auto: validate`) y el log dice qué no coincide.

Para mirar los datos, Adminer en `localhost:8081`, con servidor **`mysql`** (no `localhost`). Para Workbench o
DBeaver, publicar el puerto en un `docker-compose.override.yml`, que Compose lee solo y no se versiona:

```yaml
services:
  mysql:
    ports:
      - "127.0.0.1:3306:3306"
```
