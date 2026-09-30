#!/bin/bash
# Regenera un diagrama de Archify como SVG autocontenido, para que build.py lo incruste.
# Uso, desde diagramas/: archify/exportar.sh capas.archify.json
# Requiere la skill archify (~/.agents/skills/archify) y el navegador de cmux: el SVG que arma Archify
# depende del CSS de su página, así que se abre, se copian los estilos calculados a cada elemento y se
# serializa. El export propio de Archify trae un <style> que dentro del manual pisaría otras clases.
set -e
fuente="$1"; salida="${fuente%.archify.json}.svg"
tmp=$(mktemp -d); html="$tmp/diagrama.html"
node ~/.agents/skills/archify/bin/archify.mjs deliver architecture "$fuente" "$html" --quality showcase --json >/dev/null
superficie=$(cmux browser open "file://$html" | grep -o 'surface:[0-9]*')
cmux browser "$superficie" wait --selector ".diagram-container svg" --timeout 10 >/dev/null
cmux browser "$superficie" eval --script "$(cat "$(dirname "$0")/inline.js")" > "$salida"
cmux close-surface --surface "$superficie" >/dev/null 2>&1 || true
echo "$salida: $(wc -c < "$salida") bytes"
