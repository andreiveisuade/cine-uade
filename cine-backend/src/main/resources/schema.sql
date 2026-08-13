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

-- Clientes y empleados viven en la misma tabla: comparten nombre y email.
-- La columna rol discrimina cual es cual (CLIENTE, ADMINISTRADOR, ACOMODADOR) y
-- password_hash solo lo usan los empleados: el cliente compra sin iniciar sesion,
-- por eso admite NULL. El corte de la herencia no es el cargo sino tener contrasena,
-- y por eso el acomodador no necesita ni una columna ni una tabla nueva: es un rol mas.
CREATE TABLE IF NOT EXISTS usuario (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    rol VARCHAR(15) NOT NULL,
    password_hash VARCHAR(64) NULL
);

-- La grilla con la que un cine arma su cartelera: "Matrix en la Sala 1, todos los dias a
-- las 20:30, del 1 al 15 de septiembre". Una sola alta en vez de quince.
--
-- Guarda el patron temporal (desde/hasta + hora_inicio + los dias de programacion_dia) y
-- todo lo que cada funcion necesita para nacer: version, proyeccion y precio. No guarda
-- cuantas funciones genero: eso se cuenta desde funcion.programacion_id, que es la unica
-- fuente de verdad. Es el mismo criterio que sala, que no guarda su capacidad.
--
-- activa reemplaza al borrado, igual que promocion.activa: dar de baja una grilla evita
-- que genere funciones nuevas, pero las ya generadas quedan — pueden tener entradas
-- vendidas, y en este sistema nada que haya producido ventas se borra.
--
-- hasta NULL = grilla abierta: corre hasta que la den de baja, que es lo normal en un
-- cine. Es lo que le da sentido a activa: si toda grilla tuviera fin y generara su rango
-- entero al darse de alta, dar de baja no evitaria ninguna funcion.
--
-- generada_hasta es lo unico de la tabla que no describe la intencion sino lo que ya
-- paso: hasta que fecha se materializaron las funciones. Se guarda en vez de derivarse
-- de funcion.programacion_id porque una funcion se puede cancelar o mover de sala, y
-- entonces la cuenta daria de menos y se volverian a generar fechas ya procesadas.
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

-- Que dias de la semana corre la grilla, con el nombre de la constante de DayOfWeek. Va
-- en tabla aparte por lo mismo que promocion_dia y pelicula_genero: una columna con
-- "MONDAY,WEDNESDAY" adentro no se puede consultar ni indexar.
-- Sin filas = todos los dias del rango.
CREATE TABLE IF NOT EXISTS programacion_dia (
    programacion_id INT NOT NULL,
    dia VARCHAR(10) NOT NULL,
    PRIMARY KEY (programacion_id, dia),
    FOREIGN KEY (programacion_id) REFERENCES programacion(id) ON DELETE CASCADE
);

-- version es DOBLADA o SUBTITULADA: cómo se escucha esta copia. No es el idioma hablado
-- de la película, que es un dato de catálogo suyo.
--
-- programacion_id dice de que grilla salio la funcion, y admite NULL porque la grilla no
-- reemplaza a CU-03: la funcion suelta que carga el administrador —el preestreno, la
-- funcion especial— no pertenece a ninguna. Es lo que hace que la relacion sea 0..1 y no
-- 1, y por lo que la asociacion es asociacion y no composicion: la funcion no se va con
-- la grilla, sobrevive a su baja con sus reservas encima.
CREATE TABLE IF NOT EXISTS funcion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pelicula_id INT NOT NULL,
    sala_id INT NOT NULL,
    programacion_id INT NULL,
    inicio DATETIME NOT NULL,
    version VARCHAR(15) NOT NULL,
    proyeccion VARCHAR(10) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (pelicula_id) REFERENCES pelicula(id),
    FOREIGN KEY (sala_id) REFERENCES sala(id),
    FOREIGN KEY (programacion_id) REFERENCES programacion(id)
);

-- estado: RESERVADA, PAGADA, CANCELADA o EXPIRADA. La ultima no la escribe ningun
-- proceso de fondo: la primera consulta que se cruza con una reserva vencida la cierra
-- al pasar y libera sus butacas, porque el UNIQUE de entrada no sabe de vencimientos.
--
-- codigo es el que va en el QR y se valida en la puerta. Como el cliente no inicia
-- sesion, es la unica credencial que existe en el sistema: por eso no puede ser el id,
-- o con la reserva 5 en la mano se imprime la 6.
--
-- ingresada_en en NULL significa que todavia no entraron. Va como fecha y no como un
-- estado mas porque el ingreso es otro eje: una reserva puede estar PAGADA y no usada,
-- y mezclarlos obligaria a revisar las reglas que hoy miran estado (R5, R13).
CREATE TABLE IF NOT EXISTS reserva (
    id INT PRIMARY KEY AUTO_INCREMENT,
    funcion_id INT NOT NULL,
    cliente_id INT NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creada_en DATETIME NOT NULL,
    codigo VARCHAR(32) NOT NULL UNIQUE,
    ingresada_en DATETIME NULL,
    FOREIGN KEY (funcion_id) REFERENCES funcion(id),
    FOREIGN KEY (cliente_id) REFERENCES usuario(id)
);

