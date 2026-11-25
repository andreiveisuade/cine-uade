-- Resenas publicas de los clientes sobre el cine, con puntaje de 1 a 5
-- Antes del deploy: el codigo viejo no mira la tabla y el nuevo no arranca sin ella

CREATE TABLE IF NOT EXISTS comentario (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cliente_id INT NOT NULL,
    puntaje INT NOT NULL,
    texto VARCHAR(1000) NOT NULL,
    fecha DATETIME NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES usuario(id)
);
