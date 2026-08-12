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

-- butacas_por_fila guarda la distribución completa ("8,10,12,12,14"):
-- se lee siempre entera y nunca se consulta por partes.
CREATE TABLE IF NOT EXISTS sala (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    butacas_por_fila VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS asiento (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sala_id INT NOT NULL,
    fila INT NOT NULL,
    numero INT NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    UNIQUE (sala_id, fila, numero),
    FOREIGN KEY (sala_id) REFERENCES sala(id) ON DELETE CASCADE
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
    idioma VARCHAR(15) NOT NULL,
    proyeccion VARCHAR(10) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id),
    FOREIGN KEY (sala_id) REFERENCES sala(id)
);

CREATE TABLE IF NOT EXISTS reserva (
    id INT PRIMARY KEY AUTO_INCREMENT,
    funcion_id INT NOT NULL,
    cliente_id INT NOT NULL,
    estado VARCHAR(15) NOT NULL,
    FOREIGN KEY (funcion_id) REFERENCES funcion(id),
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

-- Una entrada por butaca, con lo que se cobro por ella: si manana cambia el precio
-- de la funcion, el ticket ya emitido sigue diciendo lo que se pago.
-- El UNIQUE impide vender dos veces el mismo asiento
-- en la misma reserva; que no se venda en otra reserva de la misma función lo
-- valida GestorReservas, porque depende del estado de la reserva.
CREATE TABLE IF NOT EXISTS entrada (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reserva_id INT NOT NULL,
    asiento_id INT NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    UNIQUE (reserva_id, asiento_id),
    FOREIGN KEY (reserva_id) REFERENCES reserva(id) ON DELETE CASCADE,
    FOREIGN KEY (asiento_id) REFERENCES asiento(id)
);
