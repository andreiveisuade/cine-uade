import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/api/formato.js";
import { ESTILO, estiloTipo } from "@/compra/mapa/estilosButaca.js";

// El fondo dice el estado; el borde y el símbolo, el tipo de butaca.
export function pintarParaComprar(asiento, elegidas) {
  const titulo = `${asiento.codigo} · ${etiqueta(asiento.tipo)} · ${precio(asiento.precio)}`;
  if (asiento.estado === "FUERA_DE_SERVICIO") {
    return { estilo: ESTILO.fueraDeServicio, deshabilitado: true, titulo: `${asiento.codigo} · fuera de servicio` };
  }
  if (asiento.ocupado) return { estilo: ESTILO.ocupada, deshabilitado: true, titulo: `${asiento.codigo} · ocupada` };
  if (elegidas[asiento.codigo]) return { estilo: ESTILO.elegida, deshabilitado: false, titulo };
  return { estilo: estiloTipo(asiento.tipo), deshabilitado: false, titulo };
}

export const REFERENCIAS = [
  [estiloTipo("ESTANDAR"), "libre"],
  [ESTILO.elegida, "elegida"],
  [ESTILO.ocupada, "ocupada"],
  [ESTILO.fueraDeServicio, "fuera de servicio"],
  [estiloTipo("VIP"), "* VIP"],
  [estiloTipo("PAREJA"), "& pareja"],
  [estiloTipo("ACCESIBLE"), "+ accesible"],
];
