# Cómo levantarlo

Requiere **Docker Desktop abierto**. El panel de escritorio, además, JDK 21 (Maven no: el proyecto trae `mvnw`).

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
./seed/datos-de-ejemplo.sh           # 6 salas, candy, 6 películas con una semana de funciones y la promo «Miércoles 2x1»
git config core.hooksPath .githooks  # hook que frena commits con credenciales
```

La promo del seed vence a los 90 días. Correr el seed dos veces da `409` inofensivos. Con otro puerto:
`API=http://localhost:9090/api ./seed/datos-de-ejemplo.sh`.

## Usarlo

```sh
cd cine-swing && ./mvnw compile exec:java      # el panel, en otra terminal
cd cine-swing; .\mvnw.cmd compile exec:java    # Windows (PowerShell)
```

En IntelliJ: la configuración **Panel Swing** ya viene en el proyecto.

| | Dónde | Credenciales |
|---|---|---|
| Web del cliente | <http://localhost:8080> | — |
| Panel del encargado | `cine-swing` | `encargado@cine.uade.ar` / `cine2026` |
| Puerta | el mismo panel | `puerta@cine.uade.ar` / `cine2026` |
| Swagger | <http://localhost:8080/swagger-ui.html> | las rutas del encargado piden el mismo usuario |
| Adminer | <http://localhost:8081> | servidor `mysql`, `DB_USER` / `DB_PASSWORD` del `.env` (`setup` los imprime al final) |

El seed trae seis clásicos para no arrancar vacío (si la base ya tiene películas, no los agrega). La cartelera
real: **Importador** del panel → confirmarlas en **Por revisar** → funciones desde **Funciones**, **Grilla** o **Planificador**.

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

## Mercado Pago (opcional)

Sin `MP_ACCESS_TOKEN` el pago online usa la pasarela emulada y todo anda igual. Para probar con Mercado Pago, en
sandbox y sin cobrar:

1. [Panel de desarrolladores](https://www.mercadopago.com.ar/developers/panel): crear una aplicación de Checkout Pro y copiar el Access Token de **prueba**.
2. Pegarlo en `cine-docker/.env` (`MP_ACCESS_TOKEN=TEST-...`, sin comillas) y `docker compose up -d backend`. `CINE_URL_PUBLICA` queda en `http://localhost:8080`.
3. Crear en el mismo panel dos cuentas de prueba (vendedor y **comprador**) y entrar a Mercado Pago con la del comprador. Es obligatorio: como invitado, MP rechaza el pago («Una de las partes… es de prueba»). En DEBITO/CREDITO MP puede ofrecer también «dinero en cuenta» (no se puede excluir).
4. En la web: ingresar con Google (con `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` en el `.env`), reservar, Mis reservas, **Pagar online**, **Ir a pagar**. Tarjeta de prueba con titular `APRO` (aprobado).
5. En localhost MP no vuelve solo, y no hace falta: la conciliación del backend (`cine.mercadopago.conciliacion`, cada 60 s) cobra el pago aprobado en un minuto como máximo y el ticket pasa a Pagada. Tocar **Volver al sitio** (si aparece) lo cobra al instante. Con `CINE_URL_PUBLICA` público (https), vuelve solo.

| Mensaje en el ticket | Causa |
|---|---|
| Pago rechazado / pendiente / sin pago | MP no tiene un pago aprobado para esa reserva; se puede volver a pagar |

Sin Docker: [`cine-backend`](../cine-backend/README.md#correrlo).

## Si algo falla

| Lo que ves | Qué hacer |
|---|---|
| `Cannot connect to the Docker daemon` | Abrir Docker Desktop |
| `falta DB_USER, copiá .env.example a .env` | `cp .env.example .env` |
| `port is already allocated` | Cambiar `PUERTO_WEB` o `PUERTO_ADMINER` en el `.env` |
| `localhost:8080` no responde, o «El servidor no está disponible» | El backend está arrancando o se cayó: `docker compose ps` y `docker compose logs backend` |
| La web carga vacía | No corrió el seed, o las funciones de la semana sembrada ya pasaron: grilla nueva desde el panel |
| Adminer no conecta | Servidor **`mysql`**, no `localhost`. Usuario y base: `DB_USER` y `DB_NAME`; la clave es `DB_PASSWORD` del `cine-docker/.env` (local, la generó el setup). El setup imprime la URL lista |
| `mvn` no se reconoce | No hace falta Maven: usar `./mvnw` (`.\mvnw.cmd` en Windows) dentro de `cine-swing` |
| `ClassNotFoundException` al correr el panel | `exec:java` no compila: usar `compile exec:java` |
| `JAVA_HOME` no definido, Java viejo o el setup avisa que falta Java 21 | Instalar JDK 21: `winget install EclipseAdoptium.Temurin.21.JDK` o `brew install openjdk@21` |
| Cambiaste el `.env` y sigue igual | La base se creó con la clave vieja: `docker compose down -v`, **que borra los datos** |
| El backend no levanta tras un `git pull` | Faltan migraciones: ver [La base](../cine-docker/README.md#la-base) |

## Día a día (en `cine-docker/`)

```bash
docker compose logs -f backend
docker compose up -d --build --no-deps backend     # tras tocar el back (o frontend, tras tocar el front)
docker compose down                                # bajar; la base queda
docker compose down -v                             # bajar y borrar la base
```

Tests: [`PRUEBAS.md`](docs/PRUEBAS.md) y el README de cada módulo.
