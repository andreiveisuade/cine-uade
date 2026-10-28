import { useEffect, useEffectEvent } from "react";
import * as api from "@/api/api-http.js";
import { useEnviar } from "@/compartido/hooks/useEnviar.js";

// La pestaña recuerda el checkout abierto: si se vuelve de Mercado Pago con Atrás, se confirma desde acá.
const CLAVE = "cine.checkout";

// Cada «Pagar» abre un checkout nuevo: uno abierto antes de cambiar el candy ya no se cobra (409). Mercado Pago se
// abre en la misma pestaña; otra pestaña algunos navegadores ni la muestran.
export function usePagar(codigo) {
  const { enviar, enviando, error } = useEnviar();

  const pagar = (medio) => enviar(async () => {
    const checkout = await api.abrirCheckout(codigo, medio);
    sessionStorage.setItem(CLAVE, JSON.stringify({ codigo, id: checkout.id }));
    window.location.assign(checkout.urlPago);
  });

  return { pagar, pagando: enviando, error };
}

// En localhost MP no redirige al sitio: al volver, el backend consulta el pago y cobra si aprobó, sin esperar a la
// conciliación. La clave se borra antes de avisar, así un doble montaje no pregunta dos veces.
export function useVueltaDelPago(codigo, alVolver) {
  const volver = useEffectEvent(alVolver);

  useEffect(() => {
    let abierto = null;
    try {
      abierto = JSON.parse(sessionStorage.getItem(CLAVE));
    } catch {
      return;
    }
    if (abierto?.codigo !== codigo) return;
    sessionStorage.removeItem(CLAVE);
    api.avisarVueltaDelPago(abierto.id).then(volver);
  }, [codigo]);
}
