import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { olvidarCredenciales } from "../api/api-http.js";

// El empleado que entró, para pintar el panel. Las credenciales no van acá sino en
// api-http.js, que es el único que las manda.
const CLAVE_SESION = "cine.sesion";

function sesionGuardada() {
  const guardada = sessionStorage.getItem(CLAVE_SESION);
  return guardada ? JSON.parse(guardada) : null;
}

const Contexto = createContext(null);

export function Sesion({ children }) {
  const [empleado, setEmpleado] = useState(sesionGuardada);
  const navegar = useNavigate();

  const abrir = useCallback((datos) => {
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(datos));
    setEmpleado(datos);
  }, []);

  const cerrar = useCallback(() => {
    sessionStorage.removeItem(CLAVE_SESION);
    olvidarCredenciales();
    setEmpleado(null);
    navegar("/login");
  }, [navegar]);

  // api-http.js lo dispara ante un 401: sin credenciales que valgan no queda nada que
  // hacer en el panel más que volver a entrar.
  useEffect(() => {
    window.addEventListener("cine:sesion-vencida", cerrar);
    return () => window.removeEventListener("cine:sesion-vencida", cerrar);
  }, [cerrar]);

  // El acomodador solo valida entradas: no tiene por qué ver el ABM de la cartelera.
  const esAdministrador = !!empleado && empleado.rol !== "ACOMODADOR";

  return <Contexto.Provider value={{ empleado, esAdministrador, abrir, cerrar }}>{children}</Contexto.Provider>;
}

export const useSesion = () => useContext(Contexto);
