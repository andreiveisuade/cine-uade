-- Buzon de importadas: PENDIENTE, CONFIRMADA o DESCARTADA, eje distinto de `en_cartelera`
-- DEFAULT 'CONFIRMADA' deja lo ya cargado en el catalogo; las descartadas no se borran para que el importador no las traiga de nuevo

ALTER TABLE pelicula
    ADD COLUMN estado_revision VARCHAR(15) NOT NULL DEFAULT 'CONFIRMADA';
