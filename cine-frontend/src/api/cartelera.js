import { get } from "./http/clienteHttp.js";
import { consulta, ruta } from "./http/rutas.js";

export const obtenerCartelera = (genero) => get("/cartelera" + consulta({ genero }));

export const obtenerPelicula = (id) => get(ruta`/peliculas/${id}`);

export const obtenerFuncionesDePelicula = (peliculaId) => get(ruta`/peliculas/${peliculaId}/funciones`);
