#!/bin/sh
# Carga un complejo de ejemplo: seis salas, unas películas y su programación.
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

alta() {
    respuesta=$(curl -s -w '\n%{http_code}' -X POST "$API/$1" \
        -H 'Content-Type: application/json' -d "$2")
    codigo=$(echo "$respuesta" | tail -1)
    cuerpo=$(echo "$respuesta" | sed '$d')
    if [ "$codigo" = "201" ]; then
        echo "  ok    $3"
    else
        echo "  $codigo   $3 -> $cuerpo"
    fi
}

echo "Películas"
alta peliculas '{"titulo":"Matrix","duracionMinutos":136,"generos":["ACCION","CIENCIA_FICCION"],
  "clasificacion":"MAS_16","director":"Lana y Lilly Wachowski","anio":1999,"idiomaOriginal":"Inglés",
  "sinopsis":"Un programador descubre que la realidad que conoce es una simulación.",
  "posterUrl":"https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg","enCartelera":true}' "Matrix"

alta peliculas '{"titulo":"El Padrino","duracionMinutos":175,"generos":["DRAMA","SUSPENSO"],
  "clasificacion":"MAS_16","director":"Francis Ford Coppola","anio":1972,"idiomaOriginal":"Inglés",
  "sinopsis":"El patriarca de una familia mafiosa le pasa el control a su hijo menor.",
  "posterUrl":"https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg","enCartelera":true}' "El Padrino"

alta peliculas '{"titulo":"Intensamente","duracionMinutos":95,"generos":["ANIMACION","COMEDIA"],
  "clasificacion":"ATP","director":"Pete Docter","anio":2015,"idiomaOriginal":"Inglés",
  "sinopsis":"Las cinco emociones de una nena de once años se quedan sin su guía.",
  "posterUrl":"https://image.tmdb.org/t/p/w500/2H1TmgdfNtsKlU9jKdeNyYL5y8T.jpg","enCartelera":true}' "Intensamente"

alta peliculas '{"titulo":"El Resplandor","duracionMinutos":146,"generos":["TERROR","SUSPENSO"],
  "clasificacion":"MAS_18","director":"Stanley Kubrick","anio":1980,"idiomaOriginal":"Inglés",
  "sinopsis":"Un escritor cuida un hotel vacío durante el invierno y empieza a perder la cabeza.",
  "posterUrl":"https://image.tmdb.org/t/p/w500/xazWoLealQwEgqZ89MLZklLZD3k.jpg","enCartelera":true}' "El Resplandor"

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

echo "Funciones"
HOY=$(date +%F)
MANANA=$(date -v+1d +%F 2>/dev/null || date -d '+1 day' +%F)

alta funciones "{\"peliculaId\":1,\"salaId\":1,\"inicio\":\"${HOY}T20:30:00\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000}" "Matrix, Sala 1, hoy 20:30"
alta funciones "{\"peliculaId\":1,\"salaId\":3,\"inicio\":\"${HOY}T22:00:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"TRES_D\",\"precio\":5500}" "Matrix 3D, Sala 3, hoy 22:00"
alta funciones "{\"peliculaId\":2,\"salaId\":2,\"inicio\":\"${HOY}T19:00:00\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":4800}" "El Padrino, Sala 2, hoy 19:00"
alta funciones "{\"peliculaId\":3,\"salaId\":6,\"inicio\":\"${HOY}T15:00:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":4500}" "Intensamente 4D, Sala 6, hoy 15:00"
alta funciones "{\"peliculaId\":3,\"salaId\":5,\"inicio\":\"${HOY}T17:30:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":4500}" "Intensamente, Sala 5, hoy 17:30"
alta funciones "{\"peliculaId\":4,\"salaId\":4,\"inicio\":\"${HOY}T23:00:00\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":5200}" "El Resplandor, Sala 4, hoy 23:00"
alta funciones "{\"peliculaId\":2,\"salaId\":1,\"inicio\":\"${MANANA}T21:00:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000}" "El Padrino, Sala 1, mañana 21:00"
alta funciones "{\"peliculaId\":4,\"salaId\":3,\"inicio\":\"${MANANA}T22:30:00\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"TRES_D\",\"precio\":5500}" "El Resplandor 3D, Sala 3, mañana 22:30"

echo "Listo."
