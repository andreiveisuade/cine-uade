import { useCallback, useEffect, useState } from "react";

/**
 * Pide datos al abrir la pantalla y cada vez que cambian `dependencias`.
 *
 * Es lo único que hacía el router viejo por cada vista —«Cargando…», el pedido y el error
 * en rojo— y alcanza para todas: ninguna pantalla necesita caché ni reintentos, porque el
 * backend contesta enseguida y lo que se muestra tiene que ser lo último que dijo.
 *
 * `vigente` descarta la respuesta de un pedido viejo: tipear «Matrix» en un filtro son
 * varios pedidos, y si el de «Matri» llega último no puede pisar al bueno.
 */
export function useCargar(pedir, dependencias) {
  const [estado, setEstado] = useState({ datos: null, error: null, cargando: true });
  const [vuelta, setVuelta] = useState(0);

  useEffect(() => {
    let vigente = true;
    // Los datos anteriores quedan a la vista mientras llega lo nuevo: recargar después de
    // una acción no hace parpadear la pantalla entera.
    setEstado((anterior) => ({ ...anterior, cargando: true }));
    pedir().then(
      (datos) => vigente && setEstado({ datos, error: null, cargando: false }),
      (e) => vigente && setEstado({ datos: null, error: e.message, cargando: false }),
    );
    return () => {
      vigente = false;
    };
  }, [...dependencias, vuelta]);

  const recargar = useCallback(() => setVuelta((v) => v + 1), []);
  return { ...estado, recargar };
}