-- Una entrada por butaca, con lo que se cobro por ella: si manana cambia el precio
-- de la funcion, el ticket ya emitido sigue diciendo lo que se pago.
--
-- tarifa es quien compra (GENERAL, MENOR, JUBILADO, ESTUDIANTE) y va aca y no en la
-- reserva porque es por persona: en una reserva de cuatro puede haber dos generales,
-- un menor y un jubilado. Ya esta incluida en precio, que es el precio de LISTA:
-- funcion x tipo de sala x tipo de butaca x tarifa. Ninguna promocion lo toca — el
-- descuento se aplica sobre el total y vive en pago.
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
    tarifa VARCHAR(15) NOT NULL DEFAULT 'GENERAL',
    precio DECIMAL(10,2) NOT NULL,
    UNIQUE (funcion_id, asiento_id),
    UNIQUE (reserva_id, asiento_id),
    FOREIGN KEY (reserva_id) REFERENCES reserva(id) ON DELETE CASCADE,
    FOREIGN KEY (asiento_id) REFERENCES asiento(id),
    FOREIGN KEY (funcion_id) REFERENCES funcion(id)
);

-- ---------- promociones ----------

-- Las tres clases de promocion van a la misma tabla con tipo como discriminador, igual
-- que usuario: comparten todas las condiciones y solo cambia como calculan el descuento.
-- Por eso las columnas del beneficio son NULL: cada tipo usa la suya y deja las otras
-- vacias. Con tres filas por promocion en tablas separadas se ganaba normalizacion y se
-- perdia poder leer una promocion entera de un SELECT.
--
--   PORCENTAJE  usa porcentaje      30.00 -> 30% del subtotal
--   MONTO_FIJO  usa monto           2000.00 de descuento
--   NXM         usa lleva y paga    2 y 1 -> el clasico 2x1
--
-- activa reemplaza al borrado, igual que producto.disponible: una promocion ya usada en
-- un cobro no se puede borrar (la FK de pago lo impide) porque el comprobante tiene que
-- seguir diciendo por que se cobro ese monto.
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
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

-- Que dias corre la promocion, con el nombre de la constante de DayOfWeek. Va en tabla
-- aparte y no como lista separada por comas por lo mismo que pelicula_genero: una
-- columna con "MONDAY,WEDNESDAY" adentro no se puede consultar ni indexar.
-- Sin filas = corre todos los dias.
CREATE TABLE IF NOT EXISTS promocion_dia (
    promocion_id INT NOT NULL,
    dia VARCHAR(10) NOT NULL,
    PRIMARY KEY (promocion_id, dia),
    FOREIGN KEY (promocion_id) REFERENCES promocion(id) ON DELETE CASCADE
);

-- Con que medios de pago aplica. Sin filas = con cualquiera. Es lo que hace que el
-- descuento del banco solo se pueda resolver al cobrar y no al reservar.
CREATE TABLE IF NOT EXISTS promocion_medio (
    promocion_id INT NOT NULL,
    medio VARCHAR(15) NOT NULL,
    PRIMARY KEY (promocion_id, medio),
    FOREIGN KEY (promocion_id) REFERENCES promocion(id) ON DELETE CASCADE
);

-- Comprobante de cobro de una reserva. Una reserva tiene a lo sumo un pago: el UNIQUE
-- lo garantiza desde la base, no solo desde el codigo.
--
-- Aca vive el descuento y no en entrada. El motivo es el 2x1: "cada dos butacas una
-- gratis" no se puede escribir como factor sobre una butaca. Y como las promociones no
-- se acumulan y gana la que mas descuenta (R15), todas tienen que producir la misma
-- unidad para poder compararse: un monto sobre el total de la reserva.
--
-- Las tres columnas cuentan la misma historia y por eso van las tres:
--   subtotal   la suma de los precios de lista de las entradas
--   descuento  cuanto saco la promocion que gano (0 si no aplico ninguna)
--   monto      lo que entro de verdad en la caja, y lo que suma el arqueo
-- Guardar la promocion ademas del descuento es lo que permite auditar meses despues por
-- que se cobro eso, aunque la promocion ya este desactivada.
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
--
-- Las dos claves admiten NULL porque la compra llega por dos caminos distintos:
--   mostrador   sin cliente ni reserva, que es como se compran unos pochoclos
--   web         despues de comprar la entrada, con el "desea agregar pochoclos?".
--               Ahi tiene reserva, y de la reserva sale el cliente sin volver a pedirlo.
-- Que la compra conozca su reserva permite retirar mostrando el mismo QR de la entrada,
-- y le da al arqueo cuanto candy vende el upsell contra el mostrador.
CREATE TABLE IF NOT EXISTS compra_candy (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cliente_id INT NULL,
    reserva_id INT NULL,
    fecha DATETIME NOT NULL,
    medio VARCHAR(15) NOT NULL,
    codigo_autorizacion VARCHAR(50) NOT NULL DEFAULT '',
    FOREIGN KEY (cliente_id) REFERENCES usuario(id),
    FOREIGN KEY (reserva_id) REFERENCES reserva(id)
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
