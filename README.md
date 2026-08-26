# Cine UADE

Sistema de gestión de un cine. TPO de Aplicaciones Interactivas (UADE).

| Carpeta | Qué es |
|---|---|
| `cine-backend/` | Java 21 + Spring Boot 3.5 (Spring MVC + Spring Data JPA + MySQL) |
| `cine-frontend/` | HTML + JS + Tailwind, servido por nginx |
| `cine-docker/` | El `docker-compose.yml` que levanta todo |

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

<details>
<summary>El <code>TMDB_TOKEN</code> (opcional, se puede dejar para después)</summary>

Sirve para traer la cartelera real de Argentina desde
[themoviedb.org](https://www.themoviedb.org/settings/api). Es gratis y sale en dos minutos.
Pedí el **API Read Access Token**, el largo tipo JWT, no la API key corta.

Sin token el sistema levanta y funciona igual: lo único que no se puede hacer es importar
cartelera, y la pantalla del importador te avisa antes de dejarte apretar el botón.
</details>

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

## Documentación

| Dónde | Qué hay |
|---|---|
| **[`cine-backend/docs/manual/index.html`](cine-backend/docs/manual/index.html)** | **El manual.** Requerimientos, casos de uso, reglas de negocio, arquitectura y 17 diagramas. Empezar por acá |
| [`cine-frontend/API.md`](cine-frontend/API.md) | El contrato HTTP completo, endpoint por endpoint |
| [`cine-docker/README.md`](cine-docker/README.md) | Detalle de los contenedores, redes, puertos y la base |
| `CLAUDE.md` | Convenciones del repo: cómo se nombra, dónde va cada cosa, cómo se commitea |

El manual **es generado**: se arma desde `template.html` con `python3 build.py`. Nunca
editar `index.html` a mano.

```bash
cd cine-backend/docs/diagramas && plantuml -tsvg *.puml
cd ../manual && python3 build.py
```

## Antes de tocar código

1. Leé la sección **Arquitectura en capas** del manual: hay un test (`ArquitecturaTest`)
   que falla si una capa importa a otra que no tiene debajo.
2. Las reglas de negocio van **siempre** en `service/`, nunca en `controller/` ni en el front.
3. Si tocás la API, actualizá `cine-frontend/API.md` en el mismo commit.
4. Todo en español: clases, métodos, variables, comentarios y mensajes de error.
