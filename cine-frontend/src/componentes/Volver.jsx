import { Anchor } from "@mantine/core";
import { Link } from "react-router";

export function Volver({ a, children }) {
  return (
    <Anchor component={Link} to={a} size="sm" c="dimmed" fw={500} w="fit-content">
      ← {children}
    </Anchor>
  );
}
