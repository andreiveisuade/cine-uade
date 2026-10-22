import base64
import re
from pathlib import Path

MANUAL = Path(__file__).resolve().parent
DIAGRAMAS = MANUAL.parent / "diagramas"
CAPTURAS = MANUAL / "capturas"

svg_map = {
    "{{SVG_CASOS_USO}}": DIAGRAMAS / "casos-de-uso.svg",
    "{{SVG_DOMINIO_CATALOGO}}": DIAGRAMAS / "clases-catalogo.svg",
    "{{SVG_DOMINIO_VENTAS}}": DIAGRAMAS / "clases-ventas.svg",
    "{{SVG_DOMINIO_CANDY}}": DIAGRAMAS / "clases-candy-valores.svg",
    "{{SVG_DER}}": DIAGRAMAS / "der.svg",
    # Hecho con diagram-design, como sistema-contexto.
    "{{SVG_DER_DESIGN}}": DIAGRAMAS / "der-design.svg",
    # Hecho con diagram-design: el .html es la fuente y el .svg sale de su export_svg.py.
    "{{SVG_CONTEXTO}}": DIAGRAMAS / "sistema-contexto.svg",
    "{{SVG_RESERVA_CLASES}}": DIAGRAMAS / "reserva-clases.svg",
    "{{SVG_SECUENCIA}}": DIAGRAMAS / "secuencia-reserva.svg",
    "{{SVG_SECUENCIA_CANDY}}": DIAGRAMAS / "secuencia-candy.svg",
    "{{SVG_DOCKER}}": DIAGRAMAS / "docker-despliegue.svg",
    "{{SVG_DESPLIEGUE_RENDER}}": DIAGRAMAS / "despliegue-render.svg",
    "{{SVG_ARRANQUE}}": DIAGRAMAS / "arranque-orden.svg",
    "{{SVG_TERMINAL}}": DIAGRAMAS / "terminal-desktop.svg",
    "{{SVG_IMPORTADOR}}": DIAGRAMAS / "importador-flujo.svg",
    "{{SVG_PETICION}}": DIAGRAMAS / "peticion-capas.svg",
    "{{SVG_USUARIOS}}": DIAGRAMAS / "usuarios-herencia.svg",
    "{{SVG_PROMOCIONES}}": DIAGRAMAS / "promociones-herencia.svg",
    "{{SVG_BUTACA_CARRERA}}": DIAGRAMAS / "butaca-carrera.svg",
    # Capas del sistema; los principios y patrones van en patron-*.svg, mas abajo.
    "{{SVG_CAPAS_PAQUETES}}": DIAGRAMAS / "capas.svg",
    "{{SVG_API_ERRORES}}": DIAGRAMAS / "api-errores.svg",
    "{{SVG_BASE_VENTA_ER}}": DIAGRAMAS / "base-venta-er.svg",
    "{{SVG_CHECKOUT_PASARELA}}": DIAGRAMAS / "checkout-pasarela.svg",
    "{{SVG_PAGO_MERCADOPAGO}}": DIAGRAMAS / "pago-mercadopago.svg",
    "{{SVG_CONCILIACION_PAGOS}}": DIAGRAMAS / "conciliacion-pagos.svg",
    "{{SVG_GRILLA_PLAN}}": DIAGRAMAS / "grilla-plan.svg",
    "{{SVG_PRECIO_ENTRADA}}": DIAGRAMAS / "precio-entrada.svg",
    "{{SVG_PRUEBAS_NIVELES}}": DIAGRAMAS / "pruebas-niveles.svg",
    "{{SVG_RECHAZOS_JERARQUIA}}": DIAGRAMAS / "rechazos-jerarquia.svg",
    "{{SVG_REGLAS_DONDE_VIVEN}}": DIAGRAMAS / "reglas-donde-viven.svg",
    "{{SVG_FRONT_ESTRUCTURA}}": DIAGRAMAS / "front-estructura.svg",
    "{{SVG_FRONT_FLUJO}}": DIAGRAMAS / "front-flujo.svg",
    "{{SVG_SWING_PANTALLAS}}": DIAGRAMAS / "swing-pantallas.svg",
    "{{SVG_SWING_PEDIDO}}": DIAGRAMAS / "swing-pedido.svg",
    "{{SVG_TRASLADO_SERVIDOR}}": DIAGRAMAS / "traslado-servidor.svg",
    "{{SVG_VALIDACION_CAPAS}}": DIAGRAMAS / "validacion-capas.svg",
}

