#!/bin/sh
# Sube cada webp de listas/ y se lo asigna a la promoción, producto o combo del mismo nombre.
#
#     ./cargar.sh                      # API=http://localhost:8080/api  ADMIN=encargado@cine.uade.ar:cine2026
#
# promo-miercoles-2x1.webp -> promoción "Miércoles 2x1"; producto-pochoclos-grandes.webp -> "Pochoclos grandes";
# combo-clasico.webp -> "Combo clásico". Se compara sin acentos, en minúsculas y con guiones; lo que no existe se saltea.

cd "$(dirname "$0")"
API=${API:-http://localhost:8080/api} ADMIN=${ADMIN:-encargado@cine.uade.ar:cine2026} exec python3 - <<'EOF'
import base64, glob, json, os, re, unicodedata, urllib.request, uuid

API, ADMIN = os.environ["API"], os.environ["ADMIN"]
AUTH = "Basic " + base64.b64encode(ADMIN.encode()).decode()

def pedir(metodo, ruta, cuerpo=None, tipo="application/json"):
    req = urllib.request.Request(API + ruta, data=cuerpo, method=metodo, headers={"Authorization": AUTH, "Content-Type": tipo})
    with urllib.request.urlopen(req) as r:
        texto = r.read()
        return r.status, json.loads(texto) if texto else None

def slug(texto):
    sin_acentos = "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", "-", sin_acentos.lower()).strip("-")

def subir(ruta_webp):
    borde = uuid.uuid4().hex
    archivo = open(ruta_webp, "rb").read()
    cuerpo = (f"--{borde}\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"{os.path.basename(ruta_webp)}\"\r\n"
              f"Content-Type: image/webp\r\n\r\n").encode() + archivo + f"\r\n--{borde}--\r\n".encode()
    return pedir("POST", "/imagenes", cuerpo, f"multipart/form-data; boundary={borde}")[1]["url"]

promos = {slug(p["nombre"]): p for p in pedir("GET", "/promociones")[1]}
productos = {slug(p["nombre"]): p for p in pedir("GET", "/candy/productos?todos=true")[1]}

for webp in sorted(glob.glob("listas/*.webp")):
    nombre = os.path.basename(webp)[:-5]
    if nombre.startswith("promo-"):
        item = promos.get(nombre[len("promo-"):])
    else:
        item = productos.get(nombre[len("producto-"):] if nombre.startswith("producto-") else nombre)
    if not item:
        print(f"  salto {nombre}: no hay ítem con ese nombre")
        continue
    url = subir(webp)
    if nombre.startswith("promo-"):
        cuerpo = {k: v for k, v in item.items() if k not in ("id", "activa")} | {"imagenUrl": url}
        estado, _ = pedir("PUT", f"/promociones/{item['id']}", json.dumps(cuerpo).encode())
    else:
        cuerpo = {"nombre": item["nombre"], "precio": item["precio"], "imagenUrl": url}
        estado, _ = pedir("PUT", f"/candy/productos/{item['id']}", json.dumps(cuerpo).encode())
    print(f"  {estado}   {nombre} -> {item['nombre']}")
EOF
