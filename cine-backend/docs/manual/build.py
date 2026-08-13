import re
from pathlib import Path

MANUAL = Path(__file__).resolve().parent
DIAGRAMAS = MANUAL.parent / "diagramas"

svg_map = {
    "{{SVG_CASOS_USO}}": DIAGRAMAS / "casos-de-uso.svg",
    "{{SVG_DOMINIO}}": DIAGRAMAS / "clases-dominio.svg",
    "{{SVG_CAPAS}}": DIAGRAMAS / "clases-capas.svg",
    "{{SVG_SECUENCIA}}": DIAGRAMAS / "secuencia-reserva.svg",
    "{{SVG_SECUENCIA_CANDY}}": DIAGRAMAS / "secuencia-candy.svg",
}


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

salida = MANUAL / "index.html"
salida.write_text(shell, encoding="utf-8")
print("generado", salida, len(shell), "bytes")
