import { get } from "./http/clienteHttp.js";

export const obtenerGeneros = () => get("/generos");

export const obtenerTiposPelicula = () => get("/tipos-pelicula");

export const obtenerTarifas = () => get("/tarifas");
