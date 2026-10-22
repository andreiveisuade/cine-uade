import re, sys
from html import escape
from pathlib import Path

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.cwd()
SCHEMA = (ROOT / "cine-backend/src/main/resources/schema.sql").read_text()
OUT = ROOT / "_other/docs/diagramas/der-design.html"

# ---------- parse ----------
tables = {}
order = []
for m in re.finditer(r"CREATE TABLE IF NOT EXISTS (\w+) \((.*?)\n\);", SCHEMA, re.S):
    name, body = m.group(1), m.group(2)
    cols, pk, uqs, fks = [], [], [], []
    for line in body.split("\n"):
        line = line.strip().rstrip(",")
        if not line or line.startswith("--"):
            continue
        if line.startswith("PRIMARY KEY"):
            pk = [c.strip() for c in re.search(r"\((.*)\)", line).group(1).split(",")]
        elif line.startswith("UNIQUE"):
            uqs.append([c.strip() for c in re.search(r"\((.*)\)", line).group(1).split(",")])
        elif line.startswith("FOREIGN KEY"):
            fm = re.match(r"FOREIGN KEY \((\w+)\) REFERENCES (\w+)\((\w+)\)( ON DELETE CASCADE)?", line)
            fks.append(dict(col=fm.group(1), ref=fm.group(2), refcol=fm.group(3), cascade=bool(fm.group(4))))
        else:
            cm = re.match(r"(\w+) ([A-Z]+(?:\([\d,]+\))?)(.*)", line)
            cname, ctype, rest = cm.groups()
            d = re.search(r"DEFAULT ('[^']*'|\S+)", rest)
            cols.append(dict(name=cname, type=ctype, pk="PRIMARY KEY" in rest, ai="AUTO_INCREMENT" in rest,
                             nn="NOT NULL" in rest, uq=" UNIQUE" in rest, default=d.group(1) if d else None))
    for c in cols:
        if c["pk"]:
            pk = [c["name"]]
    fkcols = {f["col"]: f for f in fks}
    for c in cols:
        c["fk"] = fkcols.get(c["name"])
        c["inpk"] = c["name"] in pk
    tables[name] = dict(name=name, cols=cols, pk=pk, uqs=uqs, fks=fks)
    order.append(name)

