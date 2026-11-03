import { get } from "./http/clienteHttp.js";

export const obtenerGeneros = () => get("/generos");

export const obtenerTarifas = () => get("/tarifas");
