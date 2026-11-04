import { get } from "./http/clienteHttp.js";
import { consulta, ruta } from "./http/rutas.js";

export const obtenerCartelera = (genero, tipo) => get("/cartelera" + consulta({ genero, tipo }));

export const obtenerPelicula = (id) => get(ruta`/peliculas/${id}`);

export const obtenerFuncionesDePelicula = (peliculaId) => get(ruta`/peliculas/${peliculaId}/funciones`);
