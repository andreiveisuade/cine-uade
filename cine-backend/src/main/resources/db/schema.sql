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
    en_cartelera BOOLEAN NOT NULL DEFAULT TRUE,
    -- PENDIENTE, CONFIRMADA o DESCARTADA; lo importado nace PENDIENTE, lo cargado a mano CONFIRMADA
    estado_revision VARCHAR(15) NOT NULL DEFAULT 'CONFIRMADA',
    -- Valoracion de 0 a 10 (vote_average de TMDB); con ella ordena el planificador
    puntaje DECIMAL(3,1) NOT NULL DEFAULT 0,
    -- Votos detras del puntaje: un 8,0 sobre seis votos no vale lo que sobre cinco mil
    votos INT NOT NULL DEFAULT 0,
    -- Id del video de YouTube del trailer (11 caracteres); NULL si no tiene
    trailer_youtube VARCHAR(20) NULL
);

-- Una película tiene varios géneros: tabla aparte con la relación.
-- El género se guarda como texto con el nombre de la constante del enum Genero.
CREATE TABLE IF NOT EXISTS pelicula_genero (
    pelicula_id INT NOT NULL,
    genero VARCHAR(30) NOT NULL,
    PRIMARY KEY (pelicula_id, genero),
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id) ON DELETE CASCADE
);

-- La distribucion de butacas la dice la tabla asiento, no la sala
CREATE TABLE IF NOT EXISTS sala (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    -- Minutos entre funciones para levantar la sala; por sala porque no todas tardan lo mismo
    minutos_limpieza INT NOT NULL DEFAULT 15
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

-- Clientes y empleados en una tabla, discriminados por rol; password_hash solo del empleado: el cliente entra con Google (R28)
-- 100 y no 64: un hash bcrypt con prefijo mide 68
CREATE TABLE IF NOT EXISTS usuario (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    rol VARCHAR(15) NOT NULL,
    password_hash VARCHAR(100) NULL
);

-- Grilla de funciones: patron temporal mas version, proyeccion y precio con los que nace cada funcion
-- No cuenta las funciones generadas: salen de funcion.programacion_id; activa reemplaza al borrado
-- hasta NULL = grilla abierta; generada_hasta se guarda porque derivarlo regeneraria fechas si se cancela o mueve una funcion
CREATE TABLE IF NOT EXISTS programacion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pelicula_id INT NOT NULL,
    sala_id INT NOT NULL,
    desde DATE NOT NULL,
    hasta DATE NULL,
    hora_inicio TIME NOT NULL,
    version VARCHAR(15) NOT NULL,
    proyeccion VARCHAR(10) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    generada_hasta DATE NULL,
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id),
    FOREIGN KEY (sala_id) REFERENCES sala(id)
);

-- Dias de la grilla (constante de DayOfWeek) en tabla aparte, como pelicula_genero; sin filas = todos
CREATE TABLE IF NOT EXISTS programacion_dia (
    programacion_id INT NOT NULL,
    dia VARCHAR(10) NOT NULL,
    PRIMARY KEY (programacion_id, dia),
    FOREIGN KEY (programacion_id) REFERENCES programacion(id) ON DELETE CASCADE
);

-- version es DOBLADA o SUBTITULADA, no el idioma de la pelicula
-- programacion_id admite NULL: la funcion suelta sobrevive a la baja de su grilla
-- UNIQUE (sala_id, inicio): R3 se mira antes de guardar; dos pedidos a la vez la pasan juntos
CREATE TABLE IF NOT EXISTS funcion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pelicula_id INT NOT NULL,
    sala_id INT NOT NULL,
    programacion_id INT NULL,
    inicio DATETIME NOT NULL,
    version VARCHAR(15) NOT NULL,
    proyeccion VARCHAR(10) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    UNIQUE (sala_id, inicio),
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id),
    FOREIGN KEY (sala_id) REFERENCES sala(id),
    FOREIGN KEY (programacion_id) REFERENCES programacion(id)
);

