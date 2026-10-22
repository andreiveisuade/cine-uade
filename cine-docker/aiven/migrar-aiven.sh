#!/usr/bin/env bash
# Aplica una migración a la base de Aiven antes del Manual Deploy: Render arranca con ddl-auto validate.
#
#     ./cine-docker/aiven/migrar-aiven.sh clave-temporal      # aplica migracion-clave-temporal.sql
#
# La contraseña se pide sin eco y no queda en el historial. Mismo cliente mysql:8.4 que cargar-aiven.sh.
set -euo pipefail
cd "$(dirname "$0")/../.."

[ $# -eq 1 ] || { echo "Uso: $0 <nombre>   (por ejemplo: clave-temporal)" >&2; exit 2; }
ARCHIVO="cine-backend/src/main/resources/migracion-$1.sql"
[ -f "$ARCHIVO" ] || { echo "No existe $ARCHIVO" >&2; exit 1; }

read -rp "Host de Aiven: " HOST
read -rp "Puerto: " PUERTO
read -rp "Usuario [avnadmin]: " USUARIO; USUARIO=${USUARIO:-avnadmin}
read -rsp "Contraseña de Aiven: " MYSQL_PWD; echo; export MYSQL_PWD

echo "Aplicando $ARCHIVO..."
docker run --rm -i -e MYSQL_PWD mysql:8.4 \
    mysql -h "$HOST" -P "$PUERTO" -u "$USUARIO" --ssl-mode=REQUIRED defaultdb < "$ARCHIVO"
echo "Listo. Ahora: Manual Deploy en Render."
