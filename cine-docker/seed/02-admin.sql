-- Un administrador y un acomodador para poder entrar a la app de escritorio apenas se
-- levanta el sistema: el acomodador existe para probar Puerta con el rol que la usa de
-- verdad, que no ve el resto del panel. Corre después del schema, y solo la primera vez: docker-entrypoint-initdb.d
-- se ejecuta únicamente cuando el volumen está vacío.
--
-- No hay endpoint de alta de administradores —el TP no modela quién los crea— así que
-- el primero tiene que venir sembrado desde acá.
--
-- La contraseña es cine2026, guardada en bcrypt con el prefijo {bcrypt} que pide el PasswordEncoder.
-- Son credenciales de demo de un TP: para cualquier otra cosa, cambiarlas antes de levantar.

INSERT INTO usuario (nombre, email, rol, password_hash) VALUES
    ('Encargado', 'encargado@cine.uade.ar', 'ADMINISTRADOR',
     '{bcrypt}$2y$10$8i462loUwlEda/Y6fd0jGuIXRhqVWuSfbxWInx/S/Wzto7YTScsjO'),
    ('Acomodador', 'puerta@cine.uade.ar', 'ACOMODADOR',
     '{bcrypt}$2y$10$8i462loUwlEda/Y6fd0jGuIXRhqVWuSfbxWInx/S/Wzto7YTScsjO');
