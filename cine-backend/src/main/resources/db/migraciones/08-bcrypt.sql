-- password_hash a 100 caracteres: bcrypt con prefijo {bcrypt} mide 68 y SHA-256 hexa solo 64

ALTER TABLE usuario
    MODIFY password_hash VARCHAR(100) NULL;
