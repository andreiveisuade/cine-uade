import { Accordion, Group, Text } from "@mantine/core";
import { useReducedMotion } from "@mantine/hooks";
import { etiqueta } from "@/api/etiquetas.js";
import { PildorasFiltro } from "./PildorasFiltro.jsx";

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
        <PildorasFiltro ariaLabel={ariaLabel} opciones={opciones} activa={activa} urlDe={urlDe} />
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
