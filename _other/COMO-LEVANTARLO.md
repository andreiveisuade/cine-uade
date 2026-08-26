# Cómo levantar el proyecto

Guía paso a paso para tener el sistema andando en tu máquina. Si algo no sale, la tabla
de [Si algo falla](#si-algo-falla) cubre los errores más comunes.

---

## El camino corto

Un script hace todo lo de abajo por vos: chequea Docker, arma el `.env` con contraseñas
nuevas, te pide el token de TMDB, levanta, espera a que esté sano y siembra los datos.

```bash
cd cine-docker

./setup.sh          # macOS y Linux
.\setup.ps1         # Windows (PowerShell)
```

Se puede correr las veces que quieras: si el `.env` ya existe no lo pisa, y si ya hay
datos no vuelve a sembrar.

> En Windows, si PowerShell lo bloquea:
> `powershell -ExecutionPolicy Bypass -File .\setup.ps1`

Si preferís entender qué pasa en cada paso, o el script falló en alguno, seguí leyendo.

---

## Levantarlo paso a paso

### Antes de empezar

Para **correr** el sistema alcanza con Docker. Ni Java, ni Maven, ni MySQL instalados.

| Necesitás | Para qué | Verificalo con |
|---|---|---|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) **abierto y corriendo** | Levantar todo | `docker info` |
| git | Clonar el repo | `git --version` |

Si `docker info` tira `Cannot connect to the Docker daemon`, Docker Desktop está cerrado.
Abrilo y esperá a que el ícono deje de moverse.

