#!/usr/bin/env bash
# Deja el sistema andando desde cero. Idempotente: se puede correr de nuevo sin romper nada.
#
#     ./setup.sh
#
# Equivalente para Windows: setup.ps1

set -euo pipefail
cd "$(dirname "$0")"

ok()    { printf '  \033[32m✓\033[0m %s\n' "$1"; }
info()  { printf '  \033[34m·\033[0m %s\n' "$1"; }
aviso() { printf '  \033[33m!\033[0m %s\n' "$1"; }
fatal() { printf '  \033[31m✗\033[0m %s\n' "$1" >&2; exit 1; }
paso()  { printf '\n\033[1m%s\033[0m\n' "$1"; }

clave() {
  # 24 caracteres alfanuméricos. Sin símbolos: van a un .env que leen varios programas.
  #
  # El bloque de /dev/urandom se lee acotado y de una: con `tr ... | head -c 24`, head
  # cierra el pipe apenas junta 24 y tr muere por SIGPIPE, que con pipefail voltea el
  # script entero. cut consume todo el stream, así que no hay pipe que se cierre antes.
  LC_ALL=C head -c 4096 /dev/urandom | LC_ALL=C tr -dc 'A-Za-z0-9' | cut -c1-24
}

# ---------------------------------------------------------------- 1. requisitos
paso "1/5  Requisitos"

command -v docker >/dev/null 2>&1 || fatal "No encontré docker. Instalá Docker Desktop: https://www.docker.com/products/docker-desktop/"
docker info >/dev/null 2>&1 || fatal "Docker está instalado pero no corriendo. Abrí Docker Desktop y esperá a que arranque."
docker compose version >/dev/null 2>&1 || fatal "Falta 'docker compose'. Actualizá Docker Desktop."
ok "Docker corriendo"

# El repo es publico y el token de TMDB es personal: este hook frena el commit si algo
# que va a subir parece una credencial. Los hooks no se versionan, hay que apuntarlos.
if [ -d ../.githooks ] && [ "$(git -C .. config core.hooksPath 2>/dev/null)" != ".githooks" ]; then
  git -C .. config core.hooksPath .githooks 2>/dev/null && ok "Hook anti-credenciales activado"
fi

# ------------------------------------------------------------------- 2. el .env
paso "2/5  Configuración (.env)"

if [ -f .env ]; then
  ok ".env ya existe, no lo toco"
else
  cp .env.example .env
  # Las contraseñas son de tu MySQL, adentro de tu Docker: no las tenés que recordar
  # ni compartir con nadie. Las genero para que nadie se quede con la de ejemplo.
  raiz=$(clave); app=$(clave)
  # -i '' es de BSD/macOS; en GNU/Linux el sufijo va pegado al flag.
  if sed --version >/dev/null 2>&1; then sedi=(-i); else sedi=(-i ''); fi
  sed "${sedi[@]}" "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=${raiz}|" .env
  sed "${sedi[@]}" "s|^DB_PASSWORD=.*|DB_PASSWORD=${app}|" .env
  ok ".env creado con contraseñas nuevas"
fi

# --------------------------------------------------------------- 3. token TMDB
paso "3/5  Token de TMDB (opcional)"

actual=$(grep '^TMDB_TOKEN=' .env | cut -d= -f2- || true)

if [ -n "$actual" ]; then
  ok "Ya tenés un token cargado"
elif [ ! -t 0 ]; then
  aviso "Sin terminal interactiva: dejo el token vacío"
else
  echo "  Sirve para importar la cartelera real de Argentina. El sistema anda sin él:"
  echo "  lo único que no vas a poder hacer es traer películas desde el importador."
  echo
  echo "  Andrei te lo pasa por privado. Si todavía no lo tenés, saltealo con Enter:"
  echo "  se agrega después editando .env y reiniciando con 'docker compose up -d backend'."
  echo
  printf "  Pegalo acá y Enter, o Enter solo para saltearlo: "
  read -rs token; echo
  if [ -n "$token" ]; then
    if sed --version >/dev/null 2>&1; then sedi=(-i); else sedi=(-i ''); fi
    sed "${sedi[@]}" "s|^TMDB_TOKEN=.*|TMDB_TOKEN=${token}|" .env
    ok "Token guardado en .env (que no se versiona)"
  else
    info "Sin token. Lo podés agregar después editando .env"
  fi
fi

# --------------------------------------------------------------- 4. levantar
paso "4/5  Levantando"

info "La primera vez tarda varios minutos: Maven baja las dependencias del backend."
docker compose up -d --build

printf '  Esperando a que los servicios estén sanos'
for _ in $(seq 1 60); do
  sanos=$(docker compose ps --format '{{.Health}}' 2>/dev/null | grep -c '^healthy$' || true)
  [ "$sanos" -ge 2 ] && break
  printf '.'; sleep 5
done
echo

if [ "${sanos:-0}" -lt 2 ]; then
  aviso "Tardaron más de 5 minutos. Mirá qué pasa con:"
  echo "      docker compose ps"
  echo "      docker compose logs backend"
  exit 1
fi
ok "mysql y backend sanos"

# ------------------------------------------------------------------- 5. datos
paso "5/5  Datos de ejemplo"

if [ "$(docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -Nse "SELECT COUNT(*) FROM pelicula"' 2>/dev/null || echo 0)" -gt 0 ]; then
  ok "Ya hay datos cargados, no siembro de nuevo"
else
  ./seed/datos-de-ejemplo.sh >/dev/null 2>&1 && ok "4 películas, 6 salas y 8 funciones" \
    || aviso "Falló el sembrado. Probá a mano: ./seed/datos-de-ejemplo.sh"
fi

puerto=$(grep '^PUERTO_WEB=' .env | cut -d= -f2- || echo 8080)

cat <<FIN

$(printf '\033[1mListo\033[0m')

  Cliente     http://localhost:${puerto:-8080}
  Panel       http://localhost:${puerto:-8080}/admin.html   encargado@cine.uade.ar / cine2026
  Puerta      el mismo panel                        puerta@cine.uade.ar / cine2026
  Adminer     http://localhost:$(grep '^PUERTO_ADMINER=' .env | cut -d= -f2- || echo 8081)   servidor: mysql

  Para bajarlo:  docker compose down
  Guía completa: ../_other/COMO-LEVANTARLO.md

FIN
