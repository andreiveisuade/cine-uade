import { get } from "./http/clienteHttp.js";

export const obtenerProductosCandy = () => get("/candy/productos");

// CU-16: lo cobrado, con o sin entrada; lo pendiente se ve en su reserva.
export const obtenerMisComprasDeCandy = () => get("/candy/compras/mias");
