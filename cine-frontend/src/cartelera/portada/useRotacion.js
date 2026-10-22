import { useState } from "react";
import { useDocumentVisibility } from "@mantine/hooks";
import { useReducedMotion } from "motion/react";

export const ROTAR_CADA_MS = 7000;

// Avance solo de la portada. Se frena con el mouse o el foco encima, con la pestaña oculta y con movimiento
// reducido: lo que se mueve solo se tiene que poder parar (WCAG 2.2.2).
export function useRotacion(cantidad, frenada) {
  const [elegida, setElegida] = useState(0);
  const [encima, setEncima] = useState(false);
  const oculta = useDocumentVisibility() === "hidden";
  const quieto = useReducedMotion();
  const rota = cantidad > 1 && !quieto;
  // Solo mouse y teclado: un toque en el celular deja hover y foco puestos y la rotación quedaría frenada.
  const conMouse = (valor) => (e) => e.pointerType === "mouse" && setEncima(valor);
  return {
    elegida: elegida % Math.max(cantidad, 1),
    elegir: setElegida,
    rota,
    corre: rota && !frenada && !encima && !oculta,
    avanzar: () => setElegida((i) => (i + 1) % cantidad),
    alrededor: {
      onPointerEnter: conMouse(true), onPointerLeave: conMouse(false),
      onFocus: (e) => e.target.matches(":focus-visible") && setEncima(true), onBlur: () => setEncima(false),
    },
  };
}