# ---------- layout ----------
ZONES = {
    "cartelera": ("Cartelera", "#3d6b8f"),
    "funciones": ("Funciones y programaciones", "#6b5a8f"),
    "salas": ("Salas", "#4f7a5a"),
    "ventas": ("Ventas", "#8f4a3d"),
    "usuarios": ("Usuarios", "#7a6a3d"),
    "promociones": ("Promociones", "#8f5a6b"),
    "candy": ("Candy", "#3d7a7a"),
    "imagenes": ("Imágenes", "#5a5a5a"),
}
ZONE_OF = {
    "pelicula": "cartelera", "pelicula_genero": "cartelera", "importacion": "cartelera",
    "programacion": "funciones", "programacion_dia": "funciones", "funcion": "funciones",
    "sala": "salas", "asiento": "salas",
    "bloqueo_butaca": "ventas", "reserva": "ventas", "entrada": "ventas", "pago": "ventas",
    "usuario": "usuarios",
    "promocion": "promociones", "promocion_dia": "promociones", "promocion_medio": "promociones",
    "producto": "candy", "combo_item": "candy", "compra_candy": "candy", "item_compra": "candy",
    "imagen": "imagenes",
}
COLUMNS = [
    ["pelicula", "pelicula_genero", "importacion", "imagen"],
    ["programacion", "programacion_dia", "funcion"],
    ["sala", "asiento", "bloqueo_butaca"],
    ["reserva", "entrada", "pago"],
    ["usuario", "promocion", "promocion_dia", "promocion_medio"],
    ["compra_candy", "item_compra", "producto", "combo_item"],
]
# Gutter i is to the left of column i; gutter len(COLUMNS) is right of the last column.
# (child, fkcol): (child side gutter, parent side gutter)  -- same gutter = direct route, else corridor
ROUTES = {
    ("pelicula_genero", "pelicula_id"): (0, 0),
    ("asiento", "sala_id"): (3, 3),
    ("programacion", "pelicula_id"): (1, 1),
    ("programacion", "sala_id"): (2, 2),
    ("programacion_dia", "programacion_id"): (1, 1),
    ("funcion", "pelicula_id"): (1, 1),
    ("funcion", "sala_id"): (2, 2),
    ("funcion", "programacion_id"): (2, 2),
    ("reserva", "funcion_id"): (3, 2),
    ("reserva", "cliente_id"): (4, 4),
    ("entrada", "reserva_id"): (4, 4),
    ("entrada", "asiento_id"): (3, 3),
    ("entrada", "funcion_id"): (3, 2),
    ("bloqueo_butaca", "funcion_id"): (2, 2),
    ("bloqueo_butaca", "asiento_id"): (3, 3),
    ("promocion_dia", "promocion_id"): (5, 5),
    ("promocion_medio", "promocion_id"): (5, 5),
    ("pago", "reserva_id"): (3, 3),
    ("pago", "promocion_id"): (4, 4),
    ("combo_item", "combo_id"): (6, 6),
    ("combo_item", "producto_id"): (6, 6),
    ("compra_candy", "cliente_id"): (5, 5),
    ("compra_candy", "reserva_id"): (5, 4),
    ("item_compra", "compra_id"): (6, 6),
    ("item_compra", "producto_id"): (6, 6),
}
NOTES = {
    "entrada": [("UQ (funcion_id, asiento_id): una butaca, una venta por funcion (R4)", True),
                ("funcion_id NULL al cancelar: el UNIQUE no aplica a NULL", False)],
    "bloqueo_butaca": [("PK (funcion_id, asiento_id): una sola sesion aparta la butaca", True)],
    "reserva": [("version = @Version: cobrar y cancelar a la vez, uno pierde", False),
                ("codigo UQ: la credencial del QR, no el id", False)],
    "pago": [("reserva_id UQ: un pago por reserva", False),
             ("el descuento vive aca, no en entrada (R15)", False)],
    "combo_item": [("R14 (combo mas barato que la suma) la valida Producto", False)],
    "importacion": [("sin FK a pelicula: lo trae pelicula.estado_revision", False)],
}

ROW = 24
HEAD = 30
TW = 400
GUT = 150
TOP = 150
VGAP = 56
NOTE_H = 18
X0 = 170

def col_label_y(tname, cname):
    return tables[tname]["rowy"][cname]

# incoming counts per (parent, side)
gx = lambda g: X0 + g * (TW + GUT)          # left edge of column g  (gutter g ends here)
gutter_left = lambda g: gx(g) - GUT            # gutter g spans [gx(g)-GUT, gx(g)]

col_of = {t: i for i, c in enumerate(COLUMNS) for t in c}
side_of = lambda t, g: "L" if g == col_of[t] else "R"

incoming = {}
for (child, fc), (gc, gp) in ROUTES.items():
    f = tables[child]["cols"][[c["name"] for c in tables[child]["cols"]].index(fc)]["fk"]
    incoming.setdefault((f["ref"], side_of(f["ref"], gp)), []).append((child, fc))

for i, colt in enumerate(COLUMNS):
    y = TOP
    prev_zone = None
    for t in colt:
        T = tables[t]
        if prev_zone and prev_zone != ZONE_OF[t]:
            y += 40
        prev_zone = ZONE_OF[t]
        T["x"], T["y"] = gx(i) + 0, y
        T["rowy"], T["rowh"] = {}, {}
        ry = y + HEAD
        for c in T["cols"]:
            n = max(len(incoming.get((t, s), [])) for s in "LR") if c["name"] == "id" else 0
            h = max(ROW, 16 * (n + 1)) if n >= 2 else ROW
            T["rowy"][c["name"]] = ry
            T["rowh"][c["name"]] = h
            ry += h
        notes = []
        if len(T["pk"]) > 1:
            notes.append(("PK (" + ", ".join(T["pk"]) + ")", False))
        for u in T["uqs"]:
            notes.append(("UQ (" + ", ".join(u) + ")", False))
        extra = NOTES.get(t, [])
        notes = [n for n in notes if not any(e[0].startswith(n[0]) for e in extra)] + extra
        T["notes"] = notes
        T["h"] = ry - y + (10 + NOTE_H * len(notes) if notes else 0)
        y += T["h"] + VGAP

