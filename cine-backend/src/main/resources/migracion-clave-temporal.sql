-- R27: la clave temporal que asigna la boleteria; la web le pide al cliente cambiarla al entrar

ALTER TABLE usuario
    ADD COLUMN debe_cambiar_clave BOOLEAN NOT NULL DEFAULT FALSE;
