import { borrar, put } from "./http/clienteHttp.js";

export const modificarCuenta = ({ nombre }) => put("/cuenta", { nombre });

export const darDeBaja = () => borrar("/cuenta");
