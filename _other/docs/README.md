# docs

| Carpeta | Qué hay |
|---|---|
| `manual/` | Manual navegable del proyecto (`index.html`). Requerimientos, casos de uso, diagramas, decisiones y pendientes. Es la fuente de verdad. |
| `diagramas/` | Fuentes PlantUML (`.puml`) y sus `.svg` renderizados |

## Ver el manual

```bash
open manual/index.html
```

Es autocontenido: los SVG van embebidos, no necesita servidor ni internet.

## Regenerar

```bash
cd diagramas && plantuml -tsvg *.puml     # tras tocar un .puml
cd ../manual && python3 build.py               # inyecta los SVG en el HTML
```

`index.html` **no se edita a mano**: se edita `manual/template.html` y se corre `build.py`, que
reemplaza sus placeholders `{{SVG_*}}`. Un diagrama nuevo suma su placeholder al `svg_map` de `build.py`.

## Diagramas

| Archivo | Qué muestra |
|---|---|
| `clases-dominio.puml` | Entidades, enums y relaciones del negocio, agrupadas por sub-dominio |
| `clases-capas.puml` | Arquitectura en capas: gestores, repositorios y adaptadores de infraestructura |
| `casos-de-uso.puml` | Actores y casos de uso |
| `secuencia-reserva.puml` | Flujo de reservar butacas hasta emitir el ticket |
| `secuencia-candy.puml` | Armado del combo promocional y venta en el candy |
| `docker-despliegue.puml` | Los 4 contenedores de `cine-docker`, las redes `web`/`datos`, volúmenes y qué repo construye a cada uno |
| `capas.puml` | Capas y paquetes de Spring Boot + Spring Data JPA |
| `peticion-capas.puml` | Una petición de punta a punta: contenedores, puertos y capas |
| `arranque-orden.puml` | Orden de arranque de `docker compose up -d --build` |
| `butaca-carrera.puml` | Dos personas, la misma butaca: el bloqueo en MySQL y el UNIQUE de `entrada` |
| `importador-flujo.puml` | Importador de cartelera: TMDB entra por las reglas del cine |
| `promociones-herencia.puml` | Promociones: una jerarquía, no un switch |
| `usuarios-herencia.puml` | Usuarios: herencia, no un enum suelto |
| `solid-ocp.puml` | SOLID, abierto-cerrado: un beneficio nuevo es una clase |
| `solid-isp.puml` | SOLID, segregación de interfaces |
| `solid-dip.puml` | SOLID, inversión de dependencias |
| `grasp-pure-fabrication.puml` | GRASP, fabricación pura |
| `terminal-desktop.puml` | Terminal de boletería (pendiente) y servidor: dos instalaciones, una sincronización |
