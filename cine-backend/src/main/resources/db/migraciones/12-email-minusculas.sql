-- Normaliza emails a minusculas y recortados, como los guarda y busca Usuario
-- Si el UNIQUE frena el UPDATE hay emails que difieren solo en mayusculas: resolver a mano

UPDATE usuario SET email = LOWER(TRIM(email));
