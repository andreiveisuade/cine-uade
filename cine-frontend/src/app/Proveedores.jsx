import "@mantine/core/styles.css";
import "@fontsource-variable/archivo/standard.css";
import "./tema.css";
import { localStorageColorSchemeManager, MantineProvider } from "@mantine/core";
import { domMax, LazyMotion, MotionConfig } from "motion/react";
import { Avisos } from "@/compartido/estado/Avisos.jsx";
import { tema, variables } from "./tema.js";

// La misma clave la lee el script del <head> que evita el fogonazo blanco al cargar.
const preferencia = localStorageColorSchemeManager({ key: "cine-tema" });

export function Proveedores({ children }) {
  return (
    <MantineProvider theme={tema} cssVariablesResolver={variables} colorSchemeManager={preferencia} defaultColorScheme="auto">
      {/* Con «reducir movimiento» en el sistema, Motion salta las animaciones de transformación. */}
      {/* Sin chunk aparte: si no bajaba, cada `initial` con opacidad 0 quedaba invisible hasta el F5. */}
      <LazyMotion features={domMax} strict>
        <MotionConfig reducedMotion="user">
          <Avisos>{children}</Avisos>
        </MotionConfig>
      </LazyMotion>
    </MantineProvider>
  );
}
