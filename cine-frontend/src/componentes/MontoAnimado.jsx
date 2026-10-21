import { useEffect } from "react";
import { animate, m, useMotionValue, useReducedMotion, useTransform } from "motion/react";
import { precio } from "../api/formato.js";

// El total corre hasta el nuevo valor: se ve que cambió y cuánto, sin re-renderizar en cada cuadro.
export function MontoAnimado({ valor }) {
  const monto = useMotionValue(valor);
  const texto = useTransform(monto, precio);
  const quieto = useReducedMotion();

  useEffect(() => {
    const animacion = animate(monto, valor, { duration: quieto ? 0 : 0.35, ease: "easeOut" });
    return () => animacion.stop();
  }, [monto, valor, quieto]);

  return <m.span>{texto}</m.span>;
}
