# Cómo levantarlo

Requisito: **Docker Desktop abierto**. Para el panel de escritorio, además, JDK 21 y Maven.

## Con el script

```bash
git clone https://github.com/andreiveisuade/cine-uade.git
cd cine-uade/cine-docker
./setup.sh          # macOS y Linux
.\setup.ps1         # Windows (si lo bloquea: powershell -ExecutionPolicy Bypass -File .\setup.ps1)
```

Chequea Docker, activa el hook anti-credenciales, arma el `.env` con contraseñas generadas al
azar, pide el token de TMDB (Enter lo saltea), levanta, espera hasta 5 minutos a que `mysql` y
`backend` estén sanos, siembra e imprime las URLs. Repetible: no pisa el `.env` ni siembra si ya
hay salas.

## A mano (en `cine-docker/`)

```bash
cp .env.example .env                 # cambiar MYSQL_ROOT_PASSWORD y DB_PASSWORD (locales, no se versionan)
docker compose up -d --build         # la primera vez tarda: Maven baja dependencias
docker compose ps                    # repetir hasta mysql y backend (healthy); antes 8080 no responde
./seed/datos-de-ejemplo.sh           # 6 salas, la carta del candy y la promo «Miércoles 2x1»
```

El seed entra por la API con el usuario del encargado, así que los datos pasan por las mismas
reglas que el sistema. La promoción vale desde hoy y **vence a los 90 días**: pasado ese plazo,
volver a correr el seed o cargar otra desde el panel. No es idempotente: correrlo dos veces da
`409` de nombre repetido, inofensivos. Si cambiaste `PUERTO_WEB`, pasale la URL:
`API=http://localhost:9090/api ./seed/datos-de-ejemplo.sh`. En Windows necesita `sh` y `curl`
(Git Bash); `setup.ps1` lo corre en un contenedor.

## Panel del encargado (Swing)

Con el sistema levantado, desde otra terminal:

```sh
cd cine-swing
mvn exec:java                              # o: mvn package && java -jar target/cine-swing.jar
```

Apunta a `http://localhost:8080`; otro servidor con `-Dcine.api.url=...` o `CINE_API_URL`. Tema
claro con `-Dcine.tema=claro`. Detalle en [`cine-swing/README.md`](../cine-swing/README.md).

Películas no se siembran: **Importador** del panel (con `TMDB_TOKEN`) → confirmar en **Por
revisar** → funciones desde **Funciones**, **Grilla** o **Planificador**.

| | URL | Credenciales |
|---|---|---|
| Cliente | <http://localhost:8080> | — |
| Panel del encargado | `cine-swing`, `mvn exec:java` | `encargado@cine.uade.ar` / `cine2026` (ve todo) |
| Puerta | el mismo panel de escritorio | `puerta@cine.uade.ar` / `cine2026` (solo Puerta) |
| Swagger | <http://localhost:8080/swagger-ui.html> (contrato crudo en `/v3/api-docs`, importable en Postman) | las rutas del encargado piden Basic con el mismo usuario |
| Adminer | <http://localhost:8081> | servidor `mysql`, usuario y clave `DB_USER` / `DB_PASSWORD` del `.env` |

Los dos usuarios los siembra `seed/02-admin.sql` al crear la base; no hay endpoint de alta de
empleados.

## Token de TMDB

Opcional: sin él solo falta el importador. Andrei lo pasa por privado (repo público: si se
filtra, se revoca). En `cine-docker/.env`, una línea sin comillas:

```
TMDB_TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4ZTM...
```

```bash
docker compose up -d backend          # recrea el backend con el .env nuevo
git config core.hooksPath .githooks   # hook anti-credenciales; setup.sh ya lo activa
```

Lo que muestra la pantalla **Importador**:

| Mensaje | Causa |
|---|---|
| «Falta el token de TMDB…», con el botón deshabilitado | `TMDB_TOKEN` vacío o el backend no se recreó después de cargarlo |
| «TMDB rechazó el token: revisá TMDB_TOKEN» | Mal pegado, cortado, revocado, o es la API key corta y no el token |
| «No se pudo llegar a TMDB: revisá la conexión a internet» | El backend no tiene salida a internet |

