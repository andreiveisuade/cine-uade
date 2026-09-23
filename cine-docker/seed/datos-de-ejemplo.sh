#!/bin/sh
# Carga un complejo de ejemplo: seis salas, la carta del candy y una promoción.
#
# Películas no: las trae el importador de TMDB (botón Importador del panel) y el
# encargado las confirma en Por revisar. Sembrar películas inventadas mezclaba datos de
# prueba con la cartelera real, y con ids fijos el seed se rompía apenas la base ya tenía
# importadas. Las funciones salen después, de la Grilla o del Planificador.
#
# Va por la API y no por SQL a propósito: así los datos pasan por las mismas reglas que
# usa la aplicación —R1, R2, R3, R7, R8— y es imposible sembrar algo que el sistema
# después rechazaría. De paso sirve de prueba de humo de los endpoints de alta.
#
#     ./seed/datos-de-ejemplo.sh
#
# Es acumulativo, no idempotente: correrlo dos veces deja errores de título y nombre
# repetidos (R1), que son inofensivos. Para empezar limpio: docker compose down -v.

set -e
API="${API:-http://localhost:8080/api}"
# Las altas son del encargado: van con sus credenciales, las mismas del seed 02-admin.sql.
ADMIN="${ADMIN:-encargado@cine.uade.ar:cine2026}"

alta() {
    ULTIMO_ID=""
    respuesta=$(curl -s -w '\n%{http_code}' -u "$ADMIN" -X POST "$API/$1" \
        -H 'Content-Type: application/json' -d "$2")
    codigo=$(echo "$respuesta" | tail -1)
    cuerpo=$(echo "$respuesta" | sed '$d')
    if [ "$codigo" = "201" ]; then
        echo "  ok    $3"
        ULTIMO_ID=$(echo "$cuerpo" | sed -n 's/^{"id":\([0-9]*\).*/\1/p')
    else
        echo "  $codigo   $3 -> $cuerpo"
    fi
}

echo "Salas"
# En cuña: las filas de adelante son más cortas. Las accesibles van a los bordes de A.
alta salas '{"nombre":"Sala 1","tipo":"IMAX","butacasPorFila":[8,10,12,12,14],
  "codigosAccesibles":["A1","A8"]}' "Sala 1 IMAX, 56 butacas"
alta salas '{"nombre":"Sala 2","tipo":"IMAX","butacasPorFila":[14,14,14,14,14,14,14,14],
  "codigosAccesibles":["A1","A14"]}' "Sala 2 IMAX, 112 butacas"
alta salas '{"nombre":"Sala 3","tipo":"TRES_D","butacasPorFila":[12,14,16,18,20],
  "codigosVip":["E7","E8","E9","E10","E11","E12","E13","E14"]}' "Sala 3 3D, 80 butacas con VIP al fondo"
alta salas '{"nombre":"Sala 4","tipo":"TRES_D","butacasPorFila":[16,18,20,22,24],
  "codigosAccesibles":["A1","A2","A15","A16"]}' "Sala 4 3D, 100 butacas"
# Butacas de a dos, sin apoyabrazos en el medio: la sala de parejas.
alta salas '{"nombre":"Sala 5","tipo":"DOS_D","butacasPorFila":[6,6,8,8],
  "codigosPareja":["A1","A2","A3","A4","A5","A6","B1","B2","B3","B4","B5","B6"]}' "Sala 5, 28 butacas de pareja"
alta salas '{"nombre":"Sala 6","tipo":"CUATRO_D","butacasPorFila":[10,12,12,14,14,12]}' "Sala 6 4D, 74 butacas móviles"

echo "Candy"
alta candy/productos '{"nombre":"Pochoclos grandes","tipo":"POCHOCLOS","precio":4000}' "Pochoclos grandes"
POCHOCLOS=$ULTIMO_ID
alta candy/productos '{"nombre":"Pochoclos medianos","tipo":"POCHOCLOS","precio":3200}' "Pochoclos medianos"
alta candy/productos '{"nombre":"Gaseosa 500ml","tipo":"BEBIDA","precio":2500}' "Gaseosa 500ml"
GASEOSA=$ULTIMO_ID
alta candy/productos '{"nombre":"Agua 500ml","tipo":"BEBIDA","precio":1800}' "Agua 500ml"
alta candy/productos '{"nombre":"Chocolate","tipo":"GOLOSINA","precio":1500}' "Chocolate"
# R14: el combo tiene que salir menos que sus componentes sueltos ($ 6500).
if [ -n "$POCHOCLOS" ] && [ -n "$GASEOSA" ]; then
    alta candy/combos "{\"nombre\":\"Combo clásico\",\"precio\":5500,
      \"componentes\":{\"$POCHOCLOS\":1,\"$GASEOSA\":1}}" "Combo clásico"
fi

echo "Promociones"
DESDE=$(date +%F)
alta promociones "{\"nombre\":\"Miércoles 2x1\",\"tipo\":\"NXM\",\"lleva\":2,\"paga\":1,
  \"vigenciaDesde\":\"$DESDE\",\"vigenciaHasta\":\"2026-12-31\",\"diasSemana\":[\"WEDNESDAY\"]}" "Miércoles 2x1"

echo "Listo. Las películas se importan desde el panel: Importador y después Por revisar."
