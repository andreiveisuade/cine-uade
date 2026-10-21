import { Box } from "@mantine/core";

// El ancho de lectura de todo el sitio: mismo tope y mismos márgenes laterales en cada pantalla.
export function Contenedor({ children, ...props }) {
  return (
    <Box maw="var(--cine-ancho)" mx="auto" px="var(--cine-gutter)" {...props}>
      {children}
    </Box>
  );
}
