# cine-docker

Orquestación del TP. Levanta los servicios del sistema desde las carpetas hermanas.

```
cine-uade/
├── cine-backend/    Java 21 + Spring Boot + Spring Data JPA + MySQL, con la API en ar.uade.cine.controller
├── cine-frontend/   HTML + JS + Tailwind servido por nginx
└── cine-docker/     esta carpeta: el compose
```

## Levantarlo

El paso a paso completo, con verificación y qué hacer si algo falla, está en el
[`README.md` de la raíz](../README.md). Acá va el resumen:

```sh
cp .env.example .env     # y cambiar las contraseñas
docker compose up -d --build
docker compose ps        # esperar a que mysql y backend digan (healthy)
```

La aplicación queda en <http://localhost:8080> y Adminer en <http://localhost:8081>.

El frontend espera a que el backend esté sano, y el backend a que lo esté MySQL: hasta que
`docker compose ps` no muestre los dos `(healthy)`, el puerto 8080 no responde. No está
roto, todavía no le toca.

Para entrar al panel del encargado: `encargado@cine.uade.ar` / `cine2026`. Ese
administrador lo siembra `seed/02-admin.sql` la primera vez que arranca la base, porque no
hay endpoint de alta de administradores: el TP no modela quién los crea. Son credenciales
de demo, se cambian ahí.

Con la base vacía no hay nada que mirar. Para cargar un complejo de ejemplo —4 películas,
las 6 salas con sus 450 butacas y 8 funciones entre hoy y mañana:

```sh
./seed/datos-de-ejemplo.sh
```

Ese script va por la API y no por SQL a propósito: los datos pasan por las mismas reglas
que aplica el sistema, así que no puede sembrar nada que después la aplicación rechazaría.

## Qué se expone

Un solo puerto sale al host: el de nginx. El backend y MySQL no publican ninguno y solo
se los alcanza desde adentro de la red de Docker.

| Servicio | Puerto en el host | Cómo se lo alcanza |
|---|---|---|
| frontend | 8080 | el navegador |
| backend | — | `http://backend:8080`, desde nginx |
| mysql | — | `mysql:3306`, desde el backend y Adminer |
| redis | — | `redis:6379`, solo desde el backend |
| adminer | 8081, solo en 127.0.0.1 | el navegador de esta máquina |

Diagrama de esta misma topología (contenedores, redes, volúmenes) en
[`cine-backend/docs/manual/index.html`](../cine-backend/docs/manual/index.html#correr), sección
"Cómo correrlo".

El navegador nunca habla con el backend directo: `backend` es un nombre que solo resuelve
adentro de Docker. nginx reenvía `/api` por la red interna, así que para el navegador todo
sale del mismo origen y no hace falta CORS.

Las dos redes están separadas: el contenedor que sirve la web no tiene ruta hasta la base.
El backend es el único que está en las dos.

## Ajustes de tu máquina: `docker-compose.override.yml`

Docker Compose lee automáticamente un `docker-compose.override.yml` si existe, sin flags.
Ese archivo **no se versiona**: es donde cada uno pone lo que necesita en su máquina y no
es parte del sistema.

El caso más común es publicar el puerto de MySQL para abrirlo con Workbench o DBeaver,
porque el compose entregable lo deja solo en la red interna:

```yaml
services:
  mysql:
    ports:
      - "127.0.0.1:3306:3306"
```

Con eso conectás a `127.0.0.1:3306`, base `appsinteractivas`, usuario y clave del `.env`.

## Redis se puede apagar

`redis` guarda los bloqueos de butaca de mientras alguien elige, y es el único servicio del
compose que se puede bajar sin romper nada:

```sh
docker compose stop redis          # el cine sigue vendiendo
```

Con Redis apagado el mapa deja de mostrar como tomadas las butacas que alguien está
eligiendo, y esa butaca se pierde recién al confirmar —que es como funcionaba el sistema
antes de que Redis existiera—. Lo que **no** cambia es que una butaca no se vende dos
veces: eso lo garantiza el `UNIQUE (funcion_id, asiento_id)` de MySQL, no Redis. Por eso el
backend depende de él con `service_started` y no con `service_healthy` como de la base.

No tiene volumen y arranca con `--save ""`: lo que guarda vence en tres minutos, así que
persistirlo solo serviría para recuperar, después de un reinicio, bloqueos ya vencidos.

## El importador de cartelera

El backend trae de TMDB las películas que están hoy en cartelera en Argentina y las deja
en el buzón de revisión, en estado pendiente. **Lo dispara el encargado, desde el panel**:
*Importador* en el menú del admin. No corre solo: sin un pedido no gasta una sola llamada
a TMDB.

```bash
docker compose logs -f backend               # qué trajo la última corrida
```

Es la única llamada saliente del sistema. Vive dentro del backend, en
`infrastructure/importador/`: no hay contenedor aparte.

Necesita `TMDB_TOKEN` en el `.env` — se saca gratis en
[themoviedb.org/settings/api](https://www.themoviedb.org/settings/api) y es el
*API Read Access Token*, el largo. Sin el token el sistema levanta igual: la pantalla del
panel avisa que falta antes de que alguien apriete el botón.

**Lo que baja de TMDB no entra al catálogo**, entra al buzón: nace pendiente y fuera de
cartelera hasta que el encargado lo confirma. Y entra por `GestorRevisionCartelera`, o sea
pasando por las mismas reglas que el alta a mano — R1 título único, R2 duración, R7 al
menos un género, R10 clasificación. Es la misma razón por la que `seed/datos-de-ejemplo.sh`
siembra por la API y no por SQL.

## La base

MySQL ejecuta `cine-backend/src/main/resources/schema.sql` la primera vez, cuando el
volumen está vacío. Sobre una base ya creada hay que aplicarlo a mano:

```sh
docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appsinteractivas \
  < ../cine-backend/src/main/resources/schema.sql
```

Para empezar de cero y perder los datos: `docker compose down -v && docker compose up -d`.

En Adminer el servidor es `mysql`, no `localhost`: Adminer corre adentro de Docker y llega
a la base por el nombre del servicio. El usuario y la contraseña son los del `.env`, y
tiene permisos solo sobre `appsinteractivas`.

## Mirar la base a mano

La única puerta de entrada al sistema es la API HTTP: el importador y el seed escriben por
ahí también, justamente para que ninguna regla de negocio quede sin aplicar.

Para inspeccionar los datos está Adminer en `localhost:8081` (servidor `mysql`, no
`localhost`). Y para una consulta suelta:

```sh
docker compose exec mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appsinteractivas
```
