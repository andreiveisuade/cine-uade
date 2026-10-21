-- Id del video de YouTube del trailer; la proxima importacion lo completa en las que ya estan

ALTER TABLE pelicula
    ADD COLUMN trailer_youtube VARCHAR(20) NULL;
