import { useEffect } from "react";

export function useTitulo(texto) {
  useEffect(() => {
    const anterior = document.title;
    if (texto) document.title = `${texto} · Cine UADE`;
    return () => {
      document.title = anterior;
    };
  }, [texto]);
}
