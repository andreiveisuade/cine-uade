import { get, post } from "./http/clienteHttp.js";
import { consulta, ruta } from "./http/rutas.js";

export const obtenerFuncion = (id, sesion) => get(ruta`/funciones/${id}` + consulta({ sesion }));

export const bloquearButacas = ({ funcionId, sesion, butacas }) =>
  post(ruta`/funciones/${funcionId}/bloqueos`, { sesion, butacas });
