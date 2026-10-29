import { useEffect, useEffectEvent, useState } from "react";
import * as api from "@/api/api-http.js";

const ESPERA_MS = 400;
const MAXIMO = 20;

// El pedido pendiente que ya tiene la reserva, como productoId → cantidad.
function cantidadesDe(reserva) {
  const pendiente = (reserva.candy ?? []).find((c) => c.estado === "PENDIENTE");
  return Object.fromEntries((pendiente?.items ?? []).map((i) => [i.productoId, i.cantidad]));
}

const iguales = (a, b) => Object.keys({ ...a, ...b }).every((id) => (a[id] ?? 0) === (b[id] ?? 0));

// R30: cada cambio manda el pedido entero, de a uno por vez: dos PUT a la vez chocan por @Version (409). Los clics
// seguidos se juntan en uno, y lo que el backend rechaza vuelve a lo último guardado.
export function useCandyDeLaCompra(reserva, alGuardar, alFallar) {
  const [guardadas, setGuardadas] = useState(() => cantidadesDe(reserva));
  const [cantidades, setCantidades] = useState(guardadas);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);
  const pendiente = !iguales(cantidades, guardadas);
  const guardado = useEffectEvent(alGuardar);
  const fallo = useEffectEvent(alFallar);

  useEffect(() => {
    if (!pendiente || enviando) return;
    const espera = setTimeout(async () => {
      setEnviando(true);
      setError(null);
      try {
        setGuardadas(cantidadesDe(await api.pedirCandy(reserva.codigo, cantidades)));
        guardado();
      } catch (e) {
        setError(e.message);
        fallo(e.message);
        setCantidades(guardadas);
      }
      setEnviando(false);
    }, ESPERA_MS);
    return () => clearTimeout(espera);
  }, [pendiente, enviando, cantidades, guardadas, reserva.codigo]);

  function cambiar(productoId, cuanto) {
    setCantidades((actuales) => {
      const nueva = Math.min(MAXIMO, Math.max(0, (actuales[productoId] ?? 0) + cuanto));
      const { [productoId]: _, ...resto } = actuales;
      return nueva ? { ...resto, [productoId]: nueva } : resto;
    });
  }

  function quitar(productoId) {
    setCantidades(({ [productoId]: _, ...resto }) => resto);
  }

  const hayPedido = Object.keys(guardadas).length > 0;
  return { cantidades, cambiar, quitar, vaciar: () => setCantidades({}), hayPedido, actualizando: pendiente || enviando,
    error, maximo: MAXIMO };
}
