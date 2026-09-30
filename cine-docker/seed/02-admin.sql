-- Un administrador y un acomodador para poder entrar a la app de escritorio apenas se
-- levanta el sistema: el acomodador existe para probar Puerta con el rol que la usa de
-- verdad, que no ve el resto del panel. Corre después del schema, y solo la primera vez: docker-entrypoint-initdb.d
-- se ejecuta únicamente cuando el volumen está vacío.
--
-- No hay endpoint de alta de administradores —el TP no modela quién los crea— así que
-- el primero tiene que venir sembrado desde acá.
--
-- La contraseña es cine2026, guardada como su SHA-256 sin prefijo: el formato de antes de
-- bcrypt, que Spring Security sigue aceptando y reemplaza por {bcrypt} en el primer login.
-- Son credenciales de demo de un TP: para cualquier otra cosa, cambiarlas antes de levantar.

INSERT INTO usuario (nombre, email, rol, password_hash) VALUES
    ('Encargado', 'encargado@cine.uade.ar', 'ADMINISTRADOR',
     'e502da39b133e465645bc74c757b40f2d5ecd62681ee5c9b63fb1a4cdfb41b53'),
    ('Acomodador', 'puerta@cine.uade.ar', 'ACOMODADOR',
     'e502da39b133e465645bc74c757b40f2d5ecd62681ee5c9b63fb1a4cdfb41b53');
