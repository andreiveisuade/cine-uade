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

# Las de local son de demo y ya están en el repo (seed/02-admin.sql). La de prod no va en ningún archivo.
credenciales() {
  printf '\n  \033[1mLocal\033[0m        encargado@cine.uade.ar / cine2026   (Puerta: puerta@cine.uade.ar / cine2026)\n'
  printf '  \033[1mProducción\033[0m   encargado@cine.uade.ar / la clave que pusiste al cargar Aiven\n\n'
}

if $abrir; then
  credenciales
  info "Abriendo el panel: elegí Local o Producción al ingresar. Acá vas a ver cada pedido a la API"
  exec java -Dcine.consola=true "${java_args[@]+"${java_args[@]}"}" -jar "$JAR"
fi
