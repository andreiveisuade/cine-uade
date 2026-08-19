-- El buzon de peliculas importadas.
--
-- Un solo cambio sobre `pelicula`, no destructivo: aparece `estado_revision`, si el
-- encargado ya decidio que hacer con la pelicula. PENDIENTE es lo que trajo el importador
-- y nadie miro, CONFIRMADA lo que es parte del catalogo, DESCARTADA lo que se rechazo.
--
-- Existe porque el importador de TMDB trae la cartelera entera de Argentina y que todos
-- los titulos entren derecho al catalogo hace que el catalogo deje de ser una decision
-- del cine para pasar a ser una copia de lo que pasa afuera.
--
-- Es un eje distinto de `en_cartelera`, y por eso son dos columnas y no una: en_cartelera
-- es si se esta dando y cambia todas las semanas; estado_revision es si la pelicula entro
-- al catalogo, que se responde una sola vez. Con una sola columna, "todavia no la mire" y
-- "la saque de cartelera la semana pasada" serian indistinguibles, que es justo la
-- pantalla que hay que poder dibujar.
--
-- El DEFAULT 'CONFIRMADA' deja las peliculas ya cargadas como estan: se cargaron a mano o
-- se revisaron en su momento, y aparecer de golpe en el buzon de pendientes seria una
-- sorpresa. Aplicar sobre una base ya creada; en una base nueva ya viene en schema.sql.
--
-- Las descartadas no se borran a proposito: si se borraran, la proxima corrida del
-- importador las traeria de nuevo y habria que descartarlas otra vez, para siempre.

ALTER TABLE pelicula
    ADD COLUMN estado_revision VARCHAR(15) NOT NULL DEFAULT 'CONFIRMADA';