> Para **desarrollar** (correr los tests) hace falta además Java 21 y Maven, o el atajo
> con Docker que está en [Trabajar cómodo en local](#trabajar-cómodo-en-local).

### 1. Clonar

```bash
git clone https://github.com/andreiveisuade/cine-uade.git
cd cine-uade/cine-docker
```

Todos los comandos que siguen se corren **parado en `cine-docker/`**.

### 2. Armar el `.env`

```bash
cp .env.example .env
```

Abrí el `.env` y cambiá las dos contraseñas. **No tienen que coincidir con las de nadie**:
son de tu MySQL, adentro de tu Docker.

| Variable | Qué poner |
|---|---|
| `MYSQL_ROOT_PASSWORD` | Cualquier cosa. Solo la usa la imagen de MySQL para inicializarse |
| `DB_PASSWORD` | Cualquier cosa. Es la que usa la app para conectarse |
| `TMDB_TOKEN` | Opcional. Dejalo vacío por ahora (ver más abajo) |

El `.env` **no se versiona**: cada uno arma el suyo. Si te lo olvidás, el compose no
arranca y te dice cuál falta.

### El token de TMDB

El sistema importa la cartelera real de Argentina desde
[TMDB](https://www.themoviedb.org). Para eso hace falta un token.

**Andrei se lo pasa a cada uno por privado.** No está en el repo ni va a estarlo: un token
en un repo público lo levanta cualquiera, y TMDB lo revoca.

Cuando lo tengas, abrí `cine-docker/.env` y pegalo **después del `=`, sin espacios, sin
comillas y en una sola línea**:

```diff
- TMDB_TOKEN=
+ TMDB_TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4ZTM...
```

Es largo —arriba de 200 caracteres— así que asegurate de copiarlo entero. Empieza con
`eyJ`. Después:

```bash
docker compose up -d backend
```

Y listo: el importador queda habilitado en el panel.

| Se ve así | Qué pasó |
|---|---|
| El botón del importador está deshabilitado | El `.env` no tiene token, o quedó vacío |
| Trae 0 películas y no da error | Se cortó al copiar. Fijate que termine igual que el original |
| Dice que no está autorizado | Está mal pegado, o TMDB lo revocó. Pedile uno nuevo a Andrei |

**Sin token el sistema levanta y anda igual.** Lo único que no vas a poder hacer es
importar cartelera desde TMDB; la pantalla te avisa antes de dejarte apretar el botón.
Todo el resto —cargar películas a mano, funciones, reservas, cobro, candy, arqueo—
funciona sin tocar nada de esto. Se puede dejar para después.

<details>
<summary>Si preferís sacar el tuyo propio</summary>

Es gratis y sale en dos minutos:

1. Creá una cuenta en [themoviedb.org](https://www.themoviedb.org/signup).
2. Andá a [Configuración → API](https://www.themoviedb.org/settings/api).
3. Pedí una API key: tipo de uso **Developer**, y en el formulario podés poner que es un
   trabajo práctico de facultad sin fines comerciales.
4. Copiá el **API Read Access Token** — el largo tipo JWT, **no** la API key corta de 32
   caracteres hexadecimales.

</details>

> **El `.env` nunca se sube.** Está en el `.gitignore` justamente para eso. No pegues el
> token en un issue, en el código ni en un mensaje de grupo.

### 3. Levantar

```bash
docker compose up -d --build
```

**La primera vez tarda varios minutos** y es normal: Maven baja todas las dependencias del
backend para compilar el jar. Las siguientes veces son segundos, porque Docker reusa esa capa.

### 4. Esperar a que estén sanos

Este es el paso que más confunde. Los servicios **no arrancan todos juntos**: cada uno
espera al anterior.

```
mysql  ──(healthy)──▶  backend  ──(healthy)──▶  frontend
```

Hasta que el backend no esté sano, **`localhost:8080` no responde nada**. No está roto:
todavía no le toca. Mirá el progreso con:

```bash
docker compose ps
```

Repetilo hasta ver los 5 servicios `Up`, con `mysql` y `backend` en `(healthy)`. Toma
alrededor de un minuto la primera vez.

```bash
docker compose logs -f backend    # si querés ver qué está haciendo mientras tanto
```

### 5. Cargar datos de ejemplo

Con la base recién creada no hay nada que mirar: ni películas, ni salas, ni funciones.

```bash
./seed/datos-de-ejemplo.sh
```

Carga 4 películas, las 6 salas con sus 450 butacas y 8 funciones entre hoy y mañana.
El script entra **por la API y no por SQL**, así que los datos pasan por las mismas reglas
que aplica el sistema: no puede sembrar nada que la aplicación después rechazaría.

### 6. Entrar

| Qué | Dónde | Credenciales |
|---|---|---|
| Web del cliente | <http://localhost:8080> | no hace falta iniciar sesión |
| Panel del encargado | <http://localhost:8080/admin.html> | `encargado@cine.uade.ar` / `cine2026` |
| Puerta (acomodador) | el mismo panel | `puerta@cine.uade.ar` / `cine2026` |
| Adminer (ver la base) | <http://localhost:8081> | servidor `mysql`, usuario y clave del `.env` |

En Adminer el servidor es **`mysql`**, no `localhost`: Adminer corre adentro de Docker y
llega a la base por el nombre del servicio.

---

## Si algo falla

| Lo que ves | Qué pasa | Cómo se arregla |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop está cerrado | Abrilo y esperá a que termine de arrancar |
| `falta DB_USER, copiá .env.example a .env` | No hay `.env` | `cp .env.example .env` y completalo |
| `port is already allocated` | Algo más usa el 8080 | Cambiá `PUERTO_WEB` en el `.env` y volvé a levantar |
| `localhost:8080` no responde | El backend todavía no está sano | `docker compose ps` y esperá a que diga `(healthy)` |
| `backend` reinicia en loop | No puede con la base | `docker compose logs backend`. Casi siempre es el `.env` mal |
| La web carga pero está vacía | Falta sembrar | `./seed/datos-de-ejemplo.sh` |
| Cambiaste el `.env` y sigue igual | El volumen de MySQL ya existía con la clave vieja | `docker compose down -v && docker compose up -d --build` (borra la base) |

Cuando nada de eso alcanza, empezar de cero siempre funciona:

```bash
docker compose down -v      # baja todo y borra la base
docker compose up -d --build
./seed/datos-de-ejemplo.sh
```

---

## Trabajar cómodo en local

### El loop del día a día

```bash
docker compose ps                                  # los 5 arriba; mysql y backend (healthy)
docker compose logs -f backend                     # qué está haciendo la app
docker compose up -d --build --no-deps frontend    # tras tocar el front, sin recrear MySQL
docker compose up -d --build --no-deps backend     # tras tocar el back
docker compose restart backend                     # reiniciar sin recompilar
docker compose down                                # bajar todo (la base se conserva)
docker compose down -v                             # bajar y borrar la base
```

El `--no-deps` importa: sin él Docker recrea MySQL también y perdés tiempo esperando el
healthcheck de nuevo.

### Abrir la base con Workbench o DBeaver

Por defecto MySQL **no publica ningún puerto**: solo se lo alcanza desde adentro de Docker.
Para conectarle un cliente de escritorio, creá `cine-docker/docker-compose.override.yml`:

```yaml
services:
  mysql:
    ports:
      - "127.0.0.1:3306:3306"
```

Y levantá de nuevo. Docker Compose lo toma solo, sin flags.

Ese archivo **no se versiona** a propósito: es un ajuste de tu máquina, no parte del
sistema. Conectás a `127.0.0.1:3306`, base `appsinteractivas`, con el usuario y la clave
del `.env`.

### Correr los tests

385 pruebas. No necesitan Docker ni MySQL: corren contra H2 en memoria.

> **Hoy la suite da rojo y no es culpa tuya.** 92 tests fallan porque tienen fechas
> hardcodeadas (`LocalDateTime.of(2026, 8, 20, ...)`) que ya pasaron, así que las funciones
> que arman quedan en el pasado y el sistema las rechaza con "La función ya empezó".
> Está anotado como ticket. Si ves ese error, es esto y no algo que hayas roto.

```bash
cd cine-backend && mvn clean test
```

Esto **sí** necesita Java 21 y Maven instalados. Si no los tenés y no querés instalarlos,
corrélos adentro de un contenedor:

```bash
cd cine-backend
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn -B clean test
```

El `clean` importa en los dos casos: sin él Maven corre clases de test viejas que quedaron
en `target/` y te infla el conteo.

---
