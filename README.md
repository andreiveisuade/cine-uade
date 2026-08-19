# Cine UADE

Sistema de gestión de un cine. TPO de Aplicaciones Interactivas (UADE).

| Carpeta | Qué es |
|---|---|
| `cine-backend/` | Java 21 + Spring Boot 3.5 (Spring MVC + Spring Data JPA + MySQL) |
| `cine-frontend/` | HTML + JS + Tailwind, servido por nginx |
| `cine-docker/` | El `docker-compose.yml` que levanta todo |

## Levantarlo

```bash
cd cine-docker
cp .env.example .env
docker compose up -d --build
```

App en `localhost:8080`, panel del encargado en `/admin.html`.

## Tests

```bash
cd cine-backend && mvn test
```

## Manual

`cine-backend/docs/manual/index.html` — requerimientos, casos de uso, reglas de negocio y
diagramas.
