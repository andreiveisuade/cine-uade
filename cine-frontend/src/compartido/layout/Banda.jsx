import { Box } from "@mantine/core";
import { Contenedor } from "./Contenedor.jsx";

// Franja a sangre que rompe el contenedor de la página: la sala a oscuras, igual en modo claro y oscuro.
export function Banda({ children, fondo, ...props }) {
  return (
    <Box pos="relative" c="var(--cine-banda-texto)" bg="var(--cine-banda)"
      style={{ marginInline: "calc(50% - 50vw)", overflow: "hidden", isolation: "isolate" }} {...props}>
      {fondo}
      <Contenedor pos="relative">{children}</Contenedor>
    </Box>
  );
}
