-- Una sola funcion por sala e inicio: dos extensiones de grilla a la vez generaban funciones dobles
-- Antes, buscar duplicados; si hay, borrar a mano la que no tenga reservas
-- SELECT sala_id, inicio, COUNT(*) FROM funcion GROUP BY sala_id, inicio HAVING COUNT(*) > 1;

ALTER TABLE funcion ADD UNIQUE (sala_id, inicio);
