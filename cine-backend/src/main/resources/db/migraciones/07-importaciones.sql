-- Historial de corridas del importador, que ahora dispara el encargado a mano
-- Sin FK a `pelicula` a proposito: las peliculas ya son la fuente de lo importado

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
