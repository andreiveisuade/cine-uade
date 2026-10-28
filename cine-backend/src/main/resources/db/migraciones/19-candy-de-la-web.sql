-- R30: el candy pedido en la web espera el pago de su reserva; lo ya vendido es de mostrador y quedo cobrado
-- medio admite NULL (lo pendiente no tiene) y el arqueo pasa a contar por cobrada_en
-- Antes del deploy: el DEFAULT deja vender al codigo viejo hasta que arranca el nuevo, que siempre manda estado

ALTER TABLE compra_candy
    MODIFY medio VARCHAR(15) NULL,
    ADD COLUMN estado VARCHAR(15) NOT NULL DEFAULT 'COBRADA',
    ADD COLUMN cobrada_en DATETIME NULL;

UPDATE compra_candy
    SET cobrada_en = fecha;
