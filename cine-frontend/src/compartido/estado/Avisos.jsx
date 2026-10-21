import { createContext, useCallback, useContext, useState } from "react";
import { Notification, Portal, Stack } from "@mantine/core";

const Contexto = createContext(() => {});

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

export const useAvisar = () => useContext(Contexto);
