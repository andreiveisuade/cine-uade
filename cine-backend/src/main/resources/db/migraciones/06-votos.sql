-- Cantidad de votos detras del puntaje: un 8,0 sobre seis votos no vale lo que sobre cinco mil

ALTER TABLE pelicula
    ADD COLUMN votos INT NOT NULL DEFAULT 0;
