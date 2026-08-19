-- La grilla que rueda sola.
--
-- Dos cambios sobre `programacion`, los dos no destructivos:
--
--   1. `hasta` pasa a admitir NULL. Una grilla abierta corre hasta que la den de baja,
--      que es lo normal en un cine: la funcion de las 20:30 no tiene fecha de vencimiento.
--      Sin esto, `activa` no significaba nada: una grilla cerrada ya habia generado todo
--      su rango al darse de alta, asi que darla de baja no evitaba ninguna funcion.
--
--   2. Aparece `generada_hasta`: hasta que fecha se materializaron las funciones. Es lo
--      que hace que extender sea idempotente y que se pueda colgar de una lectura sin
--      pensar cuantas veces se lee.
--
-- Se guarda en vez de derivarse a proposito. Mirar la ultima funcion generada no sirve:
-- una funcion se puede cancelar o mover de sala, y entonces la cuenta daria de menos y se
-- volverian a generar fechas ya procesadas. Ademas las fechas que chocaron no generaron
-- ninguna funcion, y tambien estan procesadas.
--
-- Aplicar sobre una base ya creada:
--   docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" appsinteractivas' \
--     < src/main/resources/migracion-grilla-abierta.sql

ALTER TABLE programacion MODIFY hasta DATE NULL;

ALTER TABLE programacion ADD COLUMN generada_hasta DATE NULL AFTER activa;

-- Las grillas que ya existian generaron todo su rango de una sola vez al darse de alta,
-- asi que su fecha procesada es su propio `hasta`. Sin este backfill volverian a evaluar
-- fechas que ya tienen funciones y las listarian como choques contra si mismas.
UPDATE programacion SET generada_hasta = hasta WHERE generada_hasta IS NULL;