-- estado: RESERVADA, PAGADA, CANCELADA o EXPIRADA; la ultima la cierra la primera consulta que ve la reserva vencida
-- codigo es el del QR y la unica credencial del cliente: no puede ser el id
-- ingresada_en NULL = no entro; es fecha y no estado para no mezclar ejes con R5 y R13
-- version: bloqueo optimista de JPA (@Version) contra cobrar y cancelar a la vez
CREATE TABLE IF NOT EXISTS reserva (
    id INT PRIMARY KEY AUTO_INCREMENT,
    funcion_id INT NOT NULL,
    cliente_id INT NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creada_en DATETIME NOT NULL,
    codigo VARCHAR(32) NOT NULL UNIQUE,
    ingresada_en DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    FOREIGN KEY (funcion_id) REFERENCES funcion(id),
    FOREIGN KEY (cliente_id) REFERENCES usuario(id)
);

-- Una entrada por butaca con el precio cobrado, congelado aunque cambie el de la funcion
-- tarifa va aca y no en reserva porque es por persona; precio es el de lista, el descuento vive en pago
-- funcion_id (sale de la reserva) existe para el UNIQUE contra la doble venta de una butaca
-- NULL al cancelar la reserva: MySQL no aplica el UNIQUE a NULL y la entrada conserva su historia
CREATE TABLE IF NOT EXISTS entrada (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reserva_id INT NOT NULL,
    asiento_id INT NOT NULL,
    funcion_id INT NULL,
    tarifa VARCHAR(15) NOT NULL DEFAULT 'GENERAL',
    precio DECIMAL(10,2) NOT NULL,
    UNIQUE (funcion_id, asiento_id),
    UNIQUE (reserva_id, asiento_id),
    FOREIGN KEY (reserva_id) REFERENCES reserva(id) ON DELETE CASCADE,
    FOREIGN KEY (asiento_id) REFERENCES asiento(id),
    FOREIGN KEY (funcion_id) REFERENCES funcion(id)
);

-- Butaca apartada tres minutos mientras se elige; la garantia contra la doble venta es el UNIQUE de entrada
-- La PK (funcion_id, asiento_id) arbitra entre sesiones sin lock explicito; una tarea borra las vencidas
CREATE TABLE IF NOT EXISTS bloqueo_butaca (
    funcion_id INT NOT NULL,
    asiento_id INT NOT NULL,
    sesion VARCHAR(64) NOT NULL,
    vence_en DATETIME NOT NULL,
    PRIMARY KEY (funcion_id, asiento_id),
    FOREIGN KEY (funcion_id) REFERENCES funcion(id) ON DELETE CASCADE,
    FOREIGN KEY (asiento_id) REFERENCES asiento(id) ON DELETE CASCADE
);

-- Las tres clases de promocion en una tabla con tipo como discriminador, como usuario
--   PORCENTAJE usa porcentaje, MONTO_FIJO usa monto, NXM usa lleva y paga (2 y 1 = 2x1)
-- activa reemplaza al borrado: la FK de pago impide borrar una promocion ya cobrada
CREATE TABLE IF NOT EXISTS promocion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    tipo VARCHAR(15) NOT NULL,
    porcentaje DECIMAL(5,2) NULL,
    monto DECIMAL(10,2) NULL,
    lleva INT NULL,
    paga INT NULL,
    vigencia_desde DATE NOT NULL,
    vigencia_hasta DATE NOT NULL,
    hora_desde TIME NULL,
    hora_hasta TIME NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    imagen_url VARCHAR(255) NULL
);

-- Dias que corre la promocion (constante de DayOfWeek); sin filas = todos
CREATE TABLE IF NOT EXISTS promocion_dia (
    promocion_id INT NOT NULL,
    dia VARCHAR(10) NOT NULL,
    PRIMARY KEY (promocion_id, dia),
    FOREIGN KEY (promocion_id) REFERENCES promocion(id) ON DELETE CASCADE
);

