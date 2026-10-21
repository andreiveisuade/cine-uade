-- Congela el ahorro por linea de candy; antes se recalculaba con el precio actual del combo
-- Lo ya cargado se completa con el ahorro de la carta actual

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
