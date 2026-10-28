import { useState } from "react";
import * as api from "@/api/api-http.js";
import { useEnviar } from "@/compartido/hooks/useEnviar.js";
import { useVueltaDelPago } from "./useVueltaDelPago.js";

// Los tres pasos del pago online: traer los medios, abrir el checkout con el medio elegido e ir a la pasarela.
export function usePagarOnline(codigo, alVolver) {
  const [medios, setMedios] = useState(null);
  const [medio, setMedio] = useState("");
  const [checkout, setCheckout] = useState(null);
  const [pagando, setPagando] = useState(false);
  const { enviar, enviando, error } = useEnviar();
  const avisarVuelta = useVueltaDelPago(pagando ? checkout.id : null, alVolver);

  // Si MP rechazó, el motivo viaja en el 302 del retorno, que el fetch no lee: se mira cómo quedó la reserva.
  const yaPague = () => enviar(async () => {
    const reserva = await avisarVuelta();
    if (reserva?.cobrable) throw new Error("Mercado Pago todavía no confirmó el pago");
  });

  // Mientras llegan los medios el botón queda cargando: un doble clic no pide dos veces.
  const empezar = () => enviar(async () => {
    // Online solo los que pasan por la pasarela; el efectivo se paga en la boletería.
    const electronicos = (await api.obtenerMediosDePago()).filter((m) => m.requiereAutorizacion);
    setMedios(electronicos);
    setMedio(electronicos[0]?.nombre || "");
  });

  const abrir = () => enviar(async () => setCheckout(await api.abrirCheckout(codigo, medio)));

  // En la misma pestaña: otra pestaña algunos navegadores ni la muestran. MP vuelve solo al ticket (auto_return);
  // si se vuelve con Atrás y el navegador restaura la página, queda "Ya pagué".
  function irAPagar() {
    setPagando(true);
    window.location.assign(checkout.urlPago);
  }

  function cambiarMedio() {
    setPagando(false);
    setCheckout(null);
  }

  return { medios, medio, setMedio, checkout, pagando, enviando, error, empezar, abrir, irAPagar, yaPague, cambiarMedio };
}
