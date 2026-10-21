-- Foto opcional de promociones y productos: la URL de una imagen subida por POST /api/imagenes
-- NULL es sin imagen, como queda toda fila previa

ALTER TABLE promocion ADD COLUMN imagen_url VARCHAR(255) NULL;
ALTER TABLE producto ADD COLUMN imagen_url VARCHAR(255) NULL;
