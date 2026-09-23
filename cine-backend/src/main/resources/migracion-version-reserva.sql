-- El bloqueo optimista de la reserva.
--
-- Un solo cambio sobre `reserva`, no destructivo: aparece `version`, que JPA incrementa en
-- cada UPDATE (@Version en Reserva). Cobrar, cancelar y expirar leen el estado y lo
-- escriben; sin la version, dos de ellas a la vez pasaban el chequeo las dos y ganaba la
-- ultima en escribir, y podia quedar una reserva CANCELADA con un pago registrado.
--
-- DEFAULT 0 para lo ya cargado: es el valor con el que arranca cualquier reserva nueva.

ALTER TABLE reserva
    ADD COLUMN version INT NOT NULL DEFAULT 0;
