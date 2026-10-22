import { useEffect, useEffectEvent } from "react";
import * as api from "@/api/api-http.js";

// Al volver al ticket con Atrás después de pagar en Mercado Pago, el backend consulta el pago en el momento:
// en localhost MP no redirige al sitio y, sin esto, el ticket esperaba a la conciliación (hasta 60 s).
export function useVueltaDelPago(checkoutId, alVolver) {
  const avisar = useEffectEvent(() => api.avisarVueltaDelPago(checkoutId).then(alVolver));

  useEffect(() => {
    if (!checkoutId) return;
    const alCambiar = () => {
      if (document.visibilityState === "visible") avisar();
    };
    document.addEventListener("visibilitychange", alCambiar);
    return () => document.removeEventListener("visibilitychange", alCambiar);
  }, [checkoutId]);

  return () => api.avisarVueltaDelPago(checkoutId).then(alVolver);
}
