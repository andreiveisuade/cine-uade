CREATE TABLE IF NOT EXISTS pelicula (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titulo VARCHAR(100) NOT NULL,
    duracion_minutos INT NOT NULL
);

-- Una película tiene varios géneros: tabla aparte con la relación.
-- El género se guarda como texto con el nombre de la constante del enum Genero.
CREATE TABLE IF NOT EXISTS pelicula_genero (
    pelicula_id INT NOT NULL,
    genero VARCHAR(30) NOT NULL,
    PRIMARY KEY (pelicula_id, genero),
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id) ON DELETE CASCADE
);
