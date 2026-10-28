import { createContext, useContext, useEffect, useState } from "react";

// Código de butaca → tarifa, porque la tarifa es por persona. En la pestaña y no en memoria: ir a Google recarga la
// página entera y la compra tiene que seguir a la vuelta. Se borra al cerrar la pestaña, como vence el bloqueo.
const CLAVE = "cine.compra";
const VACIA = { funcionId: null, butacas: {} };
const Contexto = createContext(null);

function leer() {
  try {
    return JSON.parse(sessionStorage.getItem(CLAVE)) || VACIA;
  } catch {
    return VACIA;
  }
}

export function CompraEnCurso({ children }) {
  const [seleccion, setSeleccion] = useState(leer);
  useEffect(() => sessionStorage.setItem(CLAVE, JSON.stringify(seleccion)), [seleccion]);
  return <Contexto.Provider value={{ seleccion, setSeleccion }}>{children}</Contexto.Provider>;
}

export const useCompra = () => useContext(Contexto);
