# cine-docker

Orquestación: levanta backend y frontend desde las carpetas hermanas del monorepo.

Cómo ponerlo a andar: [`_other/COMO-LEVANTARLO.md`](../_other/COMO-LEVANTARLO.md).

```sh
./setup.sh          # o .\setup.ps1 en Windows
```

## Los servicios

| Servicio | Imagen | Puerto en el host | Quién lo alcanza |
|---|---|---|---|
| frontend | nginx | **8080** | el navegador |
| backend | temurin 21 | — | nginx, por `backend:8080` |
| mysql | mysql:8.4 | — | backend y Adminer, por `mysql:3306` |
| adminer | adminer:5 | 8081, solo en `127.0.0.1` | el navegador de esta máquina |

**Un solo puerto sale al host.** El navegador nunca habla con el backend directo: nginx
reenvía `/api` por la red interna, así que todo sale del mismo origen y no hace falta CORS.

Dos redes separadas: **web** (frontend ↔ backend) y **datos** (backend y adminer ↔ mysql). El frontend no tiene ruta hasta la base. El backend es el único en las dos.

Arrancan en cadena: `mysql` healthy → `backend` healthy → `frontend`.

Diagramas en el [manual](../_other/docs/manual/index.html#correr): topología y orden de arranque.

## La base

MySQL corre `schema.sql` y `seed/02-admin.sql` **la primera vez**, con el volumen vacío.
De ahí sale el administrador, que no tiene endpoint de alta.

Sobre una base ya creada, aplicar a mano el `migracion-*.sql` que falte (cada uno dice qué
agrega). Por ejemplo, la tabla de los bloqueos de butaca:

```sh
docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appsinteractivas \
  < ../cine-backend/src/main/resources/migracion-bloqueos.sql
```

Empezar de cero: `docker compose down -v && docker compose up -d`.

El panel del encargado de escritorio (`cine-swing/`) se conecta a este mismo `localhost:8080`:
con el sistema arriba, `cd ../cine-swing && mvn exec:java` (JDK 21).

Para mirar los datos, Adminer en `localhost:8081` — servidor **`mysql`**, no `localhost`.
O una consulta suelta:

```sh
docker compose exec mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appsinteractivas
```

El seed de ejemplo (`seed/datos-de-ejemplo.sh`) entra **por la API y no por SQL**: los
datos pasan por las mismas reglas que aplica el sistema.

## Ajustes de tu máquina

Compose lee `docker-compose.override.yml` solo, sin flags. No se versiona. El uso típico
es publicar MySQL para un cliente de escritorio:

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