bottom = max(T["y"] + T["h"] for T in tables.values())

# ---------- edges ----------
edges = []
for (child, fc), (gc, gp) in ROUTES.items():
    C = tables[child]
    f = next(c for c in C["cols"] if c["name"] == fc)
    fk = f["fk"]
    P = tables[fk["ref"]]
    cs, ps = side_of(child, gc), side_of(fk["ref"], gp)
    cy = C["rowy"][fc] + ROW / 2
    lst = incoming[(fk["ref"], ps)]
    k = lst.index((child, fc))
    rh = P["rowh"][fk["refcol"]]
    py = P["rowy"][fk["refcol"]] + rh * (k + 1) / (len(lst) + 1) if len(lst) > 1 else P["rowy"][fk["refcol"]] + ROW / 2
    cx = C["x"] if cs == "L" else C["x"] + TW
    px = P["x"] if ps == "L" else P["x"] + TW
    edges.append(dict(child=child, col=fc, parent=fk["ref"], gc=gc, gp=gp, cx=cx, cy=cy, px=px, py=py,
                      cs=cs, ps=ps, nullable=not f["nn"], unique=f["uq"], cascade=fk["cascade"]))

# lanes: per gutter, longer vertical spans farther from the tables they touch
LANE = 14
by_gutter = {}
for e in edges:
    if e["gc"] == e["gp"]:
        by_gutter.setdefault(e["gc"], []).append(("d", e))
    else:
        by_gutter.setdefault(e["gc"], []).append(("c", e))
        by_gutter.setdefault(e["gp"], []).append(("p", e))
corr = [e for e in edges if e["gc"] != e["gp"]]
for j, e in enumerate(sorted(corr, key=lambda e: e["cy"])):
    e["corr_y"] = bottom + 36 + j * LANE
LANE_ORDER = {}
for g, items in by_gutter.items():
    def span(it):
        kind, e = it
        if kind == "d":
            return abs(e["cy"] - e["py"])
        return 10000 - (e["cy"] if kind == "c" else e["py"])
    items.sort(key=span)
    n = len(items)
    left, right = gutter_left(g), gx(g)
    for i, (kind, e) in enumerate(items):
        x = left + 36 + i * (GUT - 72) / max(n - 1, 1) if n > 1 else (left + right) / 2
        x = round(x / 2) * 2
        e["lane_" + kind] = x

def elbow(pts, r=8):
    d = f"M {pts[0][0]:.1f} {pts[0][1]:.1f}"
    for i in range(1, len(pts) - 1):
        (x0, y0), (x1, y1), (x2, y2) = pts[i - 1], pts[i], pts[i + 1]
        import math
        l1 = math.hypot(x1 - x0, y1 - y0); l2 = math.hypot(x2 - x1, y2 - y1)
        rr = min(r, l1 / 2, l2 / 2)
        if rr < 0.5:
            d += f" L {x1:.1f} {y1:.1f}"; continue
        ax = x1 - (x1 - x0) / l1 * rr; ay = y1 - (y1 - y0) / l1 * rr
        bx = x1 + (x2 - x1) / l2 * rr; by = y1 + (y2 - y1) / l2 * rr
        d += f" L {ax:.1f} {ay:.1f} Q {x1:.1f} {y1:.1f} {bx:.1f} {by:.1f}"
    d += f" L {pts[-1][0]:.1f} {pts[-1][1]:.1f}"
    return d

