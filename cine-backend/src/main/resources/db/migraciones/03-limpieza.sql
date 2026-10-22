-- Minutos de limpieza por sala entre el fin de una funcion y el inicio de la siguiente
-- Por sala porque no todas tardan lo mismo; DEFAULT 15 evita encadenar funciones sin corte

ALTER TABLE sala
    ADD COLUMN minutos_limpieza INT NOT NULL DEFAULT 15;
