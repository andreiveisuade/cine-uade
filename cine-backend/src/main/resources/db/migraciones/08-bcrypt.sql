-- password_hash a 100 caracteres: bcrypt con prefijo {bcrypt} mide 68 y SHA-256 hexa solo 64
-- Aplicar antes de levantar la version nueva; el login reescribe los SHA-256 viejos en bcrypt

ALTER TABLE usuario
    MODIFY password_hash VARCHAR(100) NULL;
