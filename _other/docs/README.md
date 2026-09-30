# docs

| Carpeta | Qué hay |
|---|---|
| `manual/` | El manual (`index.html`): requerimientos, reglas, casos de uso, arquitectura, decisiones y diagramas. Es la fuente de verdad |
| `diagramas/` | Fuentes PlantUML (`.puml`) y sus `.svg` |
| `evidencia/` | Capturas de la última recorrida visual |
| `PRUEBAS.md` | Cómo se verifica el sistema y el resultado de la última corrida |

El manual es autocontenido (`open manual/index.html`). **No se edita a mano**: se edita
`manual/template.html` y se regenera, con PlantUML y Python 3:

```bash
cd diagramas && plantuml -tsvg *.puml     # tras tocar un .puml
cd ../manual && python3 build.py          # inyecta los SVG y las capturas en el HTML
```

Un diagrama nuevo se registra en `svg_map` de `build.py`; una captura, en `imagen_map`.

## Diagramas

| Archivo | Qué muestra |
|---|---|
| `clases-dominio.puml` | Entidades, value objects, enums y relaciones del negocio, agrupados por subdominio |
| `clases-capas.puml` | Arquitectura en capas: controllers, DTO, gestores, repositorios y adaptadores de infraestructura |
| `casos-de-uso.puml` | Actores y casos de uso |
| `secuencia-reserva.puml` | `POST /api/reservas`: de reservar butacas a emitir el ticket |
| `secuencia-candy.puml` | Comprar en el candy: del combo al ticket |
| `docker-despliegue.puml` | Los 4 contenedores de `cine-docker`, las redes `web`/`datos`, volúmenes y qué carpeta construye a cada uno |
| `capas.puml` | Capas y paquetes de Spring Boot + Spring Data JPA |
| `peticion-capas.puml` | Una petición de punta a punta: contenedores, puertos y capas |
| `arranque-orden.puml` | Orden de arranque de `docker compose up -d --build` |
| `butaca-carrera.puml` | Dos personas, la misma butaca: el bloqueo en MySQL y el UNIQUE de `entrada` |
| `importador-flujo.puml` | Importador de cartelera: TMDB entra por las reglas del cine |
| `promociones-herencia.puml` | Promociones: una jerarquía, no un switch |
| `usuarios-herencia.puml` | Usuarios: herencia, no un enum suelto |
| `patron-state.puml` | State: el estado decide qué se puede hacer, no un if en su dueño |
| `patron-observer.puml` | Observer: la venta avisa y los comprobantes salen después del commit |
| `solid-ocp.puml` | SOLID, abierto-cerrado: un beneficio nuevo es una clase |
| `solid-isp.puml` | SOLID, segregación de interfaces: cobrar no necesita el ABM de promociones |
| `solid-dip.puml` | SOLID, inversión de dependencias |
| `grasp-pure-fabrication.puml` | GRASP, fabricación pura |
| `terminal-desktop.puml` | Terminal de boletería (pendiente) y servidor: dos instalaciones, una sincronización |
