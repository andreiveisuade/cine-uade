-- Migracion para una base YA CREADA, que conserva los datos.
--
-- Hace falta porque schema.sql se monta en /docker-entrypoint-initdb.d/ y MySQL lo corre
-- una sola vez: cuando el volumen esta vacio. Sobre una base que ya existe, las tablas
-- nuevas de programacion no aparecen y la columna funcion.programacion_id tampoco, asi
-- que el backend responde 500 en todo lo que toca funcion.
--
-- Quien clona el proyecto hoy no necesita esto: su volumen nace vacio y schema.sql corre
-- entero. Esto es para el que ya tenia la base con reservas y pagos cargados, donde
-- `docker compose down -v` seria tirar los datos para agregar una columna.
--
--     docker exec -i cine-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" appsinteractivas \
--       < src/main/resources/migracion-programaciones.sql
--
-- schema.sql sigue siendo la fuente de verdad de la forma de las tablas; esto es solo el
-- camino para llegar ahi sin recrear la base. Correrlo dos veces falla en el ALTER, que
-- no admite IF NOT EXISTS: si ya paso una vez, no hay nada que hacer.

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

-- NULL es el valor correcto para todo lo que ya estaba: son las funciones que el
-- administrador cargo a mano, que es exactamente lo que significa la columna en null.
ALTER TABLE funcion
    ADD COLUMN programacion_id INT NULL AFTER sala_id,
    ADD FOREIGN KEY (programacion_id) REFERENCES programacion(id);
