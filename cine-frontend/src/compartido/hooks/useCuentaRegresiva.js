import { useEffect, useEffectEvent, useState } from "react";

// Contra un instante fijo y no restando de a uno: en una pestaña de fondo el navegador espacia los intervalos.
export function useCuentaRegresiva(segundos, alTerminar) {
  const [restantes, setRestantes] = useState(segundos);
  const terminar = useEffectEvent(alTerminar);

  useEffect(() => {
    const fin = performance.now() + segundos * 1000;
    const intervalo = setInterval(() => {
      const quedan = Math.max(0, Math.ceil((fin - performance.now()) / 1000));
      setRestantes(quedan);
      if (quedan === 0) {
        clearInterval(intervalo);
        terminar();
      }
    }, 1000);
    return () => clearInterval(intervalo);
  }, [segundos]);

  return restantes;
}
