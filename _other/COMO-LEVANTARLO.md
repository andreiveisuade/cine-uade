# Cómo levantarlo

Requisito: **Docker Desktop abierto**.

## Con el script

```bash
git clone https://github.com/andreiveisuade/cine-uade.git
cd cine-uade/cine-docker
./setup.sh          # macOS y Linux
.\setup.ps1         # Windows (si lo bloquea: powershell -ExecutionPolicy Bypass -File .\setup.ps1)
```

Arma el `.env`, pide el token de TMDB, levanta, siembra e imprime las URLs. Repetible: no pisa
el `.env` ni siembra dos veces.

## A mano (en `cine-docker/`)

```bash
cp .env.example .env                 # cambiar MYSQL_ROOT_PASSWORD y DB_PASSWORD (locales, no se versionan)
docker compose up -d --build         # la primera vez tarda: Maven baja dependencias
docker compose ps                    # repetir hasta mysql y backend (healthy); antes 8080 no responde
./seed/datos-de-ejemplo.sh           # 6 salas, carta del candy y una promoción
```

Películas no se siembran: **Importador** del panel (con `TMDB_TOKEN`) → confirmar en **Por
revisar** → funciones desde **Grilla** o **Planificador**.

| | URL | Credenciales |
|---|---|---|
| Cliente | <http://localhost:8080> | — |
| Panel | <http://localhost:8080/admin.html> | `encargado@cine.uade.ar` / `cine2026` |
| Puerta | el mismo panel | `puerta@cine.uade.ar` / `cine2026` |
| Swagger | <http://localhost:8080/swagger-ui.html> (contrato crudo en `/v3/api-docs`, importable en Postman) | — |
| Adminer | <http://localhost:8081> | servidor `mysql`, usuario del `.env` |

## Token de TMDB

Opcional: sin él solo falta el importador. Andrei lo pasa por privado (repo público: si se
filtra, se revoca). En `cine-docker/.env`, una línea sin comillas:

```
TMDB_TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4ZTM...
```

```bash
docker compose up -d backend
git config core.hooksPath .githooks   # hook anti-credenciales; setup.sh ya lo activa
```

| Síntoma | Causa |
|---|---|
| Botón del importador deshabilitado | Falta el token |
| Trae 0 películas, sin error | Se cortó al copiar |
| No autorizado | Mal pegado o revocado |

Uno propio: cuenta en [themoviedb.org](https://www.themoviedb.org/signup) → [API](https://www.themoviedb.org/settings/api),
tipo Developer, copiar el **API Read Access Token** (empieza con `eyJ`), no la API key.

## Tomcat aparte

```bash
cd cine-backend && mvn package -DskipTests    # target/cine-api.war, WAR ejecutable
java -jar target/cine-api.war                 # embebido, localhost:8080
```

O copiarlo a `$CATALINA_HOME/webapps/` (queda en `/cine-api`; como `ROOT.war`, en la raíz).
Base por `DB_HOST`, `DB_USER`, `DB_PASSWORD` (en `setenv.sh`).

## Si algo falla

| Lo que ves | Qué hacer |
|---|---|
| `Cannot connect to the Docker daemon` | Abrir Docker Desktop |
| `falta DB_USER, copiá .env.example a .env` | `cp .env.example .env` |
| `port is already allocated` | Cambiar `PUERTO_WEB` en el `.env` |
| `localhost:8080` no responde | `docker compose ps` hasta `(healthy)` |
| `backend` reinicia en loop | `docker compose logs backend` (casi siempre el `.env`) |
| La web carga vacía | Falta sembrar |
| Cambiaste el `.env` y sigue igual | El volumen tiene la clave vieja: `down -v` |

```bash
docker compose down -v && docker compose up -d --build && ./seed/datos-de-ejemplo.sh   # de cero
```

## Día a día

```bash
docker compose logs -f backend
docker compose up -d --build --no-deps frontend    # tras tocar el front (--no-deps: no recrea MySQL)
docker compose up -d --build --no-deps backend     # tras tocar el back
docker compose restart backend                     # sin recompilar
docker compose down                                # bajar, la base queda
```

MySQL sin puerto publicado. Para Workbench/DBeaver, `cine-docker/docker-compose.override.yml`
(no versionado) y volver a levantar; conectar a `127.0.0.1:3306`, base `appsinteractivas`:

```yaml
services:
  mysql:
    ports:
      - "127.0.0.1:3306:3306"
```

## Tests

419 pruebas contra H2, sin Docker ni MySQL. El `clean` evita correr clases viejas de `target/`.

```bash
cd cine-backend && mvn clean test
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn -B clean test   # sin Java ni Maven
```
