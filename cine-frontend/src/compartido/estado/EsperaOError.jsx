import { Cargando } from "./Cargando.jsx";
import { ErrorCaja } from "./ErrorCaja.jsx";

// `esqueleto` es la forma de la pantalla que viene: se ve dónde va a quedar cada cosa antes de que llegue.
export function EsperaOError({ carga, esqueleto = <Cargando /> }) {
  return carga.error ? <ErrorCaja>{carga.error}</ErrorCaja> : esqueleto;
}
