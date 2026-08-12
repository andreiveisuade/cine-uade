# docs

| Carpeta | Qué hay |
|---|---|
| `manual/` | Manual navegable del proyecto (`index.html`). Requerimientos, casos de uso, diagramas, decisiones y pendientes. Es la fuente de verdad. |
| `diagramas/` | Fuentes PlantUML (`.puml`) y sus `.svg` renderizados |

## Ver el manual

```bash
open docs/manual/index.html
```

Para ver la base de datos: `docker compose up -d` y abrir <http://localhost:8080> (Adminer).
Servidor `mysql`, usuario `root`, password `root`, base `appsinteractivas`.

Es autocontenido: los SVG van embebidos, no necesita servidor ni internet.

## Regenerar

```bash
cd docs/diagramas && plantuml -tsvg *.puml     # tras tocar un .puml
cd ../manual && python3 build.py               # inyecta los SVG en el HTML
```

`index.html` **no se edita a mano**: se genera desde `manual/template.html`, que tiene los placeholders
`{{SVG_CASOS_USO}}`, `{{SVG_DOMINIO}}` y `{{SVG_CAPAS}}`. Para cambiar texto o diseño del manual, editar
el template y volver a correr `build.py`.

## Diagramas

| Archivo | Qué muestra |
|---|---|
| `clases-dominio.puml` | Entidades, enums y relaciones del negocio |
| `clases-capas.puml` | Arquitectura DAO: de la UI a la base, con las 5 entidades |
| `casos-de-uso.puml` | Actores y casos de uso |
| `secuencia-reserva.puml` | Flujo de reservar entradas hasta emitir el ticket |
