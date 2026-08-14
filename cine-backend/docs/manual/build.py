import base64
import re
from pathlib import Path

MANUAL = Path(__file__).resolve().parent
DIAGRAMAS = MANUAL.parent / "diagramas"
CAPTURAS = MANUAL / "capturas"

svg_map = {
    "{{SVG_CASOS_USO}}": DIAGRAMAS / "casos-de-uso.svg",
    "{{SVG_DOMINIO}}": DIAGRAMAS / "clases-dominio.svg",
    "{{SVG_CAPAS}}": DIAGRAMAS / "clases-capas.svg",
    "{{SVG_SECUENCIA}}": DIAGRAMAS / "secuencia-reserva.svg",
    "{{SVG_SECUENCIA_CANDY}}": DIAGRAMAS / "secuencia-candy.svg",
    "{{SVG_DOCKER}}": DIAGRAMAS / "docker-despliegue.svg",
}


# Capturas de la corrida de pruebas. Van embebidas como data URI por el mismo motivo
# que los SVG: el manual tiene que abrirse de un doble clic, sin servidor. En JPEG y no
# PNG porque son capturas de pantalla completas y pesan la cuarta parte.
imagen_map = {
    "{{IMG_CARTELERA}}": CAPTURAS / "cp10-cartelera.jpg",
    "{{IMG_MAPA}}": CAPTURAS / "cp10-mapa.jpg",
    "{{IMG_TICKET}}": CAPTURAS / "cp10-ticket.jpg",
    "{{IMG_GRILLA}}": CAPTURAS / "cp09-grilla.jpg",
    "{{IMG_PROMOCIONES}}": CAPTURAS / "cp14-promociones.jpg",
    "{{IMG_BLOQUEO}}": CAPTURAS / "cp19-bloqueo.jpg",
    "{{IMG_PUERTA_OK}}": CAPTURAS / "cp16-puerta-adelante.jpg",
    "{{IMG_PUERTA_RECHAZO}}": CAPTURAS / "cp16-puerta-rechazo.jpg",
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
    raise SystemExit("Faltan SVGs (corre plantuml -tsvg *.puml): " + ", ".join(faltantes))

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
