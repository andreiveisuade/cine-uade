-- Emails en minusculas y sin espacios en las puntas.
--
-- Un cambio sobre los datos de `usuario`, no sobre la tabla. Usuario ahora guarda el email
-- recortado y en minusculas, y asi lo buscan el login, la busqueda de clientes y las
-- reservas por email. Uno guardado antes con mayusculas ("Ana@Mail.com") ya no lo
-- encontraria nadie: el cliente pareceria nuevo y el empleado no podria entrar.
--
-- Si dos filas difieren solo en mayusculas, el UNIQUE de email frena el UPDATE entero y
-- no cambia nada. Se ven con:
--   SELECT LOWER(TRIM(email)), COUNT(*) FROM usuario GROUP BY 1 HAVING COUNT(*) > 1;
-- y hay que decidir a mano con cual quedarse antes de volver a aplicar esto.
-- Aplicar sobre una base ya creada; en una base nueva no hace falta.

UPDATE usuario SET email = LOWER(TRIM(email));
