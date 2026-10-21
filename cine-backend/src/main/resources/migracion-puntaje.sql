-- Puntaje 0-10 (vote_average de TMDB) para que el planificador pueda rankear peliculas

ALTER TABLE pelicula
    ADD COLUMN puntaje DECIMAL(3,1) NOT NULL DEFAULT 0;
