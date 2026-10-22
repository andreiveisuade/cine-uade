-- Para una base ya creada: schema.sql solo corre con el volumen vacio
-- Correrlo dos veces falla en el ALTER, que no admite IF NOT EXISTS

CREATE TABLE IF NOT EXISTS programacion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pelicula_id INT NOT NULL,
    sala_id INT NOT NULL,
    desde DATE NOT NULL,
    hasta DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    version VARCHAR(15) NOT NULL,
    proyeccion VARCHAR(10) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id),
    FOREIGN KEY (sala_id) REFERENCES sala(id)
);

CREATE TABLE IF NOT EXISTS programacion_dia (
    programacion_id INT NOT NULL,
    dia VARCHAR(10) NOT NULL,
    PRIMARY KEY (programacion_id, dia),
    FOREIGN KEY (programacion_id) REFERENCES programacion(id) ON DELETE CASCADE
);

-- NULL = funcion cargada a mano
ALTER TABLE funcion
    ADD COLUMN programacion_id INT NULL AFTER sala_id,
    ADD FOREIGN KEY (programacion_id) REFERENCES programacion(id);