for e in edges:
    if e["gc"] == e["gp"]:
        x = e["lane_d"]
        pts = [(e["cx"], e["cy"]), (x, e["cy"]), (x, e["py"]), (e["px"], e["py"])]
    else:
        pts = [(e["cx"], e["cy"]), (e["lane_c"], e["cy"]), (e["lane_c"], e["corr_y"]),
               (e["lane_p"], e["corr_y"]), (e["lane_p"], e["py"]), (e["px"], e["py"])]
    e["d"] = elbow(pts)

# ---------- render ----------
MUTED = "#4f5d75"; INK = "#2d3142"; PAPER = "#f5f5f5"; ACC = "#bf4520"
W = X0 + len(COLUMNS) * (TW + GUT) + 40
H = max(e.get("corr_y", 0) for e in edges) + 130
out = []
o = out.append

def ends(x, y, side, kind):
    # side: which way the line leaves the table (L = toward -x)
    s = -1 if side == "L" else 1
    g = []
    if kind == "many":       # crow's foot + circle (0..N)
        g.append(f'<path d="M {x+s*12} {y} L {x} {y-6} M {x+s*12} {y} L {x} {y+6}" stroke="{MUTED}" stroke-width="1.2" fill="none"/>')
        g.append(f'<circle cx="{x+s*18}" cy="{y}" r="3.5" fill="{PAPER}" stroke="{MUTED}" stroke-width="1.2"/>')
    elif kind == "zero_one":
        g.append(f'<line x1="{x+s*8}" y1="{y-6}" x2="{x+s*8}" y2="{y+6}" stroke="{MUTED}" stroke-width="1.2"/>')
        g.append(f'<circle cx="{x+s*16}" cy="{y}" r="3.5" fill="{PAPER}" stroke="{MUTED}" stroke-width="1.2"/>')
    else:                    # exactly one
        for dx in (8, 13):
            g.append(f'<line x1="{x+s*dx}" y1="{y-6}" x2="{x+s*dx}" y2="{y+6}" stroke="{MUTED}" stroke-width="1.2"/>')
    return "".join(g)

# zones
for i, colt in enumerate(COLUMNS):
    groups = []
    for t in colt:
        if groups and groups[-1][0] == ZONE_OF[t]:
            groups[-1][1].append(t)
        else:
            groups.append((ZONE_OF[t], [t]))
    for z, ts in groups:
        label, color = ZONES[z]
        y1 = tables[ts[0]]["y"] - 36
        y2 = tables[ts[-1]]["y"] + tables[ts[-1]]["h"] + 16
        x1 = tables[ts[0]]["x"] - 20
        o(f'<rect x="{x1}" y="{y1}" width="{TW+40}" height="{y2-y1}" rx="8" fill="{color}" fill-opacity="0.06" stroke="{color}" stroke-opacity="0.45" stroke-dasharray="4,4"/>')
        o(f'<text x="{x1+12}" y="{y1+18}" class="eyebrow" fill="{color}">{escape(label.upper())}</text>')

# edges: halo first so crossings read as bridges
for e in edges:
    o(f'<path d="{e["d"]}" fill="none" stroke="{PAPER}" stroke-width="5"/>')
    dash = ' stroke-dasharray="5,4"' if e["nullable"] else ""
    o(f'<path d="{e["d"]}" fill="none" stroke="{MUTED}" stroke-width="1.2"{dash} data-fk="{e["child"]}.{e["col"]}->{e["parent"]}"/>')
    o(ends(e["cx"], e["cy"], e["cs"], "zero_one" if e["unique"] else "many"))
    o(ends(e["px"], e["py"], e["ps"], "zero_one" if e["nullable"] else "one"))

def chip(x, y, text, fill, stroke, color):
    w = 6 + len(text) * 5.6
    return (f'<rect x="{x:.1f}" y="{y-7}" width="{w:.1f}" height="14" rx="2" fill="{fill}" stroke="{stroke}" stroke-width="0.8"/>'
            f'<text x="{x+w/2:.1f}" y="{y+3}" class="chip" text-anchor="middle" fill="{color}">{text}</text>'), w

