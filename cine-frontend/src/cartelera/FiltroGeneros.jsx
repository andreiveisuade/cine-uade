import { Button, Group } from "@mantine/core";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";

export function FiltroGeneros({ generos, genero }) {
  return (
    <Group gap={6} wrap="nowrap" style={{ overflowX: "auto", scrollbarWidth: "none" }} aria-label="Filtrar por género">
      {["", ...generos].map((g) => (
        <Button key={g || "todos"} component={Link} to={g ? `/cartelera/${g}` : "/cartelera"} size="compact-sm" radius="xl"
          variant={(genero || "") === g ? "filled" : "default"} style={{ flexShrink: 0 }}>
          {g ? etiqueta(g) : "Todos"}
        </Button>
      ))}
    </Group>
  );
}
