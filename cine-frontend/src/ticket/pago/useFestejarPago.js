import { useEffect, useRef } from "react";
import { useReducedMotion } from "motion/react";

// Confetti una sola vez por ticket, cuando el pago llega con la pantalla abierta.
export function useFestejarPago(pagoEnVivo, codigo) {
  const quieto = useReducedMotion();
  const celebrada = useRef(null);

  useEffect(() => {
    if (!pagoEnVivo || quieto || celebrada.current === codigo) return;
    // Se baja recién cuando hace falta: casi ningún ticket la usa.
    let activo = true;
    let confetti;
    import("canvas-confetti").then(({ default: lanzar }) => {
      if (!activo) return;
      // Se marca al lanzar y no al entrar: StrictMode desmonta y vuelve a correr el efecto, y el primero se descarta.
      celebrada.current = codigo;
      confetti = lanzar;
      lanzar({ particleCount: 120, spread: 75, origin: { y: 0.3 }, disableForReducedMotion: true });
    });
    return () => {
      activo = false;
      confetti?.reset();
    };
  }, [pagoEnVivo, quieto, codigo]);
}
