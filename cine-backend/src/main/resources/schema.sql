CREATE TABLE IF NOT EXISTS pelicula (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titulo VARCHAR(100) NOT NULL,
    duracion_minutos INT NOT NULL,
    clasificacion VARCHAR(10) NOT NULL,
    -- datos de catalogo: para mostrar la pelicula, sin reglas asociadas
    director VARCHAR(100) NOT NULL DEFAULT '',
    sinopsis TEXT,
    anio INT NOT NULL DEFAULT 0,
    idioma_original VARCHAR(40) NOT NULL DEFAULT '',
    poster_url VARCHAR(255) NOT NULL DEFAULT '',
    en_cartelera BOOLEAN NOT NULL DEFAULT TRUE
);

-- Una película tiene varios géneros: tabla aparte con la relación.
-- El género se guarda como texto con el nombre de la constante del enum Genero.
CREATE TABLE IF NOT EXISTS pelicula_genero (
    pelicula_id INT NOT NULL,
    genero VARCHAR(30) NOT NULL,
    PRIMARY KEY (pelicula_id, genero),
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id) ON DELETE CASCADE
);

-- La sala no guarda su distribución de butacas: eso lo dice la tabla asiento, que es la
-- única fuente de verdad. Cuántas butacas por fila se usa una sola vez, al generarlas.
CREATE TABLE IF NOT EXISTS sala (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    tipo VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS asiento (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sala_id INT NOT NULL,
    fila INT NOT NULL,
    numero INT NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    -- estado fisico: habilitada o rota. Que esté ocupada depende de la función.
    estado VARCHAR(20) NOT NULL DEFAULT 'HABILITADO',
    UNIQUE (sala_id, fila, numero),
    FOREIGN KEY (sala_id) REFERENCES sala(id) ON DELETE CASCADE
);

-- Clientes y administradores viven en la misma tabla: comparten nombre y email.
-- La columna rol discrimina cual es cual. password_hash solo lo usa el administrador:
-- el cliente compra sin iniciar sesion, por eso admite NULL.
CREATE TABLE IF NOT EXISTS usuario (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    rol VARCHAR(15) NOT NULL,
    password_hash VARCHAR(64) NULL
);

-- version es DOBLADA o SUBTITULADA: cómo se escucha esta copia. No es el idioma hablado
-- de la película, que es un dato de catálogo suyo.
CREATE TABLE IF NOT EXISTS funcion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pelicula_id INT NOT NULL,
    sala_id INT NOT NULL,
    inicio DATETIME NOT NULL,
    version VARCHAR(15) NOT NULL,
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
    creada_en DATETIME NOT NULL,
    FOREIGN KEY (funcion_id) REFERENCES funcion(id),
    FOREIGN KEY (cliente_id) REFERENCES usuario(id)
);

-- Una entrada por butaca, con lo que se cobro por ella: si manana cambia el precio
-- de la funcion, el ticket ya emitido sigue diciendo lo que se pago.
--
-- funcion_id no es un dato nuevo (sale de la reserva): es lo que hace posible el
-- UNIQUE que impide vender la misma butaca dos veces en la misma funcion. Sin el,
-- dos reservas simultaneas podian tomar la misma butaca: las dos leian "libre"
-- antes de que la otra escribiera.
--
-- Es NULL cuando la reserva se cancela. MySQL no aplica el UNIQUE a las filas con
-- NULL, asi que la butaca vuelve a estar disponible sin borrar la entrada: la
-- reserva cancelada conserva que butacas tenia y a que precio.
CREATE TABLE IF NOT EXISTS entrada (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reserva_id INT NOT NULL,
    asiento_id INT NOT NULL,
    funcion_id INT NULL,
    precio DECIMAL(10,2) NOT NULL,
    UNIQUE (funcion_id, asiento_id),
    UNIQUE (reserva_id, asiento_id),
    FOREIGN KEY (reserva_id) REFERENCES reserva(id) ON DELETE CASCADE,
    FOREIGN KEY (asiento_id) REFERENCES asiento(id),
    FOREIGN KEY (funcion_id) REFERENCES funcion(id)
);

-- Comprobante de cobro de una reserva. Una reserva tiene a lo sumo un pago: el UNIQUE
-- lo garantiza desde la base, no solo desde el codigo.
CREATE TABLE IF NOT EXISTS pago (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reserva_id INT NOT NULL UNIQUE,
    monto DECIMAL(10,2) NOT NULL,
    medio VARCHAR(15) NOT NULL,
    fecha DATETIME NOT NULL,
    codigo_autorizacion VARCHAR(50) NOT NULL DEFAULT '',
    FOREIGN KEY (reserva_id) REFERENCES reserva(id)
);

-- ---------- candy ----------

-- Un combo es un producto mas: tiene precio propio y se vende como una unidad.
-- disponible reemplaza al borrado: el producto puede estar en compras ya emitidas.
CREATE TABLE IF NOT EXISTS producto (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    tipo VARCHAR(15) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE
);

-- Que trae cada combo. Las dos claves apuntan a producto: el combo y lo que contiene
-- son productos, y por eso un combo puede armarse con cualquiera de los otros.
-- Que el precio del combo sea menor a la suma de sus componentes lo valida GestorCandy:
-- es una comparacion entre filas distintas y la base no la puede expresar.
CREATE TABLE IF NOT EXISTS combo_item (
    combo_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    PRIMARY KEY (combo_id, producto_id),
    FOREIGN KEY (combo_id) REFERENCES producto(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES producto(id)
);

-- Venta de mostrador: no tiene estados porque se cobra en el acto, y por eso lleva
-- encima el medio de pago en vez de pasar por la tabla pago, que es de las reservas.
CREATE TABLE IF NOT EXISTS compra_candy (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cliente_id INT NOT NULL,
    fecha DATETIME NOT NULL,
    medio VARCHAR(15) NOT NULL,
    codigo_autorizacion VARCHAR(50) NOT NULL DEFAULT '',
    FOREIGN KEY (cliente_id) REFERENCES usuario(id)
);

-- El nombre y el precio quedan congelados en la linea, igual que en entrada: el ticket
-- emitido tiene que seguir diciendo lo que se cobro aunque despues cambie la carta.
CREATE TABLE IF NOT EXISTS item_compra (
    id INT PRIMARY KEY AUTO_INCREMENT,
    compra_id INT NOT NULL,
    producto_id INT NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    UNIQUE (compra_id, producto_id),
    FOREIGN KEY (compra_id) REFERENCES compra_candy(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES producto(id)
);
