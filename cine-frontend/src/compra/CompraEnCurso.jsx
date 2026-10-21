import { createContext, useContext, useState } from "react";

// Código de butaca → tarifa, porque la tarifa es por persona.
// En memoria y no en el navegador: recargar la pierde, igual que vence el bloqueo del backend.
const Contexto = createContext(null);

export function CompraEnCurso({ children }) {
  const [seleccion, setSeleccion] = useState({ funcionId: null, butacas: {} });
  return <Contexto.Provider value={{ seleccion, setSeleccion }}>{children}</Contexto.Provider>;
}

export const useCompra = () => useContext(Contexto);
