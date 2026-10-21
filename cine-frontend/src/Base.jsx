import "@mantine/core/styles.css";
import { createTheme, localStorageColorSchemeManager, MantineProvider, v8CssVariablesResolver } from "@mantine/core";
import { LazyMotion, MotionConfig } from "motion/react";
import { Avisos } from "./componentes/Avisos.jsx";

// Las funciones de animación bajan en un chunk aparte: el primer pintado no las espera.
const animaciones = () => import("./componentes/animaciones.js").then((m) => m.default);

const tema = createTheme({
  primaryColor: "indigo",
  defaultRadius: "md",
});

// Los fondos light de Mantine 9 son sólidos y en oscuro gritan; los de la 8 son translúcidos.
const variables = v8CssVariablesResolver;

// La misma clave la lee el script del <head> que evita el fogonazo blanco al cargar.
const preferencia = localStorageColorSchemeManager({ key: "cine-tema" });

export function Base({ children }) {
  return (
    <MantineProvider theme={tema} cssVariablesResolver={variables} colorSchemeManager={preferencia} defaultColorScheme="auto">
      {/* Con «reducir movimiento» en el sistema, Motion salta las animaciones de transformación. */}
      <LazyMotion features={animaciones} strict>
        <MotionConfig reducedMotion="user">
          <Avisos>{children}</Avisos>
        </MotionConfig>
      </LazyMotion>
    </MantineProvider>
  );
}
