# Cómo levantarlo

Requiere **Docker Desktop abierto**. El panel de escritorio, además, JDK 21 y Maven.

## Con el script

```bash
git clone https://github.com/andreiveisuade/cine-uade.git
cd cine-uade/cine-docker
./setup.sh          # Windows: .\setup.ps1 (si lo bloquea: powershell -ExecutionPolicy Bypass -File .\setup.ps1)
```

Arma el `.env` con contraseñas al azar, pide el token de TMDB (Enter lo saltea), levanta, espera a que esté
sano, siembra e imprime las URLs. Se puede correr de nuevo: no pisa el `.env` ni siembra dos veces.

## A mano (en `cine-docker/`)

```bash
cp .env.example .env                 # cambiar las dos contraseñas
docker compose up -d --build         # la primera vez tarda: Maven baja dependencias
docker compose ps                    # hasta que mysql y backend digan (healthy)
./seed/datos-de-ejemplo.sh           # 6 salas, la carta del candy y la promo «Miércoles 2x1»
git config core.hooksPath .githooks  # hook que frena commits con credenciales
```

La promo del seed vence a los 90 días. Correr el seed dos veces da `409` inofensivos. Con otro puerto:
`API=http://localhost:9090/api ./seed/datos-de-ejemplo.sh`.

## Usarlo

```sh
cd cine-swing && mvn exec:java       # el panel, en otra terminal
```

| | Dónde | Credenciales |
|---|---|---|
| Web del cliente | <http://localhost:8080> | — |
| Panel del encargado | `cine-swing` | `encargado@cine.uade.ar` / `cine2026` |
| Puerta | el mismo panel | `puerta@cine.uade.ar` / `cine2026` |
| Swagger | <http://localhost:8080/swagger-ui.html> | las rutas del encargado piden el mismo usuario |
| Adminer | <http://localhost:8081> | servidor `mysql`, `DB_USER` / `DB_PASSWORD` del `.env` |

Las películas no se siembran: **Importador** del panel → confirmarlas en **Por revisar** → funciones desde
**Funciones**, **Grilla** o **Planificador**.

## Token de TMDB

Opcional: sin él solo falta el importador. Andrei lo pasa por privado. Va en `cine-docker/.env`, sin comillas
(`TMDB_TOKEN=eyJ...`), y después `docker compose up -d backend`. Uno propio: cuenta en
[themoviedb.org](https://www.themoviedb.org/settings/api), tipo Developer, el **API Read Access Token**
(empieza con `eyJ`), no la API key.

| Mensaje del Importador | Causa |
|---|---|
| «Falta el token de TMDB…» | `TMDB_TOKEN` vacío, o el backend no se recreó |
| «TMDB rechazó el token…» | Mal pegado, revocado, o es la API key y no el token |
| «No se pudo llegar a TMDB…» | El backend no tiene internet |

## Sin Docker

Hace falta un MySQL con la base creada desde `cine-backend/src/main/resources/schema.sql` y el usuario de
`cine-docker/seed/02-admin.sql`.

```bash
cd cine-backend && mvn package -DskipTests && java -jar target/cine-api.war   # localhost:8080
```

El war también se puede copiar a `webapps/` de un Tomcat. La base se configura con `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USER` y `DB_PASSWORD` (por defecto `localhost:3306/appsinteractivas`, `root` / `root`). Así se
sirve solo la API; la web la sirve nginx, o `npm run dev`.

## Si algo falla

| Lo que ves | Qué hacer |
|---|---|
| `Cannot connect to the Docker daemon` | Abrir Docker Desktop |
| `falta DB_USER, copiá .env.example a .env` | `cp .env.example .env` |
| `port is already allocated` | Cambiar `PUERTO_WEB` o `PUERTO_ADMINER` en el `.env` |
| `localhost:8080` no responde, o «El servidor no está disponible» | El backend está arrancando o se cayó: `docker compose ps` y `docker compose logs backend` |
| La web carga vacía | Falta importar películas y programar funciones |
| Cambiaste el `.env` y sigue igual | La base se creó con la clave vieja: `docker compose down -v`, **que borra los datos** |
| El backend no levanta tras un `git pull` | Faltan migraciones: ver [La base](../cine-docker/README.md#la-base) |

## Día a día (en `cine-docker/`)

```bash
docker compose logs -f backend
docker compose up -d --build --no-deps backend     # tras tocar el back (o frontend, tras tocar el front)
docker compose down                                # bajar; la base queda
docker compose down -v                             # bajar y borrar la base
```

Tests: [`PRUEBAS.md`](docs/PRUEBAS.md).
