import { useEffect, useEffectEvent } from "react";
import { sinButacas, sostenerSeleccion } from "./bloqueo.js";
import { useCompra } from "./CompraEnCurso.jsx";

const RENOVAR_CADA_MS = 60_000;

export function useRenovarBloqueo(funcionId) {
  const { seleccion, setSeleccion } = useCompra();
  // El intervalo se arma una vez por función y lee la selección actual sin rearmarlo.
  // Si la selección es de otra función no hay nada que sostener.
  const sostenerActual = useEffectEvent(async () =>
    seleccion.funcionId === funcionId ? sostenerSeleccion(funcionId, seleccion.butacas) : []);

  useEffect(() => {
    if (!funcionId) return;
    const renovacion = setInterval(async () => {
      const rechazadas = await sostenerActual().catch(() => []);
      if (rechazadas.length) {
        // Seguir mostrándolas elegidas sería prometer una butaca que ya no está.
        setSeleccion((s) => ({ ...s, butacas: sinButacas(s.butacas, rechazadas) }));
      }
    }, RENOVAR_CADA_MS);
    return () => clearInterval(renovacion);
  }, [funcionId, setSeleccion]);
}
