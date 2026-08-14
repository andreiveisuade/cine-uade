-- La limpieza entre funciones.
--
-- Un solo cambio sobre `sala`, no destructivo: aparece `minutos_limpieza`, cuanto hay
-- que dejar entre el final de una funcion y el comienzo de la siguiente para levantar
-- la sala.
--
-- Va por sala y no como constante del sistema porque no todas tardan lo mismo: una de
-- sesenta butacas se limpia mientras a la de doscientas todavia le estan saliendo. Que
-- sea configurable es lo que permite programar la sala chica mas apretada.
--
-- El DEFAULT 15 hace que las salas ya cargadas queden con un margen razonable en vez de
-- con cero, que las dejaria encadenando funciones sin corte. Aplicar sobre una base ya
-- creada; en una base nueva esto ya viene en schema.sql.

ALTER TABLE sala
    ADD COLUMN minutos_limpieza INT NOT NULL DEFAULT 15;
