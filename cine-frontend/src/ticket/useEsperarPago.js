import { useEffect, useState } from "react";
import * as api from "@/api/api-http.js";

// El pago se concilia solo en el backend: mientras espera, se vuelve a pedir para que aparezca "Pagada" sin recargar.
const CADA_MS = 10_000;

export function useEsperarPago(codigo, inicial) {
  const [actualizada, setActualizada] = useState(null);
  const esperandoPago = inicial?.estado === "RESERVADA" && inicial.cobrable
    && !(actualizada?.codigo === codigo && actualizada.estado !== "RESERVADA");

  useEffect(() => {
    if (!esperandoPago) return;
    const intervalo = setInterval(() => {
      api.obtenerReservaPorCodigo(codigo).then(setActualizada, () => {});
    }, CADA_MS);
    return () => clearInterval(intervalo);
  }, [esperandoPago, codigo]);

  const reserva = actualizada?.codigo === codigo ? actualizada : inicial;
  // Lo refrescado solo existe si se estaba esperando el pago: si dice PAGADA, el cambio pasó con la pantalla abierta.
  const pagoEnVivo = actualizada?.codigo === codigo && actualizada.estado === "PAGADA";
  return { reserva, pagoEnVivo };
}
