import { Anchor } from "@mantine/core";
import { NavLink } from "react-router";

// Un link de la barra: la sección en la que se está queda resaltada.
export function Enlace({ a, children, alElegir }) {
  return (
    <Anchor component={NavLink} to={a} size="sm" fw={500} c="dimmed" underline="never" onClick={alElegir}
      style={({ isActive }) => (isActive ? { color: "var(--mantine-color-text)" } : undefined)}>
      {children}
    </Anchor>
  );
}
