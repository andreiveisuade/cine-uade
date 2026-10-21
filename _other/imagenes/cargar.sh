#!/bin/sh
# Sube cada webp de listas/ y se lo asigna a la promoción o producto del mismo nombre.
#
# Funciona cuando se mergee el backend de imágenes: POST /api/imagenes (multipart, campo
# `archivo`, admin) devuelve {"url"}, e `imagenUrl` se guarda con PUT en promociones y productos.
#
#     ./cargar.sh
#
# El archivo se llama como el ítem: promo-miercoles-2x1.webp -> promoción "Miércoles 2x1";
# producto-pochoclos-grandes.webp / combo-clasico.webp -> producto "Pochoclos grandes" / combo "Combo clásico".
# Se compara sin acentos, en minúsculas y con guiones.

set -e
cd "$(dirname "$0")"
API="${API:-http://localhost:8080/api}"
ADMIN="${ADMIN:-encargado@cine.uade.ar:cine2026}"

# "Miércoles 2x1" -> "miercoles-2x1"
slug() {
    printf '%s' "$1" | perl -MUnicode::Normalize -CS -pe '$_=NFD($_); s/\p{M}//g' | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-//; s/-$//'
}

# Busca en un listado el id cuyo nombre, pasado por slug(), es $2. $1 es el recurso.
buscar_id() {
    curl -s -u "$ADMIN" "$API/$1" | tr '{' '\n' | sed -n 's/.*"id":\([0-9]*\).*"nombre":"\([^"]*\)".*/\1 \2/p' |
    while read -r id nombre; do
        [ "$(slug "$nombre")" = "$2" ] && echo "$id" && break
    done
}

# Reemplaza o agrega imagenUrl en el JSON actual del ítem y lo guarda con PUT.
asignar() {
    actual=$(curl -s -u "$ADMIN" "$API/$1/$2")
    nuevo=$(printf '%s' "$actual" | sed 's/,"imagenUrl":"[^"]*"//; s/,"imagenUrl":null//; s/}$//')",\"imagenUrl\":\"$3\"}"
    curl -s -o /dev/null -w '%{http_code}' -u "$ADMIN" -X PUT "$API/$1/$2" -H 'Content-Type: application/json' -d "$nuevo"
}

for webp in listas/*.webp; do
    [ -f "$webp" ] || continue
    nombre=$(basename "$webp" .webp)
    case "$nombre" in
        promo-*)    recurso=promociones;      clave=${nombre#promo-} ;;
        producto-*) recurso=candy/productos;  clave=${nombre#producto-} ;;
        combo-*)    recurso=candy/combos;     clave=$nombre ;;
        *) echo "  salto $nombre: prefijo desconocido"; continue ;;
    esac
    id=$(buscar_id "$recurso" "$clave")
    if [ -z "$id" ]; then echo "  salto $nombre: no hay ítem con ese nombre"; continue; fi
    url=$(curl -s -u "$ADMIN" -F "archivo=@$webp;type=image/webp" "$API/imagenes" | sed -n 's/.*"url":"\([^"]*\)".*/\1/p')
    if [ -z "$url" ]; then echo "  falló $nombre: la subida no devolvió url"; continue; fi
    echo "  $(asignar "$recurso" "$id" "$url")   $nombre -> $recurso/$id"
done
