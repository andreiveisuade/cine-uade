import "@mantine/core/styles.css";
import { createTheme, localStorageColorSchemeManager, MantineProvider, v8CssVariablesResolver } from "@mantine/core";
import { Avisos } from "./componentes/Avisos.jsx";

const tema = createTheme({
  primaryColor: "indigo",
  defaultRadius: "md",
});

// Los fondos light de Mantine 9 son sólidos y en oscuro gritan; los de la 8 son translúcidos.
const variables = v8CssVariablesResolver;

const preferencia = localStorageColorSchemeManager({ key: "cine-tema" });

export function Base({ children }) {
  return (
    <MantineProvider theme={tema} cssVariablesResolver={variables} colorSchemeManager={preferencia} defaultColorScheme="auto">
      <Avisos>{children}</Avisos>
    </MantineProvider>
  );
}