-- Medios de pago con los que aplica; sin filas = cualquiera
CREATE TABLE IF NOT EXISTS promocion_medio (
    promocion_id INT NOT NULL,
    medio VARCHAR(15) NOT NULL,
    PRIMARY KEY (promocion_id, medio),
    FOREIGN KEY (promocion_id) REFERENCES promocion(id) ON DELETE CASCADE
);

-- Comprobante de cobro; UNIQUE garantiza un pago por reserva
-- El descuento vive aca y no en entrada: el 2x1 no es un factor por butaca (R15 compara montos)
-- subtotal = lista, descuento = lo que saco la promocion ganadora, monto = lo que entro en caja
CREATE TABLE IF NOT EXISTS pago (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reserva_id INT NOT NULL UNIQUE,
    subtotal DECIMAL(10,2) NOT NULL,
    promocion_id INT NULL,
    descuento DECIMAL(10,2) NOT NULL DEFAULT 0,
    monto DECIMAL(10,2) NOT NULL,
    medio VARCHAR(15) NOT NULL,
    fecha DATETIME NOT NULL,
    codigo_autorizacion VARCHAR(50) NOT NULL DEFAULT '',
    FOREIGN KEY (reserva_id) REFERENCES reserva(id),
    FOREIGN KEY (promocion_id) REFERENCES promocion(id)
);

-- Un combo es un producto con precio propio; disponible reemplaza al borrado (hay compras emitidas)
CREATE TABLE IF NOT EXISTS producto (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    tipo VARCHAR(15) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    imagen_url VARCHAR(255) NULL
);

-- Contenido de cada combo; ambas claves apuntan a producto
-- R14 (combo mas barato que la suma) lo valida Producto: la base no compara filas
CREATE TABLE IF NOT EXISTS combo_item (
    combo_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    PRIMARY KEY (combo_id, producto_id),
    FOREIGN KEY (combo_id) REFERENCES producto(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES producto(id)
);

-- Venta del candy; no pasa por pago. La de mostrador nace COBRADA; la de la web (R30) nace PENDIENTE,
-- sin medio, y se cobra en el pago de su reserva (o queda ANULADA si la reserva se cancela o vence)
-- cliente y reserva admiten NULL: mostrador no tiene ninguno, la web los toma de la reserva
-- cobrada_en es la fecha del arqueo: el pedido de la web puede cobrarse otro dia que fecha
CREATE TABLE IF NOT EXISTS compra_candy (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cliente_id INT NULL,
    reserva_id INT NULL,
    fecha DATETIME NOT NULL,
    medio VARCHAR(15) NULL,
    codigo_autorizacion VARCHAR(50) NOT NULL DEFAULT '',
    estado VARCHAR(15) NOT NULL,
    cobrada_en DATETIME NULL,
    FOREIGN KEY (cliente_id) REFERENCES usuario(id),
    FOREIGN KEY (reserva_id) REFERENCES reserva(id)
);

-- Nombre, precio y ahorro congelados en la linea, como en entrada; ahorro_unitario es 0 si no es combo
CREATE TABLE IF NOT EXISTS item_compra (
    id INT PRIMARY KEY AUTO_INCREMENT,
    compra_id INT NOT NULL,
    producto_id INT NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    ahorro_unitario DECIMAL(10,2) NOT NULL DEFAULT 0,
    UNIQUE (compra_id, producto_id),
    FOREIGN KEY (compra_id) REFERENCES compra_candy(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES producto(id)
);

-- Corridas del importador disparadas desde el panel; sin FK a pelicula, que ya guarda su estado_revision
-- `detalle` es el log o el motivo de la falla: texto para leer, no para consultar
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

-- Las fotos de promos y productos, con el nombre de su URL; en la base y no en disco para que un despliegue sin
-- disco persistente no las pierda. MEDIUMBLOB: hasta 16 MB, y Imagen corta en 2 MB
CREATE TABLE IF NOT EXISTS imagen (
    nombre VARCHAR(60) PRIMARY KEY,
    contenido MEDIUMBLOB NOT NULL
);
