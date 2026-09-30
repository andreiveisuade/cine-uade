#!/bin/bash
# Pruebas de humo de punta a punta contra el sistema levantado con docker compose y el seed cargado.
# Recorre las reglas y los topes que un test de la suite no ve pasar por nginx, MySQL y la red de Docker.
# ESCRIBE DATOS (una película, una función, una reserva, un cobro y candy): solo contra una base desechable.
#
#     ./humo.sh                       # contra http://localhost:8080
#     BASE=http://otro:8080 ./humo.sh
#
# Sale con código 1 si algún caso no da el status y el texto esperados.
B="${BASE:-http://localhost:8080}/api"; A="encargado@cine.uade.ar:cine2026"
ok=0; mal=0
caso() { # nombre, status esperado, texto esperado (parcial), curl args...
  local n="$1" st="$2" tx="$3"; shift 3
  local r; r=$(curl -s -w '\n%{http_code}' "$@"); local c=${r##*$'\n'}; local b=${r%$'\n'*}
  if [ "$c" = "$st" ] && [[ "$b" == *"$tx"* ]]; then ok=$((ok+1)); echo "ok   $n"; else mal=$((mal+1)); echo "MAL  $n -> $c $b"; fi
}
J=(-H 'Content-Type: application/json')
dia() { date -v+"$1"d +%F 2>/dev/null || date -d "+$1 day" +%F; }
HOY=$(date +%F); MAN=$(dia $((3 + RANDOM % 300)))
P=$(curl -s -u $A "${J[@]}" -X POST $B/peliculas -d '{"titulo":"Humo '$RANDOM'","duracionMinutos":100,"generos":["DRAMA"],"clasificacion":"ATP"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
S=$(curl -s -u $A $B/salas | python3 -c 'import sys,json;print([s["id"] for s in json.load(sys.stdin) if s["tipo"]=="DOS_D"][0])')
F=$(curl -s -u $A "${J[@]}" -X POST $B/funciones -d '{"peliculaId":'$P',"salaId":'$S',"inicio":"'$MAN'T20:00:00","idioma":"SUBTITULADA","proyeccion":"DOS_D","precio":5000}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
echo "pelicula $P sala $S funcion $F"
BUT=$(curl -s $B/funciones/$F | python3 -c 'import sys,json;d=json.load(sys.stdin);print(" ".join(a["codigo"] for a in d["asientos"] if a["estado"]=="HABILITADO" and not a["ocupado"]))')
set -- $BUT
once=$(printf '"%s":null,' "${@:1:11}"); once="{${once%,}}"
caso "reserva de 11 butacas" 400 "como máximo 10" "${J[@]}" -X POST $B/reservas -d '{"funcionId":'$F',"nombre":"Ana","email":"ana@mail.com","butacas":'$once'}'
caso "decimal en un entero" 400 "funcionId" "${J[@]}" -X POST $B/reservas -d '{"funcionId":'$F'.5,"nombre":"Ana","email":"ana@mail.com","butacas":{"'$1'":null}}'
caso "tarifa inválida" 400 "no es válida" "${J[@]}" -X POST $B/reservas -d '{"funcionId":'$F',"nombre":"Ana","email":"ana@mail.com","butacas":{"'$1'":"VIP"}}'
R=$(curl -s "${J[@]}" -X POST $B/reservas -d '{"funcionId":'$F',"nombre":"Ana","email":"ana@mail.com","butacas":{"'$1'":null,"'$2'":"jubilado"}}')
RID=$(echo "$R" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])'); COD=$(echo "$R" | python3 -c 'import sys,json;print(json.load(sys.stdin)["codigo"])')
echo "reserva $RID codigo $COD"
caso "butaca ya vendida" 409 "" "${J[@]}" -X POST $B/reservas -d '{"funcionId":'$F',"nombre":"Bea","email":"bea@mail.com","butacas":{"'$1'":null}}'
caso "candy para reserva sin pagar" 400 "no está pagada" -u $A "${J[@]}" -X POST $B/candy/compras -d '{"reservaId":'$RID',"cantidades":{"1":1},"medio":"EFECTIVO"}'
caso "efectivo con código" 400 "no lleva código" -u $A "${J[@]}" -X POST $B/reservas/$RID/pago -d '{"medio":"EFECTIVO","codigoAutorizacion":"X1"}'
caso "cobro en efectivo" 201 "" -u $A "${J[@]}" -X POST $B/reservas/$RID/pago -d '{"medio":"EFECTIVO"}'
caso "candy para reserva pagada" 201 "" -u $A "${J[@]}" -X POST $B/candy/compras -d '{"reservaId":'$RID',"cantidades":{"1":1},"medio":"EFECTIVO"}'
caso "candy con 21 unidades" 400 "como máximo 20" -u $A "${J[@]}" -X POST $B/candy/compras -d '{"cantidades":{"1":21},"medio":"EFECTIVO"}'
caso "puerta antes del día" 400 "se entra solo ese día" -u puerta@cine.uade.ar:cine2026 "${J[@]}" -X POST $B/acceso -d '{"codigo":"'$COD'"}'
caso "función a más de un año" 400 "próximo año" -u $A "${J[@]}" -X POST $B/funciones -d '{"peliculaId":'$P',"salaId":'$S',"inicio":"'$(dia 400)'T20:00:00","idioma":"SUBTITULADA","proyeccion":"DOS_D","precio":5000}'
caso "función con segundos" 400 "sin segundos" -u $A "${J[@]}" -X POST $B/funciones -d '{"peliculaId":'$P',"salaId":'$S',"inicio":"'$(dia 2)'T20:30:46","idioma":"SUBTITULADA","proyeccion":"DOS_D","precio":5000}'
caso "fecha año 999999999" 400 "1000 y 9999" -u $A "$B/arqueo?fecha=%2B999999999-12-31"
caso "programación de 2 años" 400 "366 días" -u $A "${J[@]}" -X POST $B/programaciones -d '{"peliculaId":'$P',"salaId":'$S',"desde":"'$MAN'","hasta":"'$(dia 700)'","horaInicio":"22:00","diasSemana":["MONDAY"],"idioma":"SUBTITULADA","proyeccion":"DOS_D","precio":5000}'
caso "duración 2147483647" 400 "entre 1 y 600" -u $A "${J[@]}" -X POST $B/peliculas -d '{"titulo":"Larga","duracionMinutos":2147483647,"generos":["DRAMA"],"clasificacion":"ATP"}'
caso "limpieza 500" 400 "120 minutos" -u $A "${J[@]}" -X POST $B/salas -d '{"nombre":"Sucia","tipo":"DOS_D","butacasPorFila":[5],"minutosLimpieza":500}'
caso "porcentaje 99.999" 400 "" -u $A "${J[@]}" -X POST $B/promociones -d '{"nombre":"Casi gratis","tipo":"PORCENTAJE","porcentaje":99.999,"vigenciaDesde":"'$HOY'","vigenciaHasta":"'$(dia 30)'","diasSemana":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]}'
caso "firewall en json" 400 "El pedido no es válido" "$B/peliculas/1;x=1"
caso "cuerpo de 2 MB (nginx 413)" 413 '"error"' -u $A "${J[@]}" -X POST $B/salas --data-binary @<(head -c 2000000 /dev/zero | tr '\0' a)
caso "pendiente oculta al público" 404 "No existe la película" "$B/peliculas/999999"
caso "401 en json" 401 "Email o contraseña incorrectos" -u encargado@cine.uade.ar:mala -X POST $B/sesion
echo "== $ok ok, $mal mal"
[ "$mal" -eq 0 ]
