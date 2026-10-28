-- R28: el cliente entra con Google; sin clave de cliente no hay clave temporal (R27) ni hashes que guardar
-- Después del deploy, no antes: el código nuevo no lee la columna y el viejo no arranca sin ella (ddl-auto validate)

ALTER TABLE usuario
    DROP COLUMN debe_cambiar_clave;

UPDATE usuario
    SET password_hash = NULL
    WHERE rol = 'CLIENTE';
