-- En que formatos se estrena cada pelicula (R31): las que ya estaban, en 2D
-- y ademas en 3D las que ya tienen funciones o grillas en 3D, asi R31 se sigue cumpliendo

CREATE TABLE IF NOT EXISTS pelicula_formato (
    pelicula_id INT NOT NULL,
    formato VARCHAR(10) NOT NULL,
    PRIMARY KEY (pelicula_id, formato),
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id) ON DELETE CASCADE
);

INSERT IGNORE INTO pelicula_formato (pelicula_id, formato)
    SELECT id, 'DOS_D' FROM pelicula;

INSERT IGNORE INTO pelicula_formato (pelicula_id, formato)
    SELECT DISTINCT pelicula_id, 'TRES_D' FROM funcion WHERE proyeccion = 'TRES_D'
    UNION
    SELECT DISTINCT pelicula_id, 'TRES_D' FROM programacion WHERE proyeccion = 'TRES_D';
