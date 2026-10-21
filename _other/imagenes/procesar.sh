#!/bin/sh
# Recorta al aspecto exacto y convierte a webp liviano: originales/ -> listas/.
#
#     ./procesar.sh
#
# Promos (nombre promo-*) 16:9 a 1600x900; productos y combos 1:1 a 1000x1000.
# Usa sips y cwebp (brew install webp), sin instalar nada más. Baja la calidad
# desde 80 hasta que el webp pesa menos de 300 KB.

set -e
cd "$(dirname "$0")"
mkdir -p listas
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
MAX_BYTES=307200

for origen in originales/*.png originales/*.jpg originales/*.jpeg; do
    [ -f "$origen" ] || continue
    base=$(basename "$origen"); nombre="${base%.*}"
    case "$nombre" in
        promo-*) ancho=1600; alto=900 ;;
        *)       ancho=1000; alto=1000 ;;
    esac

    w=$(sips -g pixelWidth "$origen" | awk '/pixelWidth/ {print $2}')
    h=$(sips -g pixelHeight "$origen" | awk '/pixelHeight/ {print $2}')
    # Cubrir el destino: escalar por el lado que más falta y recortar al centro lo que sobra.
    if [ $((w * alto)) -ge $((h * ancho)) ]; then
        sips -s format png --resampleHeight "$alto" "$origen" --out "$TMP/$nombre.png" >/dev/null
    else
        sips -s format png --resampleWidth "$ancho" "$origen" --out "$TMP/$nombre.png" >/dev/null
    fi
    sips -c "$alto" "$ancho" "$TMP/$nombre.png" >/dev/null

    calidad=80
    while :; do
        cwebp -quiet -q "$calidad" "$TMP/$nombre.png" -o "listas/$nombre.webp"
        tam=$(wc -c < "listas/$nombre.webp")
        [ "$tam" -le "$MAX_BYTES" ] || [ "$calidad" -le 30 ] && break
        calidad=$((calidad - 10))
    done
    echo "  ok    $nombre.webp  ${ancho}x${alto}  q$calidad  $((tam / 1024)) KB"
done
