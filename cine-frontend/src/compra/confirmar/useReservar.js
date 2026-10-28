import { useState } from "react";
import { useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { sesionDeCompra } from "@/compra/bloqueo.js";
import { useCompra } from "@/compra/CompraEnCurso.jsx";

// Crear la reserva con lo elegido y llevar al ticket; si otra compra ganó una butaca, volver al mapa.
export function useReservar(funcionId) {
  const navegar = useNavigate();
  const avisar = useAvisar();
  const { seleccion, setSeleccion } = useCompra();
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  // La reserva ya está hecha y se va al ticket: vaciar la selección no es "se perdió". Es estado y no ref porque
  // el render lo lee (esta pantalla sigue montada mientras dura la animación de salida).
  const [reservada, setReservada] = useState(false);

  async function reservar() {
    setError(null);
    setEnviando(true);
    try {
      const reserva = await api.crearReserva({
        funcionId, butacas: seleccion.butacas,
        // La misma sesión que bloqueó: si no, el propio bloqueo rebotaría la reserva.
        sesion: sesionDeCompra(),
      });
      setReservada(true);
      setSeleccion({ funcionId: null, butacas: {} });
      navegar(`/ticket/${reserva.codigo}`);
    } catch (e) {
      setEnviando(false);
      // 409: otra compra ganó una butaca en el medio (vendida o bloqueada); vuelve al mapa recargado.
      if (e.status === 409) {
        setSeleccion({ funcionId, butacas: {} });
        avisar(e.message, "error");
        navegar(`/funcion/${funcionId}`);
        return;
      }
      setError(e.message);
    }
  }

  return { reservar, enviando, error, reservada };
}
