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

`index.html` **no se edita a mano**: se genera desde `manual/template.html`, que tiene los placeholders
`{{SVG_CASOS_USO}}`, `{{SVG_DOMINIO}}`, `{{SVG_CAPAS}}`, `{{SVG_SECUENCIA}}`, `{{SVG_SECUENCIA_CANDY}}` y
`{{SVG_DOCKER}}`. Para cambiar texto o diseño del manual, editar el template y volver a correr `build.py`.
Si agregás un diagrama nuevo, sumá su placeholder al `svg_map` de `build.py`.

## Diagramas

| Archivo | Qué muestra |
|---|---|
| `clases-dominio.puml` | Entidades, enums y relaciones del negocio, agrupadas por sub-dominio |
| `clases-capas.puml` | Arquitectura en capas: gestores, repositorios y adaptadores de infraestructura |
| `casos-de-uso.puml` | Actores y casos de uso |
| `secuencia-reserva.puml` | Flujo de reservar butacas hasta emitir el ticket |
| `secuencia-candy.puml` | Armado del combo promocional y venta en el candy |
| `docker-despliegue.puml` | Los 4 contenedores de `cine-docker`, las redes `web`/`datos`, volúmenes y qué repo construye a cada uno |
