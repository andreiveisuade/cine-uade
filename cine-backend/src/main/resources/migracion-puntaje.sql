-- La valoracion de cada pelicula.
--
-- Un solo cambio sobre `pelicula`, no destructivo: aparece `puntaje`, de 0 a 10. Es el
-- vote_average que TMDB ya devolvia y el importador venia descartando.
--
-- Existe para el planificador de la grilla: "programar las mejores" no se puede resolver
-- sin un numero que compare dos peliculas. La diversidad de generos y la ocupacion de las
-- salas salen de datos que ya estaban; el ranking no.
--
-- DEFAULT 0 para lo ya cargado: una pelicula sin puntaje no rompe nada, simplemente
-- queda al final del orden hasta que alguien la valore o el importador la actualice.

ALTER TABLE pelicula
    ADD COLUMN puntaje DECIMAL(3,1) NOT NULL DEFAULT 0;
