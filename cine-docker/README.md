# cine-docker

Orquestación del TP. Levanta los servicios del sistema desde los repos hermanos.

```
TPO/
├── cine-backend/    Java 21 + JDBC + MySQL, con la API HTTP en ar.uade.cine.api
├── cine-frontend/   HTML + JS + Tailwind servido por nginx
└── cine-docker/     este repo: el compose
```

## Levantarlo

```sh
cp .env.example .env     # y cambiar las contraseñas
docker compose up -d --build
```

La aplicación queda en <http://localhost:8080> y Adminer en <http://localhost:8081>.

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
| parser | — | no recibe nada: sale a TMDB y le pega al backend |

Diagrama de esta misma topología (contenedores, redes, volúmenes) en
[`cine-backend/docs/manual/index.html`](../cine-backend/docs/manual/index.html#correr), sección
"Cómo correrlo".

El navegador nunca habla con el backend directo: `backend` es un nombre que solo resuelve
adentro de Docker. nginx reenvía `/api` por la red interna, así que para el navegador todo
sale del mismo origen y no hace falta CORS.

Las dos redes están separadas: el contenedor que sirve la web no tiene ruta hasta la base.
El backend es el único que está en las dos.

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

`parser` trae de TMDB las películas que están hoy en cartelera en Argentina y las carga
por HTTP contra `POST /api/peliculas`. Vive en su propio repo, `cine-pelis-parser`.

No arranca con el resto: está detrás del perfil `importador`, para no gastar llamadas a
TMDB cada vez que se levanta el compose.

```bash
docker compose --profile importador run --rm parser --simular   # qué traería
docker compose --profile importador up -d parser                # queda corriendo cada 6h
docker compose logs -f parser                                   # qué está haciendo
```

Necesita `TMDB_TOKEN` en el `.env` — se saca gratis en
[themoviedb.org/settings/api](https://www.themoviedb.org/settings/api) y es el
*API Read Access Token*, el largo. Sin el token los demás servicios levantan igual: el
parser falla solo, al arrancar, diciendo qué le falta.

**Está solo en la red `web`, a propósito.** No tiene ruta hasta MySQL, así que no puede
escribir en la base ni queriendo: todo lo que carga pasa por las reglas de
`GestorCartelera` — R1 título único, R2 duración, R7 al menos un género, R10
clasificación. Es la misma razón por la que `seed/datos-de-ejemplo.sh` siembra por la API
y no por SQL.

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

## El menú de consola

Sigue funcionando: la API se sumó, no reemplazó nada. Como MySQL ya no publica el 3306,
la forma directa de usarlo contra esta base es correrlo adentro del contenedor del
backend, que ya está en la red interna y con las variables puestas:

```sh
docker compose exec backend java -cp /app/cine-api.jar ar.uade.cine.Main
```

Fuera de Docker, `mvn exec:java` sigue andando como siempre contra una base propia en
`localhost:3306`: los defaults de `ConexionMySQL` son esos justamente para que no haga
falta configurar nada.
