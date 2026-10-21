import { useEffect, useRef, useState } from "react";

// 32 px es el mínimo táctil: una sala ancha no entra en un celular, así que el scroll arranca en el centro.
export function useScrollCentrado(filas, cantidad) {
  const marco = useRef(null);
  const [desborda, setDesborda] = useState(false);
  useEffect(() => {
    const el = marco.current;
    if (!el) return;
    el.scrollLeft = (el.scrollWidth - el.clientWidth) / 2;
    setDesborda(el.scrollWidth > el.clientWidth + 1);
  }, [filas, cantidad]);
  return { marco, desborda };
}
