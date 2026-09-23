import { Anchor } from "@mantine/core";
import { Link } from "react-router";

/** El «← algo» de arriba de las pantallas de detalle. */
export function Volver({ a, children }) {
  return (
    <Anchor component={Link} to={a} size="sm" c="dimmed">
      ← {children}
    </Anchor>
  );
}
