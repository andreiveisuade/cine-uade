# cine-docker

Orquestación: levanta backend y frontend desde las carpetas hermanas del monorepo, más MySQL y
Adminer.

Cómo ponerlo a andar, el seed y qué hacer si algo falla:
[`_other/COMO-LEVANTARLO.md`](../_other/COMO-LEVANTARLO.md).

```sh
./setup.sh          # o .\setup.ps1 en Windows
```

| Archivo | Qué es |
|---|---|
| `docker-compose.yml` | Los cuatro servicios, las dos redes y el volumen de la base |
| `.env.example` | Las variables, comentadas. Se copia a `.env`, que no se versiona |
| `setup.sh`, `setup.ps1` | Arman el `.env`, levantan, esperan a que esté sano y siembran. Repetibles |
| `seed/02-admin.sql` | Los dos usuarios de demo, al crear la base |
| `seed/datos-de-ejemplo.sh` | Seis salas, la carta del candy y una promoción, por la API |

## Los servicios

| Servicio | Imagen | Puerto en el host | Quién lo alcanza |
|---|---|---|---|
| frontend | nginx sin root (lo construye `cine-frontend/`) | **8080** (`PUERTO_WEB`) | el navegador y Swing |
| backend | eclipse-temurin 21 JRE (lo construye `cine-backend/`) | — | nginx, por `backend:8080` |
| mysql | mysql:8.4 | — | backend y Adminer, por `mysql:3306` |
| adminer | adminer:5 | 8081 (`PUERTO_ADMINER`), solo en `127.0.0.1` | el navegador de esta máquina |

**Un solo puerto sale al host.** El navegador y el panel de escritorio nunca hablan con el
backend directo: nginx reenvía `/api` y Swagger por la red interna, así que todo sale del mismo
origen y no hace falta CORS. Qué más hace nginx (tope de 1 MB, errores propios en JSON,
timeouts) está en [`cine-frontend/README.md`](../cine-frontend/README.md#nginx).

Dos redes separadas: **web** (frontend ↔ backend) y **datos** (backend y adminer ↔ mysql). El
frontend no tiene ruta hasta la base. El backend es el único en las dos.

Arrancan en cadena: `mysql` healthy → `backend` healthy (contesta `/api/generos`) → `frontend`.

Diagramas en el [manual](../_other/docs/manual/index.html#correr): topología y orden de arranque.

## Variables

Todas en `.env`, con su porqué en `.env.example`:

| Variable | Para qué |
|---|---|
| `MYSQL_ROOT_PASSWORD` | Solo para que MySQL se inicialice; la app nunca entra con root |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | La base y el usuario de la app, con permisos solo sobre esa base |
| `PUERTO_WEB`, `PUERTO_ADMINER` | Los puertos del host (8080 y 8081) |
| `TZ` | Zona horaria de los contenedores; sin ella corren en UTC y el arqueo del día pierde las funciones de la noche |
| `TMDB_TOKEN`, `TMDB_REGION` | El importador de cartelera. Sin token el sistema levanta igual y el Importador avisa que falta |

Si falta una obligatoria, Compose no arranca y dice cuál (`falta DB_USER, copiá .env.example a
.env`). Un cambio de clave no alcanza a una base ya creada: MySQL la guardó al inicializarse.

## La base

MySQL corre `schema.sql` y `seed/02-admin.sql` **la primera vez**, con el volumen vacío.
De ahí salen los dos usuarios de demo, que no tienen endpoint de alta: `encargado@cine.uade.ar`
(administrador) y `puerta@cine.uade.ar` (acomodador), los dos con `cine2026`. La clave está
guardada en el formato viejo y se re-hashea a bcrypt en el primer login.

Sobre una base ya creada, aplicar a mano los `migracion-*.sql` que falten (cada uno dice qué
agrega), en el orden en que se sumaron: `programaciones`, `grilla-abierta`, `limpieza`,
`staging`, `puntaje`, `votos`, `importaciones`, `bcrypt`, `bloqueos`, `version-reserva`,
`ahorro-congelado` y `email-minusculas`. Por ejemplo, la tabla de los bloqueos de butaca:

```sh
docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < ../cine-backend/src/main/resources/migracion-bloqueos.sql
```

Las variables van entre comillas simples a propósito: las resuelve el contenedor, que ya las
tiene del `.env`, y no la terminal. Si falta una migración que cambia tablas, el backend no
arranca: corre con `ddl-auto: validate` y el log dice qué tabla o columna no coincide. La de
`email-minusculas` solo cambia datos, así que su falta no se nota al arrancar sino al no
encontrar un usuario guardado con mayúsculas.

Empezar de cero, **borrando todos los datos**: `docker compose down -v && docker compose up -d
--build` y volver a sembrar.

Para mirar los datos, Adminer en `localhost:8081` — servidor **`mysql`**, no `localhost`, con
`DB_USER` y `DB_PASSWORD`. O una consola:

```sh
docker compose exec mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
```

El seed de ejemplo (`seed/datos-de-ejemplo.sh`) entra **por la API y no por SQL**: los
datos pasan por las mismas reglas que aplica el sistema. Qué carga y cómo correrlo, en
[`COMO-LEVANTARLO.md`](../_other/COMO-LEVANTARLO.md#a-mano-en-cine-docker).

El panel del encargado de escritorio (`cine-swing/`) se conecta a este mismo `localhost:8080`:
con el sistema arriba, `cd ../cine-swing && mvn exec:java` (JDK 21).

## Ajustes de tu máquina

Compose lee `docker-compose.override.yml` solo, sin flags. No se versiona. El uso típico
es publicar MySQL para un cliente de escritorio (Workbench, DBeaver), en `127.0.0.1:3306`,
base `appsinteractivas`:

```yaml
services:
  mysql:
    ports:
      - "127.0.0.1:3306:3306"
```

## Los bloqueos de butaca viven en MySQL

La butaca que alguien está eligiendo queda apartada tres minutos en la tabla
`bloqueo_butaca`, en la misma base que las entradas. Antes era un contenedor de Redis
aparte; ahora es una sola fuente de verdad y un servicio menos. Los vencidos los borra el
backend cada cinco minutos.

La garantía contra la doble venta no es esa tabla sino el `UNIQUE (funcion_id, asiento_id)`
de `entrada`: el bloqueo solo evita que dos personas elijan la misma butaca a la vez.

Sobre un despliegue que todavía tiene Redis: aplicar `migracion-bloqueos.sql` (arriba) y
levantar con `docker compose up -d --build --remove-orphans`, que baja el contenedor
`cine-redis` que ya no está en el compose.
