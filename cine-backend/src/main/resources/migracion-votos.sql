-- Sobre cuantos votos se calculo el puntaje.
--
-- Un solo cambio sobre `pelicula`, no destructivo. TMDB ya devolvia vote_count junto al
-- vote_average y el importador lo descartaba, asi que el planificador comparaba numeros
-- que no son comparables: un 8,0 sobre seis votos pesaba igual que un 8,0 sobre cinco
-- mil, y un 0,0 sobre cero votos —una pelicula recien estrenada que nadie voto todavia—
-- quedaba al fondo del ranking como si fuera mala.
--
-- Con los datos reales de la cartelera argentina el problema no es teorico: de veinte
-- titulos, siete no tienen ningun voto y cuatro tienen menos de cincuenta.
--
-- DEFAULT 0 para lo ya cargado: sin votos, el planificador trata el puntaje como lo que
-- es, un dato sin respaldo, y lo lleva hacia el promedio del catalogo.

ALTER TABLE pelicula
    ADD COLUMN votos INT NOT NULL DEFAULT 0;
