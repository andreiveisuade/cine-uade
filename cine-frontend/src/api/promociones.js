import { get } from "./http/clienteHttp.js";

export const obtenerPromocionesVigentes = () => get("/promociones/vigentes");
