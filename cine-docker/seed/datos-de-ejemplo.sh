#!/bin/sh
# Carga un complejo de ejemplo: seis salas, la carta del candy, seis películas con una semana
# de funciones y una promoción.
#
# Las películas son clásicos conocidos con el póster de TMDB, para que una instalación nueva no
# abra con la cartelera vacía. La cartelera real la trae el importador (botón Importador del
# panel, con TMDB_TOKEN) y el encargado la confirma en Por revisar. Las funciones salen de la
# grilla automática, la misma del panel, para una semana desde hoy.
#
# Va por la API y no por SQL a propósito: así los datos pasan por las mismas reglas que
# usa la aplicación —R2, R14, nombres únicos— y es imposible sembrar algo que el sistema
# después rechazaría. De paso sirve de prueba de humo de los endpoints de alta.
#
#     ./seed/datos-de-ejemplo.sh
#
# Salas, candy y promo son acumulativos: correrlo dos veces deja errores 409 de nombre repetido,
# que son inofensivos. Películas y funciones no: si la base ya tiene películas no las toca, porque
# la grilla sí sumaría funciones nuevas. Para empezar limpio: docker compose down -v.

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

echo "Películas"
# Con Basic, GET /peliculas lista todas, también las pendientes del importador.
if [ "$(curl -s -u "$ADMIN" "$API/peliculas")" != "[]" ]; then
    echo "  ya hay películas: no siembro ni películas ni funciones"
else
    POSTER=https://image.tmdb.org/t/p/w500
    alta peliculas "{\"titulo\":\"Matrix\",\"duracionMinutos\":136,\"generos\":[\"ACCION\",\"CIENCIA_FICCION\"],
      \"clasificacion\":\"MAS_13\",\"director\":\"Lana y Lilly Wachowski\",\"anio\":1999,\"idiomaOriginal\":\"Inglés\",
      \"sinopsis\":\"Un programador descubre que el mundo en el que vive es una simulación y se suma a la rebelión contra las máquinas.\",
      \"posterUrl\":\"$POSTER/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg\",\"puntaje\":8.2,\"trailerYoutube\":\"vKQi3bBA1y8\",\"enCartelera\":true}" "Matrix"
    alta peliculas "{\"titulo\":\"Toy Story\",\"duracionMinutos\":81,\"generos\":[\"ANIMACION\",\"COMEDIA\"],
      \"clasificacion\":\"ATP\",\"director\":\"John Lasseter\",\"anio\":1995,\"idiomaOriginal\":\"Inglés\",
      \"sinopsis\":\"Woody, el vaquero preferido de Andy, ve peligrar su lugar cuando llega Buzz Lightyear, un guardián espacial que no sabe que es un juguete.\",
      \"posterUrl\":\"$POSTER/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg\",\"puntaje\":8.0,\"trailerYoutube\":\"v-PjgYDrg70\",\"enCartelera\":true}" "Toy Story"
    alta peliculas "{\"titulo\":\"Interestelar\",\"duracionMinutos\":169,\"generos\":[\"CIENCIA_FICCION\",\"DRAMA\"],
      \"clasificacion\":\"MAS_13\",\"director\":\"Christopher Nolan\",\"anio\":2014,\"idiomaOriginal\":\"Inglés\",
      \"sinopsis\":\"Con la Tierra al borde de la extinción, un grupo de astronautas cruza un agujero de gusano en busca de un planeta habitable.\",
      \"posterUrl\":\"$POSTER/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg\",\"puntaje\":8.4,\"trailerYoutube\":\"zSWdZVtXT7E\",\"enCartelera\":true}" "Interestelar"
    alta peliculas "{\"titulo\":\"La La Land\",\"duracionMinutos\":128,\"generos\":[\"ROMANCE\",\"COMEDIA\",\"DRAMA\"],
      \"clasificacion\":\"ATP\",\"director\":\"Damien Chazelle\",\"anio\":2016,\"idiomaOriginal\":\"Inglés\",
      \"sinopsis\":\"Una aspirante a actriz y un pianista de jazz se enamoran en Los Ángeles mientras persiguen sueños que los llevan por caminos distintos.\",
      \"posterUrl\":\"$POSTER/uDO8zWDhfWwoFdKS4fzkUJt0Rf0.jpg\",\"puntaje\":7.9,\"trailerYoutube\":\"0pdqf4P9MB8\",\"enCartelera\":true}" "La La Land"
    alta peliculas "{\"titulo\":\"¡Huye!\",\"duracionMinutos\":104,\"generos\":[\"TERROR\"],
      \"clasificacion\":\"MAS_16\",\"director\":\"Jordan Peele\",\"anio\":2017,\"idiomaOriginal\":\"Inglés\",
      \"sinopsis\":\"Un joven viaja a conocer a la familia de su novia y descubre que la amabilidad de la casa esconde algo mucho peor.\",
      \"posterUrl\":\"$POSTER/tFXcEccSQMf3lfhfXKSU9iRBpa3.jpg\",\"puntaje\":7.6,\"trailerYoutube\":\"DzfpyUB60YY\",\"enCartelera\":true}" "¡Huye!"
    alta peliculas "{\"titulo\":\"Tiempos violentos\",\"duracionMinutos\":154,\"generos\":[\"DRAMA\"],
      \"clasificacion\":\"MAS_18\",\"director\":\"Quentin Tarantino\",\"anio\":1994,\"idiomaOriginal\":\"Inglés\",
      \"sinopsis\":\"Dos sicarios, un boxeador, la mujer de un mafioso y una pareja de asaltantes cruzan sus historias en Los Ángeles.\",
      \"posterUrl\":\"$POSTER/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg\",\"puntaje\":8.5,\"trailerYoutube\":\"s7EdQ4FqbhY\",\"enCartelera\":true}" "Tiempos violentos"

    echo "Funciones"
    # La grilla automática: una semana desde hoy, de 14 a 24, en 2D subtitulada. Necesita las salas de arriba.
    alta grilla '{"dias":7,"cuantasPeliculas":6,"precio":5000}' "Una semana de grilla"
fi

echo "Promociones"
DESDE=$(date +%F)
# Noventa días desde hoy y no una fecha fija: el backend no crea una promoción ya vencida. Se suma en
# segundos porque `date -d @` es de GNU y busybox, y `date -r` el equivalente de macOS.
FIN=$(( $(date +%s) + 90 * 86400 ))
HASTA=$(date -d "@$FIN" +%F 2>/dev/null || date -r "$FIN" +%F)
alta promociones "{\"nombre\":\"Miércoles 2x1\",\"tipo\":\"NXM\",\"lleva\":2,\"paga\":1,
  \"vigenciaDesde\":\"$DESDE\",\"vigenciaHasta\":\"$HASTA\",\"diasSemana\":[\"WEDNESDAY\"]}" "Miércoles 2x1"

echo "Listo."
