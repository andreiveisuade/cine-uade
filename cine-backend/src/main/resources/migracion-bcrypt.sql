-- Contraseñas en bcrypt.
--
-- Un solo cambio sobre `usuario`, no destructivo: `password_hash` pasa de 64 a 100
-- caracteres. Un SHA-256 en hexa son 64 justos; un hash bcrypt con el prefijo que le pone
-- Spring Security ({bcrypt}$2a$10$...) mide 68, y no entraria.
--
-- Los hashes que ya estan quedan como estan: son SHA-256 sin prefijo, el login los sigue
-- aceptando y los reescribe en bcrypt la primera vez que el empleado entra. Por eso esto
-- tiene que estar aplicado antes de levantar la version nueva: si no, ese primer login
-- falla al guardar. Aplicar sobre una base ya creada; en una base nueva esto ya viene en
-- schema.sql.

ALTER TABLE usuario
    MODIFY password_hash VARCHAR(100) NULL;
