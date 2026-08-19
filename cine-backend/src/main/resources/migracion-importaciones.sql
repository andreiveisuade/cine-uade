-- El historial de corridas del importador.
--
-- Una tabla nueva, sin tocar ninguna existente. Aparece porque el importador dejo de
-- correr solo cada seis horas: ahora lo dispara el encargado desde el panel, y una
-- corrida que alguien pidio a mano es un hecho que hay que poder mirar despues. La
-- pregunta que responde —"cuando trajimos cartelera por ultima vez y cuanto entro"— no la
-- contesta ninguna otra tabla: `pelicula.estado_revision` dice que hay en el buzon, no de
-- que corrida vino ni cuando se pidio.
--
-- No tiene clave foranea a `pelicula` a proposito. Las peliculas que entraron ya estan
-- guardadas y son las que se ven en el buzon; anotar aca cuales fueron seria tener dos
-- versiones del mismo hecho, y con el tiempo se separan.
--
-- Los contadores nacen en cero y `termino_en` admite NULL: una corrida existe desde que se
-- la pide, y hasta que el importador vuelve no hay nada que contar. Una fila que se quedo
-- EN_CURSO —el backend se reinicio en el medio— la da por perdida el gestor cuando alguien
-- consulta el historial, sin proceso de fondo.
--
-- Aplicar sobre una base ya creada; en una base nueva ya viene en schema.sql.
--
--     docker compose exec -T mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" appsinteractivas \
--       < ../cine-backend/src/main/resources/migracion-importaciones.sql

CREATE TABLE IF NOT EXISTS importacion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    estado VARCHAR(15) NOT NULL,
    paginas INT NOT NULL DEFAULT 1,
    pedida_en DATETIME NOT NULL,
    termino_en DATETIME NULL,
    nuevas INT NOT NULL DEFAULT 0,
    salteadas INT NOT NULL DEFAULT 0,
    fallidas INT NOT NULL DEFAULT 0,
    detalle TEXT
);
