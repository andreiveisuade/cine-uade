-- R35: la sala 4D proyecta siempre en 3D. La grilla vieja dejo funciones 2D en salas 4D y el catalogo casi solo en 2D
-- Borra todo lo programado y las ventas que cuelgan de eso, que son de prueba: NO SE PUEDE DESHACER, hace un dump antes
-- Quedan peliculas, salas, clientes, productos, promos y el candy de mostrador, que no tiene reserva
-- No se reinician los AUTO_INCREMENT: un ticket ya emitido no comparte numero con uno nuevo
-- Despues del deploy hay que volver a generar la grilla: Swing, Planificador, o seed/datos-de-ejemplo.sh

START TRANSACTION;

-- item_compra se va con su compra y entrada con su reserva, por ON DELETE CASCADE
DELETE FROM compra_candy WHERE reserva_id IS NOT NULL;
DELETE FROM pago;
DELETE FROM reserva;

-- bloqueo_butaca se va con su funcion. La funcion antes que la grilla: apunta a ella
DELETE FROM funcion;
-- programacion_dia se va con su grilla. Si quedara una activa, generaria funciones de nuevo
DELETE FROM programacion;

-- TMDB no informa el formato: se asigna 3D por genero, para que las salas 3D y 4D tengan que dar (R31)
INSERT IGNORE INTO pelicula_formato (pelicula_id, formato)
    SELECT DISTINCT pelicula_id, 'TRES_D' FROM pelicula_genero
    WHERE genero IN ('ACCION', 'CIENCIA_FICCION', 'ANIMACION');

COMMIT;

SELECT (SELECT COUNT(*) FROM funcion) AS funciones,
       (SELECT COUNT(*) FROM programacion) AS grillas,
       (SELECT COUNT(*) FROM reserva) AS reservas,
       (SELECT COUNT(*) FROM pelicula) AS peliculas,
       (SELECT COUNT(DISTINCT pelicula_id) FROM pelicula_formato WHERE formato = 'TRES_D') AS peliculas_en_3d;
