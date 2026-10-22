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
archify/exportar.sh capas.archify.json    # tras tocar un .archify.json (skill archify + navegador de cmux)
cd ../manual && python3 build.py          # inyecta los SVG y las capturas en el HTML
```

DER desde `schema.sql`, desde la raíz del repo (Python 3 solo; el export pide la skill diagram-design en `~/.claude/skills`):
`python3 _other/docs/diagramas/der/generar_der.py && python3 ~/.claude/skills/diagram-design/scripts/export_svg.py _other/docs/diagramas/der-design.html && python3 _other/docs/diagramas/der/verificar_der.py`

Un diagrama nuevo se registra en `svg_map` de `build.py`; una captura, en `imagen_map`.

## Diagramas

| Archivo | Qué muestra |
|---|---|
| `clases-catalogo.puml` | Dominio 1/3: cartelera, salas, funciones y grilla |
| `clases-ventas.puml` | Dominio 2/3: usuarios, ventas y promociones, con sus Value Objects |
| `clases-candy-valores.puml` | Dominio 3/3: candy, `Dinero`, `Periodo` y `FranjaHoraria` |
| `capas.archify.json` | Las seis capas y quién llama a quién (Archify) |
| `casos-de-uso.puml` | Actores y casos de uso |
| `secuencia-reserva.puml` | `POST /api/reservas`: de reservar butacas a emitir el ticket |
| `secuencia-candy.puml` | Comprar en el candy: del combo al ticket |
| `sistema-contexto.html` | C4 nivel 1 (diagram-design, exportado a `.svg`): cliente, encargado y acomodador, el sistema, Mercado Pago y TMDB |
| `docker-despliegue.puml` | Los 4 contenedores de `cine-docker`, las redes `web`/`datos`, volúmenes y qué carpeta construye a cada uno |
| `reserva-clases.archify.json` | Las mismas capas con las clases de `POST /api/reservas` (Archify) |
| `peticion-capas.puml` | Una petición de punta a punta: contenedores, puertos y capas |
| `arranque-orden.puml` | Orden de arranque de `docker compose up -d --build` |
| `butaca-carrera.puml` | Dos personas, la misma butaca: el bloqueo en MySQL y el UNIQUE de `entrada` |
| `importador-flujo.puml` | Importador de cartelera: TMDB entra por las reglas del cine |
| `promociones-herencia.puml` | Promociones: una jerarquía, no un switch |
| `usuarios-herencia.puml` | Usuarios: herencia, no un enum suelto |
| `patron-*.puml` | Un diagrama por patrón GoF, principio SOLID y patrón GRASP aplicado, con `_estilo-patrones.iuml` (el estilo de los apuntes) |
| `terminal-desktop.puml` | Terminal de boletería (pendiente) y servidor: dos instalaciones, una sincronización |
| `reglas-donde-viven.puml` | En qué clase se decide cada regla R1..R21 |
| `validacion-capas.puml` | Qué valida cada capa, del cliente a la base |
| `rechazos-jerarquia.puml` | `Rechazo` sellada: una clase por status |
| `api-errores.puml` | Quién contesta cada error: nginx, Spring Security, controller o `ManejadorErrores` |
| `der.puml` | DER completo: las 20 tablas de `schema.sql` por subdominio, con las restricciones que sostienen reglas |
| `base-venta-er.puml` | Zoom del DER en la venta: PK, FK, `UNIQUE` y cascadas |
| `precio-entrada.puml` | El precio de una entrada y el descuento al cobrar |
| `checkout-pasarela.puml` | Checkout: abrir y confirmar contra la pasarela |
| `pago-mercadopago.puml` | Pago online con Mercado Pago: el retorno consulta el pago |
| `conciliacion-pagos.puml` | Conciliación: la tarea cobra lo que Mercado Pago aprobó aunque el comprador no vuelva |
| `grilla-plan.puml` | La grilla: un plan para previsualizar, crear y extender |
| `front-estructura.puml` | Front: carpetas por funcionalidad, dependencias hacia abajo |
| `front-flujo.puml` | Front: de la pantalla al backend y de vuelta |
| `swing-pedido.puml` | Swing: un formulario, del clic al mensaje |
| `swing-pantallas.puml` | Swing: herencia de las pantallas (Template Method) |
| `traslado-servidor.puml` | Qué dice dónde está cada parte al mudar el servidor |
| `pruebas-niveles.puml` | Qué levanta cada nivel de prueba del backend |
