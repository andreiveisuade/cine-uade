#!/usr/bin/env bash
# Prepara la base de Aiven para el primer deploy: el schema y los dos empleados con claves nuevas.
#
#     ./cine-docker/aiven/cargar-aiven.sh
#
# Hace falta porque en Render `ddl-auto: validate` no crea tablas y nadie corre schema.sql. Las claves
# se piden sin eco y no quedan en el historial; cine2026 es de demo y no puede quedar en internet.
# Usa el cliente de la imagen mysql:8.4 (la misma versión que Aiven), así no hace falta instalarlo.
set -euo pipefail
cd "$(dirname "$0")/../.."

read -rp "Host de Aiven: " HOST
read -rp "Puerto: " PUERTO
read -rp "Usuario [avnadmin]: " USUARIO; USUARIO=${USUARIO:-avnadmin}
read -rsp "Contraseña de Aiven: " MYSQL_PWD; echo; export MYSQL_PWD

clave() {
    local a b
    read -rsp "Clave nueva para $1: " a; echo >&2
    read -rsp "Repetila: " b; echo >&2
    [ "$a" = "$b" ] && [ ${#a} -ge 8 ] || { echo "No coinciden o tienen menos de 8 caracteres" >&2; exit 1; }
    # htpasswd lee la clave por stdin (-i): no aparece en la lista de procesos. Spring acepta el $2y$ de bcrypt.
    echo "{bcrypt}$(printf '%s' "$a" | htpasswd -niBC 10 x | cut -d: -f2)"
}
ENCARGADO=$(clave encargado@cine.uade.ar)
PUERTA=$(clave puerta@cine.uade.ar)

mysql() {
    docker run --rm -i -e MYSQL_PWD mysql:8.4 \
        mysql -h "$HOST" -P "$PUERTO" -u "$USUARIO" --ssl-mode=REQUIRED defaultdb "$@"
}

if [ "$(mysql -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='defaultdb'")" != 0 ]; then
    echo "defaultdb ya tiene tablas: no piso nada. Para empezar de cero, borralas a mano." >&2
    exit 1
fi

echo "Cargando schema.sql..."
mysql < cine-backend/src/main/resources/schema.sql
echo "Cargando empleados..."
mysql <<SQL
INSERT INTO usuario (nombre, email, rol, password_hash) VALUES
    ('Encargado', 'encargado@cine.uade.ar', 'ADMINISTRADOR', '$ENCARGADO'),
    ('Acomodador', 'puerta@cine.uade.ar', 'ACOMODADOR', '$PUERTA');
SQL
mysql -t -e "SELECT COUNT(*) AS tablas FROM information_schema.tables WHERE table_schema='defaultdb';
             SELECT email, rol, LEFT(password_hash, 12) AS hash FROM usuario;"
echo "Listo. Siguiente: crear el servicio en Render y cargar las variables en su panel."
