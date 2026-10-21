import { useRef, useState } from "react";
import { useMediaQuery } from "@mantine/hooks";

// Con mouse, quedarse sobre una tarjeta muestra el trailer muteado arriba del póster; en touch no hay hover.
const ESPERA_PREVIA = 600;

export function usePrevia() {
  const conMouse = useMediaQuery("(hover: hover) and (pointer: fine)");
  const [actual, setActual] = useState(null);
  const espera = useRef(null);
  function entrar(pelicula) {
    if (!conMouse || !pelicula.trailerYoutube) return;
    clearTimeout(espera.current);
    espera.current = setTimeout(() => setActual(pelicula.id), ESPERA_PREVIA);
  }
  function salir() {
    clearTimeout(espera.current);
    setActual(null);
  }
  return { actual, entrar, salir };
}