for t in order:
    T = tables[t]
    x, y = T["x"], T["y"]
    _, zcolor = ZONES[ZONE_OF[t]]
    o(f'<g data-table="{t}">')
    o(f'<rect x="{x}" y="{y}" width="{TW}" height="{T["h"]}" rx="6" fill="#ffffff" stroke="{INK}" stroke-opacity="0.55"/>')
    o(f'<path d="M {x+6} {y} H {x+TW-6} Q {x+TW} {y} {x+TW} {y+6} V {y+HEAD} H {x} V {y+6} Q {x} {y} {x+6} {y} Z" fill="{zcolor}" fill-opacity="0.14"/>')
    o(f'<line x1="{x}" y1="{y+HEAD}" x2="{x+TW}" y2="{y+HEAD}" stroke="{INK}" stroke-opacity="0.3"/>')
    o(f'<text x="{x+12}" y="{y+20}" class="tname">{t}</text>')
    o(f'<text x="{x+TW-12}" y="{y+19}" class="sub" text-anchor="end">{len(T["cols"])} columnas</text>')
    for i, c in enumerate(T["cols"]):
        ry, rh = T["rowy"][c["name"]], T["rowh"][c["name"]]
        if i % 2:
            o(f'<rect x="{x+1}" y="{ry}" width="{TW-2}" height="{rh}" fill="{INK}" fill-opacity="0.03"/>')
        cy = ry + rh / 2
        o(f'<g data-col="{c["name"]}">')
        o(f'<text x="{x+12}" y="{cy+4}" class="cname">{c["name"]}</text>')
        cx = x + 150
        flags = []
        if c["inpk"]: flags.append(("PK", "#2d3142", "#2d3142", "#ffffff"))
        if c["fk"]: flags.append(("FK", "#ffffff", MUTED, MUTED))
        if c["uq"]: flags.append(("UQ", "#ffffff", INK, INK))
        if c["nn"] and not c["pk"]: flags.append(("NN", "#ffffff", "#9aa3b2", MUTED))
        if c["ai"]: flags.append(("AI", "#ffffff", "#9aa3b2", MUTED))
        if c["fk"] and c["fk"]["cascade"]: flags.append(("CASCADE", "#ffffff", "#9aa3b2", MUTED))
        for text, fill, stroke, color in flags:
            s, w = chip(cx, cy, text, fill, stroke, color)
            o(s); cx += w + 4
        typ = c["type"] + ("" if c["nn"] or c["pk"] else " NULL")
        o(f'<text x="{x+TW-12}" y="{cy+4}" class="ctype" text-anchor="end">{escape(typ)}'
          + (f'<tspan class="cdef">&#160;=&#160;{escape(c["default"])}</tspan>' if c["default"] else "") + "</text>")
        o("</g>")
    if T["notes"]:
        ny = T["y"] + T["h"] - 10 - NOTE_H * len(T["notes"])
        o(f'<line x1="{x}" y1="{ny}" x2="{x+TW}" y2="{ny}" stroke="{INK}" stroke-opacity="0.2"/>')
        for k, (n, acc) in enumerate(T["notes"]):
            o(f'<text x="{x+12}" y="{ny+15+k*NOTE_H}" class="note{" acc" if acc else ""}">{escape(n)}</text>')
    o("</g>")

# legend
ly = H - 70
o(f'<line x1="{X0}" y1="{ly-20}" x2="{W-X0}" y2="{ly-20}" stroke="{INK}" stroke-opacity="0.15"/>')
lx = X0
items = [("one", "uno exactamente (FK NOT NULL)"), ("zero_one", "cero o uno (FK NULL o UNIQUE)"), ("many", "cero o muchos")]
for kind, text in items:
    o(f'<line x1="{lx}" y1="{ly}" x2="{lx+40}" y2="{ly}" stroke="{MUTED}" stroke-width="1.2"/>')
    o(ends(lx + 40, ly, "L", kind))
    o(f'<text x="{lx+52}" y="{ly+4}" class="legend">{text}</text>')
    lx += 300
