import { get, post } from "./http/clienteHttp.js";

export const obtenerComentarios = () => get("/comentarios");

export const publicarComentario = ({ puntaje, texto }) => post("/comentarios", { puntaje, texto });
