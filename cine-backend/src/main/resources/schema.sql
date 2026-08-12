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

CREATE TABLE IF NOT EXISTS sala (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    capacidad INT NOT NULL
);

CREATE TABLE IF NOT EXISTS cliente (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS funcion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pelicula_id INT NOT NULL,
    sala_id INT NOT NULL,
    inicio DATETIME NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id),
    FOREIGN KEY (sala_id) REFERENCES sala(id)
);

CREATE TABLE IF NOT EXISTS reserva (
    id INT PRIMARY KEY AUTO_INCREMENT,
    funcion_id INT NOT NULL,
    cliente_id INT NOT NULL,
    cantidad_entradas INT NOT NULL,
    estado VARCHAR(15) NOT NULL,
    FOREIGN KEY (funcion_id) REFERENCES funcion(id),
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);
