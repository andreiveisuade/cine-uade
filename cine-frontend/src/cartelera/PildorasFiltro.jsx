import { Button, Group } from "@mantine/core";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";

export function PildorasFiltro({ ariaLabel, opciones, activa, urlDe }) {
  return (
    <Group gap={6} role="group" aria-label={ariaLabel}>
      {["", ...opciones].map((o) => (
        <Button key={o || "todos"} component={Link} to={urlDe(o)} size="compact-sm" radius="xl"
          variant={(activa || "") === o ? "filled" : "default"}>
          {o ? etiqueta(o) : "Todos"}
        </Button>
      ))}
    </Group>
  );
}
