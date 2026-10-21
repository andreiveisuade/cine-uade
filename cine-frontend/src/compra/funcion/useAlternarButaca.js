import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { sinButacas, sostenerSeleccion } from "@/compra/bloqueo.js";
import { useCompra } from "@/compra/CompraEnCurso.jsx";

// Tocar una butaca la suma o la suelta y le pide al backend que sostenga la selección entera.
export function useAlternarButaca(funcion, butacas, recargar) {
  const { setSeleccion } = useCompra();
  const avisar = useAvisar();

  return async function alternar(asiento) {
    // Arranca en GENERAL: una tarifa reducida hay que acreditarla en la puerta.
    const nuevas = butacas[asiento.codigo]
      ? sinButacas(butacas, [asiento.codigo])
      : { ...butacas, [asiento.codigo]: "GENERAL" };
    // Se pinta antes del bloqueo para que se vea elegida en el acto.
    setSeleccion({ funcionId: funcion.id, butacas: nuevas });

    let rechazadas;
    try {
      rechazadas = await sostenerSeleccion(funcion.id, nuevas);
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
  };
}
