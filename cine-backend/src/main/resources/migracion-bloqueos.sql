-- Los bloqueos de butaca mientras se elige, que antes vivian en Redis.
--
-- Una tabla nueva, sin tocar ninguna existente. Redis estaba solo para esto: guardar tres
-- minutos quien esta eligiendo cada butaca. Con la tabla, el bloqueo vive en la misma base
-- que las entradas —una sola fuente de verdad, y un contenedor menos que levantar,
-- monitorear y explicar— y la atomicidad la da la clave primaria en vez de un script Lua.
--
-- No hay nada que migrar: los bloqueos duran tres minutos, asi que lo que hubiera en Redis
-- al apagarlo ya vencio. Quien estaba eligiendo en ese momento vuelve a tocar el mapa.
--
-- Aplicar sobre una base ya creada; en una base nueva ya viene en schema.sql.
--
--     docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appsinteractivas \
--       < ../cine-backend/src/main/resources/migracion-bloqueos.sql

CREATE TABLE IF NOT EXISTS bloqueo_butaca (
    funcion_id INT NOT NULL,
    asiento_id INT NOT NULL,
    sesion VARCHAR(64) NOT NULL,
    vence_en DATETIME NOT NULL,
    PRIMARY KEY (funcion_id, asiento_id),
    FOREIGN KEY (funcion_id) REFERENCES funcion(id) ON DELETE CASCADE,
    FOREIGN KEY (asiento_id) REFERENCES asiento(id) ON DELETE CASCADE
);
