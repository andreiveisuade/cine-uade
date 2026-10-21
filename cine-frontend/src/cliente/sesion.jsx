import { createContext, useContext, useEffect, useState } from "react";
import * as api from "../api/api-http.js";

// Quién está con sesión: undefined mientras se pregunta, null si nadie. La sesión es la cookie del backend;
// acá solo queda el usuario para mostrar, y al recargar se le vuelve a preguntar al backend.
const Contexto = createContext(null);

export function SesionDelCliente({ children }) {
  const [usuario, setUsuario] = useState(undefined);

  useEffect(() => {
    api.sesionActual().then(setUsuario, () => setUsuario(null));
  }, []);

  return <Contexto.Provider value={{ usuario, setUsuario }}>{children}</Contexto.Provider>;
}

export const useSesion = () => useContext(Contexto);
