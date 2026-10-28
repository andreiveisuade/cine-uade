import { useState } from "react";

// Lo que repite todo formulario al mandar: el botón cargando mientras viaja y el mensaje del backend si falla.
export function useEnviar() {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  async function enviar(pedido) {
    setError(null);
    setEnviando(true);
    try {
      await pedido();
    } catch (e) {
      setError(e.message);
    }
    setEnviando(false);
  }

  return { enviar, enviando, error };
}
