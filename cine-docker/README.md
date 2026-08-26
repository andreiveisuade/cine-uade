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
| redis | redis:8 | — | solo el backend |
| adminer | adminer:5 | 8081, solo en `127.0.0.1` | el navegador de esta máquina |

**Un solo puerto sale al host.** El navegador nunca habla con el backend directo: nginx
reenvía `/api` por la red interna, así que todo sale del mismo origen y no hace falta CORS.

Dos redes separadas: **web** (frontend ↔ backend) y **datos** (backend y adminer ↔ mysql,
backend ↔ redis). El frontend no tiene ruta hasta la base. El backend es el único en las dos.

Arrancan en cadena: `mysql` healthy → `backend` healthy → `frontend`.

Diagramas en el [manual](../_other/docs/manual/index.html#correr): topología y orden de arranque.

## La base

MySQL corre `schema.sql` y `seed/02-admin.sql` **la primera vez**, con el volumen vacío.
De ahí sale el administrador, que no tiene endpoint de alta.

Sobre una base ya creada, aplicar el schema a mano:

```sh
docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appsinteractivas \
  < ../cine-backend/src/main/resources/schema.sql
```

Empezar de cero: `docker compose down -v && docker compose up -d`.

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

## Redis se puede apagar

```sh
docker compose stop redis      # el cine sigue vendiendo
```

Guarda los bloqueos de butaca de mientras alguien elige. Apagado, el mapa deja de mostrar
como tomadas las que otro está eligiendo y esa butaca se pierde recién al confirmar.

Lo que **no** cambia: una butaca no se vende dos veces. Eso lo garantiza el
`UNIQUE (funcion_id, asiento_id)` de MySQL. Por eso el backend lo espera con
`service_started` y no con `service_healthy` como a la base.

Sin volumen y con `--save ""`: lo que guarda vence en tres minutos.

## El importador

Trae de TMDB lo que está hoy en cartelera en Argentina. **Lo dispara el encargado** desde
el panel: sin un pedido no gasta una llamada. Es la única llamada saliente del sistema, y
vive dentro del backend (`infrastructure/importador/`), sin contenedor aparte.

Necesita `TMDB_TOKEN` en el `.env`. Sin token el sistema levanta igual y la pantalla avisa.

Lo que baja **no entra al catálogo**: entra al buzón como pendiente, y pasa por las mismas
reglas que el alta a mano hasta que el encargado lo confirma.

```sh
docker compose logs -f backend      # qué trajo la última corrida
```
