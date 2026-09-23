import { createContext, useCallback, useContext, useState } from "react";
import { Notification, Portal, Stack } from "@mantine/core";

const Contexto = createContext(() => {});

/**
 * Aviso efímero arriba de todo, para el resultado de una acción: «Reserva cancelada», o el
 * `{error}` del backend tal cual. Es un contexto y no una librería de notificaciones porque
 * lo único que hace falta es mostrar un texto tres segundos y medio.
 */
export function Avisos({ children }) {
  const [avisos, setAvisos] = useState([]);

  const avisar = useCallback((mensaje, tipo = "ok") => {
    const id = crypto.randomUUID();
    setAvisos((lista) => [...lista, { id, mensaje, tipo }]);
    setTimeout(() => setAvisos((lista) => lista.filter((a) => a.id !== id)), 3500);
  }, []);

  return (
    <Contexto.Provider value={avisar}>
      {children}
      <Portal>
        <Stack gap="xs" pos="fixed" top={16} left="50%" style={{ transform: "translateX(-50%)", zIndex: 1000 }}>
          {avisos.map((a) => (
            <Notification key={a.id} color={a.tipo === "ok" ? "green" : "red"} withCloseButton={false} withBorder>
              {a.mensaje}
            </Notification>
          ))}
        </Stack>
      </Portal>
    </Contexto.Provider>
  );
}

/** `avisar(mensaje)` para lo que salió bien, `avisar(mensaje, "error")` para lo otro. */
export const useAvisar = () => useContext(Contexto);
