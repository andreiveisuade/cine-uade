# Cine UADE

Sistema de gestión de un cine. TPO de Aplicaciones Interactivas (UADE).

| Carpeta | Qué es |
|---|---|
| `cine-backend/` | Java 21 + Spring Boot 3.5 (Spring MVC + Spring Data JPA + MySQL) |
| `cine-frontend/` | HTML + JS + Tailwind, servido por nginx |
| `cine-docker/` | El `docker-compose.yml` que levanta todo |

## Levantarlo

Hace falta **Docker y nada más**: ni Java, ni Maven, ni MySQL instalados.

```bash
cd cine-docker
cp .env.example .env          # completar las 3 claves (ver abajo)
docker compose up -d --build
./seed/datos-de-ejemplo.sh    # opcional: salas, películas y funciones de ejemplo
```

| Qué | Dónde | Credenciales |
|---|---|---|
| Web del cliente | http://localhost:8080 | no hace falta iniciar sesión |
| Panel del encargado | http://localhost:8080/admin.html | `encargado@cine.uade.ar` / `cine2026` |
| Puerta (acomodador) | el mismo panel | `puerta@cine.uade.ar` / `cine2026` |
| Adminer (ver la base) | http://localhost:8081 | servidor `mysql`, usuario del `.env` |

### El `.env`

No está en el repo a propósito: cada uno arma el suyo y no se comparte.

- `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD` — son de **tu** MySQL adentro de Docker. Poné las
  que quieras; no tienen que coincidir con las de nadie.
- `TMDB_TOKEN` — el *API Read Access Token* (el largo) de
  [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api). Es gratis y sale
  en dos minutos. Sin él todo funciona menos traer cartelera real desde el importador.

### Comandos del día a día

```bash
docker compose ps                                  # los 5 arriba; mysql y backend (healthy)
docker compose logs -f backend                     # qué está haciendo la app
docker compose up -d --build --no-deps frontend    # tras tocar el front, sin recrear MySQL
docker compose down -v                             # borra la base y empieza de cero
```

## Tests

```bash
cd cine-backend && mvn clean test
```

362 pruebas. No necesitan Docker ni MySQL: corren contra H2 en memoria. El `clean` importa:
sin él Maven corre clases de test viejas que quedaron en `target/`.

## Documentación

| Dónde | Qué hay |
|---|---|
| **[`cine-backend/docs/manual/index.html`](cine-backend/docs/manual/index.html)** | **El manual.** Requerimientos, casos de uso, reglas de negocio, arquitectura y 16 diagramas. Empezar por acá |
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
