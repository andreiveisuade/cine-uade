-- @Version de Reserva: sin ella cobrar y cancelar a la vez podian dejar CANCELADA con un pago

ALTER TABLE reserva
    ADD COLUMN version INT NOT NULL DEFAULT 0;
