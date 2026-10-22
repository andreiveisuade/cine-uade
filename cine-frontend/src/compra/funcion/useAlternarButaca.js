import { useLayoutEffect, useRef } from "react";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { sinButacas, sostenerSeleccion } from "@/compra/bloqueo.js";
import { useCompra } from "@/compra/CompraEnCurso.jsx";

// Tocar una butaca la suma o la suelta y le pide al backend que sostenga la selección entera.
export function useAlternarButaca(funcion, butacas, recargar) {
  const { setSeleccion } = useCompra();
  const avisar = useAvisar();
  // Dos clics antes de que React vuelva a pintar verían las mismas `butacas`: la vigente se lleva en un ref.
  const vigentes = useRef(butacas);
  // Los bloqueos salen de a uno: dos a la vez con la misma sesión el backend los toma en paralelo y rechaza la primera.
  const cola = useRef(Promise.resolve());
  useLayoutEffect(() => {
    vigentes.current = butacas;
  }, [butacas]);

  return function alternar(asiento) {
    const butacas = vigentes.current;
    // Arranca en GENERAL: una tarifa reducida hay que acreditarla en la puerta.
    const nuevas = butacas[asiento.codigo]
      ? sinButacas(butacas, [asiento.codigo])
      : { ...butacas, [asiento.codigo]: "GENERAL" };
    // Se pinta antes del bloqueo para que se vea elegida en el acto.
    vigentes.current = nuevas;
    setSeleccion({ funcionId: funcion.id, butacas: nuevas });
    cola.current = cola.current.then(() => sostener(asiento, butacas));
    return cola.current;
  };

  // Manda la selección vigente al salir de la cola, no la de cuando se tocó: así el último pedido trae todas.
  async function sostener(asiento, butacas) {
    let rechazadas;
    try {
      rechazadas = await sostenerSeleccion(funcion.id, vigentes.current);
    } catch (e) {
      // Un 400 es una selección que el backend no aparta entera (tope, butaca fuera de servicio): se suelta y se dice por qué.
      // Otro error no cambia nada: la doble venta la impide la base.
      if (e.status !== 400) return;
      if (!butacas[asiento.codigo]) {
        setSeleccion((s) => ({ ...s, butacas: sinButacas(s.butacas, [asiento.codigo]) }));
      }
      avisar(e.message, "error");
      recargar();
      return;
    }
    if (rechazadas.length) {
      // Que se escape una butaca es que otro llegó primero: el mapa se vuelve a pedir.
      setSeleccion((s) => ({ ...s, butacas: sinButacas(s.butacas, rechazadas) }));
      avisar(`${rechazadas.join(", ")}: alguien las está comprando`, "error");
      recargar();
    }
  }
}