Uno propio: cuenta en [themoviedb.org](https://www.themoviedb.org/signup) → [API](https://www.themoviedb.org/settings/api),
tipo Developer, copiar el **API Read Access Token** (empieza con `eyJ`), no la API key.

## Sin Docker, o en un Tomcat aparte

Hace falta un MySQL con la base ya creada con `cine-backend/src/main/resources/schema.sql`
(Hibernate corre en `ddl-auto: validate`: no crea tablas, y si no coinciden no arranca) y el
primer usuario de `cine-docker/seed/02-admin.sql`.

```bash
cd cine-backend && mvn package -DskipTests    # target/cine-api.war, WAR ejecutable
java -jar target/cine-api.war                 # embebido, localhost:8080
mvn spring-boot:run                           # lo mismo, sin empaquetar
```

O copiarlo a `$CATALINA_HOME/webapps/` (queda en `/cine-api`; como `ROOT.war`, en la raíz).
La base se configura con `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` (en Tomcat,
en `setenv.sh`); sin ellas usa `localhost:3306/appsinteractivas` con `root` / `root`. También
leen el entorno `TMDB_TOKEN`, `TMDB_REGION` y `PORT`. Así se sirve solo la API y Swagger: la web
del cliente la sirve el nginx del compose, o `npm run dev` apuntando a este 8080.

## Si algo falla

| Lo que ves | Qué hacer |
|---|---|
| `Cannot connect to the Docker daemon` | Abrir Docker Desktop |
| `falta DB_USER, copiá .env.example a .env` | `cp .env.example .env` |
| `port is already allocated` | Cambiar `PUERTO_WEB` (o `PUERTO_ADMINER`) en el `.env`; el seed y Swing se apuntan al puerto nuevo como se dice arriba |
| `localhost:8080` no responde | `docker compose ps` hasta `(healthy)` |
| «El servidor no está disponible: volvé a intentarlo en un rato» | nginx no llega al backend (502): está arrancando o se cayó. `docker compose logs backend` |
| `backend` reinicia en loop | `docker compose logs backend` (casi siempre el `.env`) |
| La web carga vacía | Falta sembrar o importar películas y programar funciones |
| No aparece la promo del seed | Venció: vale 90 días desde que se sembró |
| Cambiaste el `.env` y sigue igual | La base ya se creó con la clave vieja: `docker compose down -v`, **que borra los datos** |
| El backend no levanta tras un `git pull` (`ddl-auto: validate`) | La base es vieja: aplicar los `migracion-*.sql` que falten (ver [`cine-docker/README.md`](../cine-docker/README.md#la-base)) o empezar de cero |

```bash
docker compose down -v && docker compose up -d --build && ./seed/datos-de-ejemplo.sh   # de cero, borra la base
```

## Día a día

```bash
docker compose logs -f backend
docker compose up -d --build --no-deps frontend    # tras tocar el front (--no-deps: no recrea MySQL)
docker compose up -d --build --no-deps backend     # tras tocar el back
docker compose restart backend                     # sin recompilar
docker compose down                                # bajar, la base queda
```

Para desarrollar el front con recarga en caliente, con el sistema levantado:
`cd cine-frontend && npm install && npm run dev` (en `localhost:5173`; Vite reenvía `/api`
al 8080). Detalle en [`cine-frontend/README.md`](../cine-frontend/README.md).

MySQL no publica puerto. Para Workbench o DBeaver, ver
[`cine-docker/README.md`](../cine-docker/README.md#ajustes-de-tu-máquina).

## Tests

Backend: 1149 pruebas contra H2 en modo MySQL creada con el `schema.sql` real, sin Docker ni
MySQL; tarda alrededor de un minuto. Swing: 60, contra un servidor HTTP falso y sin pantalla.
El `clean` evita correr clases viejas de `target/`. No corras dos `mvn test` a la vez sobre la
misma carpeta.

```bash
cd cine-backend && mvn clean test
mvn test -Dtest=GestorPagosTest                     # una clase
cd ../cine-swing && mvn clean test
```

Sin Java ni Maven instalados, desde `cine-backend/` o `cine-swing/`:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn -B clean test
```

El frontend no tiene tests; `npm run build` en `cine-frontend/` verifica que compile.
