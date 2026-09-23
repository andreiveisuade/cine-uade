import "@mantine/core/styles.css";
import { createTheme, localStorageColorSchemeManager, MantineProvider, v8CssVariablesResolver } from "@mantine/core";
import { Avisos } from "./componentes/Avisos.jsx";

const tema = createTheme({
  primaryColor: "indigo",
  defaultRadius: "md",
});

// Los fondos "light" de Mantine 9 son sólidos, y en oscuro un aviso amarillo se vuelve un
// bloque marrón que grita más que el contenido. Los de la 8 son translúcidos y acompañan.
const variables = v8CssVariablesResolver;

// Misma clave que usaba el front anterior: quien ya había elegido oscuro lo conserva.
const preferencia = localStorageColorSchemeManager({ key: "cine-tema" });

/** Lo que comparten el sitio del cliente y el panel: tema, modo oscuro y avisos. */
export function Base({ children }) {
  return (
    <MantineProvider theme={tema} cssVariablesResolver={variables} colorSchemeManager={preferencia} defaultColorScheme="auto">
      <Avisos>{children}</Avisos>
    </MantineProvider>
  );
}
