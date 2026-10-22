import { useEffect } from "react";
import { animate, m, useMotionValue, useReducedMotion, useTransform } from "motion/react";
import { precio } from "@/compartido/formato.js";

// El total corre hasta el nuevo valor: se ve que cambió y cuánto, sin re-renderizar en cada cuadro.
// `desde` hace que cuente al aparecer (el ahorro de un combo arranca en 0).
export function MontoAnimado({ valor, desde = valor }) {
  const monto = useMotionValue(desde);
  const texto = useTransform(monto, precio);
  const quieto = useReducedMotion();

  useEffect(() => {
    const animacion = animate(monto, valor, { duration: quieto ? 0 : 0.35, ease: "easeOut" });
    return () => animacion.stop();
  }, [monto, valor, quieto]);

  return <m.span>{texto}</m.span>;
}
