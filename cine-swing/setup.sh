#!/usr/bin/env bash
# Arma el panel del encargado y lo abre. Recompila solo si cambió algo desde el último jar.
#
#     ./setup.sh                    # arma (si hace falta) y abre; el servidor se elige al ingresar
#     ./setup.sh --tests            # además corre los tests antes de armar
#     ./setup.sh --solo-armar       # arma y no abre
#     ./setup.sh -Dcine.tema=claro  # lo demás va tal cual a java

set -euo pipefail
cd "$(dirname "$0")"

ok()    { printf '  \033[32m✓\033[0m %s\n' "$1"; }
info()  { printf '  \033[34m·\033[0m %s\n' "$1"; }
fatal() { printf '  \033[31m✗\033[0m %s\n' "$1" >&2; exit 1; }

JAR=target/cine-swing.jar
tests=false; abrir=true; java_args=()
for arg in "$@"; do
  case "$arg" in
    --tests) tests=true ;;
    --solo-armar) abrir=false ;;
    *) java_args+=("$arg") ;;
  esac
done

java -version 2>&1 | grep -q 'version "21' || fatal "Falta Java 21. Instalalo con: brew install openjdk@21"
ok "Java 21"

# Maven no hace falta instalado: mvnw baja el de la versión fijada en .mvn/.
if $tests; then
  info "Corriendo los tests (contra un servidor HTTP falso, sin backend)"
  ./mvnw -B -q test || fatal "Fallaron los tests: el detalle está en target/surefire-reports/"
  ok "Tests"
fi

if [ ! -f "$JAR" ] || [ -n "$(find src pom.xml -newer "$JAR" -print -quit)" ]; then
  info "Armando el jar (la primera vez baja las dependencias)"
  ./mvnw -B -q package -DskipTests || fatal "No compiló: corré ./mvnw package para ver el error"
  ok "Armado $JAR"
else
  ok "$JAR al día, no hace falta recompilar"
fi

# Las de local son de demo y ya están en el repo (seed/02-admin.sql). Las de prod nunca se imprimen: viven en el
# llavero de macOS y van al portapapeles un rato, así no quedan en el historial ni en el scroll de la terminal.
LLAVERO=cine-uade-prod
ENCARGADO=encargado@cine.uade.ar
credenciales() {
  printf '\n  \033[1mLocal\033[0m        %s / cine2026   (Puerta: puerta@cine.uade.ar / cine2026)\n' "$ENCARGADO"
  if ! command -v security >/dev/null 2>&1; then
    printf '  \033[1mProducción\033[0m   %s / la clave que pusiste al cargar Aiven\n\n' "$ENCARGADO"
    return
  fi
  if clave=$(security find-generic-password -s "$LLAVERO" -a "$ENCARGADO" -w 2>/dev/null); then
    printf '%s' "$clave" | pbcopy
    printf '  \033[1mProducción\033[0m   %s / clave copiada al portapapeles (se borra en 60 s)\n\n' "$ENCARGADO"
    # Solo la borra si sigue siendo ella: si copiaste otra cosa en el medio, no la pisa.
    ( sleep 60; [ "$(pbpaste)" = "$clave" ] && printf '' | pbcopy ) >/dev/null 2>&1 &
  else
    printf '  \033[1mProducción\033[0m   %s / sin clave en el llavero. Guardala una vez (la pide sin eco):\n' "$ENCARGADO"
    printf '               security add-generic-password -s %s -a %s -w\n\n' "$LLAVERO" "$ENCARGADO"
  fi
}

if $abrir; then
  credenciales
  info "Abriendo el panel: elegí Local o Producción al ingresar. Acá vas a ver cada pedido a la API"
  exec java -Dcine.consola=true "${java_args[@]+"${java_args[@]}"}" -jar "$JAR"
fi
