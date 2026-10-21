import "@mantine/core/styles.css";
import "@fontsource-variable/archivo/standard.css";
import "./tema.css";
import { localStorageColorSchemeManager, MantineProvider } from "@mantine/core";
import { LazyMotion, MotionConfig } from "motion/react";
import { Avisos } from "@/componentes/Avisos.jsx";
import { tema, variables } from "./tema.js";

// Las funciones de animación bajan en un chunk aparte: el primer pintado no las espera.
const animaciones = () => import("./animaciones.js").then((m) => m.default);

// La misma clave la lee el script del <head> que evita el fogonazo blanco al cargar.
const preferencia = localStorageColorSchemeManager({ key: "cine-tema" });

export function Proveedores({ children }) {
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
