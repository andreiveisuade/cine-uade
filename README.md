# Cine UADE

Sistema de gestión de un cine. TPO de Aplicaciones Interactivas (UADE).

| Carpeta | Qué es |
|---|---|
| `cine-backend/` | Java 21 + Spring Boot 3.5 (Spring MVC + Spring Data JPA + MySQL) |
| `cine-frontend/` | HTML + JS + Tailwind, servido por nginx |
| `cine-docker/` | El `docker-compose.yml` que levanta todo |

## Levantarlo

Hace falta Docker. Nada más: ni Java ni Maven ni MySQL instalados.

```bash
cd cine-docker
cp .env.example .env          # completar las tres claves (ver abajo)
docker compose up -d --build
./seed/datos-de-ejemplo.sh    # opcional: salas, películas y funciones de ejemplo
```

App en `localhost:8080`, panel del encargado en `/admin.html`, Adminer en `localhost:8081`.
Usuario de demo: `encargado@cine.uade.ar` / `cine2026`.

### El `.env`

No está en el repo a propósito: cada uno arma el suyo y no se comparte.

- `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD` — son de **tu** MySQL adentro de Docker. Poné las
  que quieras; no tienen que coincidir con las de nadie.
- `TMDB_TOKEN` — el *API Read Access Token* (el largo) de
  [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api). Es gratis y sale
  en dos minutos. Sin él todo funciona menos traer cartelera real desde el importador.

## Tests

```bash
cd cine-backend && mvn test
```

385 pruebas. No necesitan Docker ni MySQL: corren contra H2 en memoria.

## Manual

`cine-backend/docs/manual/index.html` — requerimientos, casos de uso, reglas de negocio y
diagramas.
