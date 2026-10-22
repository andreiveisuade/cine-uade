-- Grilla abierta: `hasta` admite NULL y `generada_hasta` marca hasta donde se generaron funciones
-- Se guarda y no se deriva: una funcion cancelada o movida haria regenerar fechas ya procesadas

ALTER TABLE programacion MODIFY hasta DATE NULL;

ALTER TABLE programacion ADD COLUMN generada_hasta DATE NULL AFTER activa;

-- Backfill: las grillas viejas generaron todo su rango al darse de alta
UPDATE programacion SET generada_hasta = hasta WHERE generada_hasta IS NULL;
