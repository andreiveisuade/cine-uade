-- El ahorro de cada linea de candy, congelado al vender.
--
-- Un cambio sobre `item_compra`, no destructivo: aparece `ahorro_unitario`. Hasta
-- ahora el ahorro se recalculaba con el precio actual del combo, asi que editar un combo
-- cambiaba lo que decian haber ahorrado las compras viejas, aunque el nombre y el precio
-- de la linea si quedaban congelados.
--
-- Lo ya cargado se completa con el ahorro que muestra hoy, el de la carta actual: es lo
-- que venian diciendo esos tickets, y desde aca deja de moverse. Lo que no es combo queda
-- en 0, que es su ahorro.

ALTER TABLE item_compra
    ADD COLUMN ahorro_unitario DECIMAL(10,2) NOT NULL DEFAULT 0;

UPDATE item_compra ic
JOIN producto combo ON combo.id = ic.producto_id
SET ic.ahorro_unitario = (
        SELECT SUM(suelto.precio * ci.cantidad)
        FROM combo_item ci
        JOIN producto suelto ON suelto.id = ci.producto_id
        WHERE ci.combo_id = combo.id
    ) - combo.precio
WHERE combo.tipo = 'COMBO';
