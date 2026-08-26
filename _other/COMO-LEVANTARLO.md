# Cómo levantarlo

Hace falta **Docker Desktop abierto**. Nada más.

## Con el script

```bash
git clone https://github.com/andreiveisuade/cine-uade.git
cd cine-uade/cine-docker

./setup.sh          # macOS y Linux
.\setup.ps1         # Windows
```

Arma el `.env`, pide el token de TMDB, levanta, espera y siembra los datos. Se puede
repetir: no pisa el `.env` ni siembra dos veces.

Windows, si PowerShell lo bloquea: `powershell -ExecutionPolicy Bypass -File .\setup.ps1`

Al terminar imprime las URLs. Listo.

## A mano

Todo parado en `cine-docker/`.

**1. Configurar**

```bash
cp .env.example .env
```

Cambiá `MYSQL_ROOT_PASSWORD` y `DB_PASSWORD` por cualquier cosa: son de tu MySQL, adentro
de tu Docker. No las comparte nadie. El `.env` no se versiona.

**2. Levantar**

```bash
docker compose up -d --build
```

La primera vez tarda minutos: Maven baja las dependencias del backend.

**3. Esperar**

Arrancan en cadena: `mysql` → `backend` → `frontend`. Hasta que el backend no esté sano,
el puerto 8080 no responde.

```bash
docker compose ps      # repetir hasta ver mysql y backend (healthy)
```

**4. Sembrar**

```bash
./seed/datos-de-ejemplo.sh      # 4 películas, 6 salas, 8 funciones
```

**5. Entrar**

| | URL | Credenciales |
|---|---|---|
| Cliente | <http://localhost:8080> | — |
| Panel | <http://localhost:8080/admin.html> | `encargado@cine.uade.ar` / `cine2026` |
| Puerta | el mismo panel | `puerta@cine.uade.ar` / `cine2026` |
| Adminer | <http://localhost:8081> | servidor `mysql`, usuario del `.env` |

## Token de TMDB

Sirve para importar la cartelera real. **Sin él todo lo demás anda igual**: lo único que
no vas a poder hacer es traer películas desde el importador.

**Andrei te lo pasa por privado.** No está en el repo: un token en un repo público lo
levanta cualquiera y TMDB lo revoca.

Pegalo en `cine-docker/.env`, sin comillas ni espacios, en una línea:

```
TMDB_TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4ZTM...
```

```bash
docker compose up -d backend
```

| Síntoma | Causa |
|---|---|
| El botón del importador está deshabilitado | Falta el token en el `.env` |
| Trae 0 películas, sin error | Se cortó al copiar |
| Dice que no está autorizado | Mal pegado, o revocado |

<details>
<summary>Sacar uno propio</summary>

Cuenta gratis en [themoviedb.org](https://www.themoviedb.org/signup) → [Configuración →
API](https://www.themoviedb.org/settings/api) → tipo **Developer**. Copiá el **API Read
Access Token** (largo, empieza con `eyJ`), no la API key corta.
</details>

## Si algo falla

| Lo que ves | Qué hacer |
|---|---|
| `Cannot connect to the Docker daemon` | Abrir Docker Desktop |
| `falta DB_USER, copiá .env.example a .env` | `cp .env.example .env` |
| `port is already allocated` | Cambiar `PUERTO_WEB` en el `.env` |
| `localhost:8080` no responde | Esperar: `docker compose ps` hasta `(healthy)` |
| `backend` reinicia en loop | `docker compose logs backend`. Casi siempre es el `.env` |
| La web carga vacía | Falta sembrar |
| Cambiaste el `.env` y sigue igual | El volumen tiene la clave vieja: `down -v` |

Empezar de cero:

```bash
docker compose down -v && docker compose up -d --build && ./seed/datos-de-ejemplo.sh
```

## Día a día

```bash
docker compose logs -f backend                     # ver qué hace
docker compose up -d --build --no-deps frontend    # tras tocar el front
docker compose up -d --build --no-deps backend     # tras tocar el back
docker compose restart backend                     # reiniciar sin recompilar
docker compose down                                # bajar (la base queda)
```

`--no-deps` evita recrear MySQL y esperar su healthcheck de nuevo.

**Abrir la base con Workbench o DBeaver.** MySQL no publica puerto. Creá
`cine-docker/docker-compose.override.yml` (no se versiona) y volvé a levantar:

```yaml
services:
  mysql:
    ports:
      - "127.0.0.1:3306:3306"
```

Conectás a `127.0.0.1:3306`, base `appsinteractivas`, usuario y clave del `.env`.

## Tests

385 pruebas contra H2 en memoria, sin Docker ni MySQL.

```bash
cd cine-backend && mvn clean test                                              # con Java 21 y Maven
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn -B clean test   # sin instalar nada
```

> **Hoy 92 dan rojo y no es culpa tuya.** Los tests tienen fechas escritas a mano
> (`LocalDateTime.of(2026, 8, 20, ...)`) que ya pasaron, así que las funciones que arman
> quedan en el pasado y el sistema las rechaza. Está anotado como ticket.

El `clean` importa: sin él Maven corre clases viejas de `target/`.
