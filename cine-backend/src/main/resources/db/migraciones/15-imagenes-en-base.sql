-- Las imágenes pasan del volumen `imagenes` a la tabla `imagen`. Después de esto, las que ya estaban en el volumen
-- se cargan una vez con el bloque de abajo, desde cine-docker y con el backend nuevo ya construido.
CREATE TABLE IF NOT EXISTS imagen (
    nombre VARCHAR(60) PRIMARY KEY,
    contenido MEDIUMBLOB NOT NULL
);

-- docker run --rm -v cine-docker_imagenes:/v alpine sh -c 'cd /v && for f in *; do
--   echo "INSERT IGNORE INTO imagen VALUES (\"$f\", 0x$(od -An -v -tx1 "$f" | tr -d " \n"));"; done' \
--   | docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