# Un diagrama por patron o principio, en el estilo de los apuntes de la catedra
# (_estilo-patrones.iuml): patron-<nombre>.svg es el placeholder {{SVG_PATRON_<NOMBRE>}}.
for svg in sorted(DIAGRAMAS.glob("patron-*.svg")):
    svg_map["{{SVG_PATRON_" + svg.stem[len("patron-"):].upper().replace("-", "_") + "}}"] = svg


# Capturas de la corrida de pruebas. Van embebidas como data URI por el mismo motivo
# que los SVG: el manual tiene que abrirse de un doble clic, sin servidor. En JPEG y no
# PNG porque son capturas de pantalla completas y pesan la cuarta parte.
imagen_map = {
    "{{IMG_MAPA}}": CAPTURAS / "cp10-mapa.jpg",
    "{{IMG_TICKET}}": CAPTURAS / "cp10-ticket.jpg",
    "{{IMG_BLOQUEO}}": CAPTURAS / "cp18-bloqueo.jpg",
    # El panel del encargado en Swing, en el tema oscuro por defecto.
    "{{IMG_SWING_PELICULAS}}": CAPTURAS / "swing-peliculas.jpg",
    "{{IMG_SWING_IMPORTADOR}}": CAPTURAS / "swing-importador.jpg",
    "{{IMG_SWING_BUTACAS}}": CAPTURAS / "swing-butacas.jpg",
    "{{IMG_SWING_GRILLA}}": CAPTURAS / "swing-grilla.jpg",
    "{{IMG_SWING_PLANIFICADOR}}": CAPTURAS / "swing-planificador.jpg",
    "{{IMG_SWING_AGENDA}}": CAPTURAS / "swing-agenda.jpg",
    "{{IMG_SWING_COBRO}}": CAPTURAS / "swing-cobro.jpg",
    "{{IMG_SWING_BORDERO}}": CAPTURAS / "swing-bordero.jpg",
    "{{IMG_SWING_CAJA}}": CAPTURAS / "swing-caja.jpg",
    "{{IMG_SWING_PUERTA}}": CAPTURAS / "swing-puerta.jpg",
}


def como_data_uri(path):
    datos = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:image/jpeg;base64,{datos}"


def clean_svg(path):
    """Saca el width/height fijo del <svg> raiz para que escale con el CSS."""
    text = path.read_text(encoding="utf-8")

    def fix_root(m):
        tag = m.group(0)
        tag = re.sub(r'\swidth="\d+(?:px)?"', "", tag)
        tag = re.sub(r'\sheight="\d+(?:px)?"', "", tag)
        tag = re.sub(r'(style="[^"]*?)width:\d+px;height:\d+px;', r"\1", tag)
        return tag

    return re.sub(r"<svg\b[^>]*>", fix_root, text, count=1)


shell = (MANUAL / "template.html").read_text(encoding="utf-8")

faltantes = [str(p) for p in svg_map.values() if not p.exists()]
if faltantes:
    raise SystemExit("Faltan SVGs (plantuml -tsvg *.puml, o archify/exportar.sh para los .archify.json): " + ", ".join(faltantes))

for placeholder, svg_path in svg_map.items():
    if placeholder not in shell:
        raise SystemExit(f"Placeholder no encontrado en template.html: {placeholder}")
    shell = shell.replace(placeholder, clean_svg(svg_path))

faltantes = [str(p) for p in imagen_map.values() if not p.exists()]
if faltantes:
    raise SystemExit("Faltan capturas en manual/capturas/: " + ", ".join(faltantes))

for placeholder, imagen in imagen_map.items():
    if placeholder not in shell:
        raise SystemExit(f"Placeholder no encontrado en template.html: {placeholder}")
    shell = shell.replace(placeholder, como_data_uri(imagen))

salida = MANUAL / "index.html"
salida.write_text(shell, encoding="utf-8")
print("generado", salida, len(shell), "bytes")
