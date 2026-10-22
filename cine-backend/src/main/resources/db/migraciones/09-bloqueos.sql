-- Bloqueos de butaca en la base en vez de Redis; la atomicidad la da la clave primaria
-- Sobre base ya creada; en una nueva viene en schema.sql

CREATE TABLE IF NOT EXISTS bloqueo_butaca (
    funcion_id INT NOT NULL,
    asiento_id INT NOT NULL,
    sesion VARCHAR(64) NOT NULL,
    vence_en DATETIME NOT NULL,
    PRIMARY KEY (funcion_id, asiento_id),
    FOREIGN KEY (funcion_id) REFERENCES funcion(id) ON DELETE CASCADE,
    FOREIGN KEY (asiento_id) REFERENCES asiento(id) ON DELETE CASCADE
);
