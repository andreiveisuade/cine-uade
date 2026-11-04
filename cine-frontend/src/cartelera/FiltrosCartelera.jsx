import { Accordion, Button, Group, Text } from "@mantine/core";
import { useReducedMotion } from "@mantine/hooks";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";

const url = (genero, tipo) => `/cartelera${genero ? `/${genero}` : ""}${tipo ? `?tipo=${tipo}` : ""}`;

function Filtro({ titulo, ariaLabel, opciones, activa, urlDe }) {
  return (
    <Accordion.Item value={titulo}>
      <Accordion.Control>
        <Group gap="xs">
          <Text size="sm" fw={600} tt="uppercase">{titulo}</Text>
          {/* Plegado también se tiene que ver qué está filtrando. */}
          {activa && <Text size="sm" c="dimmed">{etiqueta(activa)}</Text>}
        </Group>
      </Accordion.Control>
      <Accordion.Panel>
        <Group gap={6} aria-label={ariaLabel}>
          {["", ...opciones].map((o) => (
            <Button key={o || "todos"} component={Link} to={urlDe(o)} size="compact-sm" radius="xl"
              variant={(activa || "") === o ? "filled" : "default"}>
              {o ? etiqueta(o) : "Todos"}
            </Button>
          ))}
        </Group>
      </Accordion.Panel>
    </Accordion.Item>
  );
}

export function FiltrosCartelera({ generos, genero, tipos, tipo }) {
  const quieto = useReducedMotion(undefined, { getInitialValueInEffect: false });
  return (
    <Accordion multiple transitionDuration={quieto ? 0 : 200} chevronPosition="right"
      styles={{ item: { borderColor: "var(--cine-borde)" }, control: { paddingInline: 0 }, content: { paddingInline: 0 } }}>
      <Filtro titulo="Géneros" ariaLabel="Filtrar por género" opciones={generos} activa={genero} urlDe={(g) => url(g, tipo)} />
      <Filtro titulo="Tipo" ariaLabel="Filtrar por tipo" opciones={tipos} activa={tipo} urlDe={(t) => url(genero, t)} />
    </Accordion>
  );
}