o(f'<line x1="{lx}" y1="{ly}" x2="{lx+40}" y2="{ly}" stroke="{MUTED}" stroke-width="1.2" stroke-dasharray="5,4"/>')
o(f'<text x="{lx+52}" y="{ly+4}" class="legend">FK que admite NULL</text>')
lx += 240
for text in ["PK clave primaria", "FK clave foranea", "UQ unique", "NN not null", "AI auto_increment", "CASCADE on delete cascade"]:
    k, rest = text.split(" ", 1)
    s, w = chip(lx, ly, k, "#2d3142" if k == "PK" else "#ffffff", "#9aa3b2", "#ffffff" if k == "PK" else MUTED)
    o(s)
    o(f'<text x="{lx+w+6}" y="{ly+4}" class="legend">{rest}</text>')
    lx += w + 16 + len(rest) * 6.4
o(f'<text x="{X0}" y="{ly+34}" class="legend">Tipos y defaults tal cual schema.sql (MySQL 8.4); "= valor" es el DEFAULT. Pie de cada tabla: claves compuestas y las reglas que sostiene.</text>')

svg = "\n".join(out)
html = f'''<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Modelo de datos</title>
  <link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@400;500;600&family=Geist+Mono:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}
    :root {{
      --color-paper: #f5f5f5; --color-ink: #2d3142; --color-muted: #4f5d75; --color-accent: #bf4520;
      --font-sans: 'Geist', system-ui, sans-serif;
      --font-serif: 'Instrument Serif', serif;
      --font-mono: 'Geist Mono', ui-monospace, monospace;
    }}
    body {{ font-family: var(--font-sans); background: var(--color-paper); color: var(--color-ink); padding: 3rem 2rem; }}
    .diagram-container {{ width: 100%; overflow-x: auto; }}
    svg {{ width: 100%; min-width: {W}px; display: block; }}
    .tname {{ font-family: var(--font-sans); font-weight: 600; font-size: 14px; fill: #2d3142; }}
    .cname {{ font-family: var(--font-sans); font-weight: 500; font-size: 12px; fill: #2d3142; }}
    .ctype {{ font-family: var(--font-mono); font-size: 10px; fill: #4f5d75; }}
    .cdef {{ fill: #8a93a6; }}
    .chip {{ font-family: var(--font-mono); font-size: 8px; font-weight: 600; letter-spacing: 0.04em; }}
    .sub {{ font-family: var(--font-mono); font-size: 9px; fill: #4f5d75; }}
    .eyebrow {{ font-family: var(--font-mono); font-size: 10px; font-weight: 600; letter-spacing: 0.16em; }}
    .note {{ font-family: var(--font-mono); font-size: 9.5px; fill: #2d3142; }}
    .note.acc {{ fill: #bf4520; font-weight: 600; }}
    .legend {{ font-family: var(--font-mono); font-size: 11px; fill: #4f5d75; }}
    .h1 {{ font-family: var(--font-serif); font-size: 40px; fill: #2d3142; }}
    .eb {{ font-family: var(--font-mono); font-size: 11px; letter-spacing: 0.18em; fill: #4f5d75; }}
  </style>
</head>
<body>
  <div class="diagram-container">
    <svg viewBox="0 0 {W} {H}" xmlns="http://www.w3.org/2000/svg" role="img" aria-labelledby="der-design-title der-design-desc">
      <title id="der-design-title">Modelo de datos del cine</title>
      <desc id="der-design-desc">Las 21 tablas de schema.sql agrupadas en ocho subdominios, con todas sus columnas, tipos, claves, restricciones y las 25 claves foraneas con su cardinalidad.</desc>
      <rect width="{W}" height="{H}" fill="{PAPER}"/>
      <text x="{X0}" y="50" class="eb">SCHEMA.SQL · MYSQL 8.4 · 21 TABLAS · 25 FK</text>
      <text x="{X0}" y="92" class="h1">Modelo de datos del cine</text>
{svg}
    </svg>
  </div>
</body>
</html>
'''
OUT.write_text(html)
print(OUT, W, H)
