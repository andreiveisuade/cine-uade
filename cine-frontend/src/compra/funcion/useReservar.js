import { useState } from "react";
import { useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { sesionDeCompra } from "@/compra/bloqueo.js";
import { useCompra } from "@/compra/CompraEnCurso.jsx";

// Reservar lo elegido y seguir a Tu compra; si otra compra ganó una butaca, quedarse en el mapa recargado.
export function useReservar(funcionId, recargarMapa) {
  const navegar = useNavigate();
  const avisar = useAvisar();
  const { seleccion, setSeleccion } = useCompra();
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function reservar() {
    setError(null);
    setEnviando(true);
    try {
      const reserva = await api.crearReserva({
        funcionId, butacas: seleccion.butacas,
        // La misma sesión que bloqueó: si no, el propio bloqueo rebotaría la reserva.
        sesion: sesionDeCompra(),
      });
      setSeleccion({ funcionId: null, butacas: {} });
      navegar(`/compra/${reserva.codigo}`);
    } catch (e) {
      setEnviando(false);
      // 409: otra compra ganó una butaca en el medio (vendida o bloqueada).
      if (e.status === 409) {
        setSeleccion({ funcionId, butacas: {} });
        avisar(e.message, "error");
        recargarMapa();
        return;
      }
      setError(e.message);
    }
  }

  return { reservar, enviando, error };
}
