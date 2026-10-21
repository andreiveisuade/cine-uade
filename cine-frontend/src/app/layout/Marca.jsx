import { Box, UnstyledButton } from "@mantine/core";
import { Link } from "react-router";

export function Marca() {
  return (
    <UnstyledButton component={Link} to="/" className="cine-display" fz={26} fw={850} lh={1} aria-label="Cine UADE, inicio">
      CINE <Box component="span" c="var(--mantine-color-ambar-text)">UADE</Box>
    </UnstyledButton>
  );
}
