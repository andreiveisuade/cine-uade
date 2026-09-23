import { useCallback, useEffect, useState } from "react";

// `vigente` descarta la respuesta de un pedido viejo: si «Matri» llega después de «Matrix», no lo pisa.
export function useCargar(pedir, dependencias) {
  const [estado, setEstado] = useState({ datos: null, error: null, cargando: true });
  const [vuelta, setVuelta] = useState(0);

  useEffect(() => {
    let vigente = true;
    // Los datos anteriores quedan a la vista: recargar no hace parpadear la pantalla.
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
