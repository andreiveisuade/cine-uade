-- El candy se retira con el QR de la entrada: retirada_en NULL = todavia no se entrego
-- Antes del deploy: el codigo viejo no lee la columna y el nuevo no arranca sin ella

ALTER TABLE compra_candy
    ADD COLUMN retirada_en DATETIME NULL;
