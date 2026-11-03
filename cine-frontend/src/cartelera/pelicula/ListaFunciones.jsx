import { Accordion, Group, Stack, Text } from "@mantine/core";
import { useReducedMotion } from "@mantine/hooks";
import { cantidad, dia, porDia } from "@/compartido/formato.js";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { Horario } from "./Horario.jsx";

// Un día por panel: el primero abierto, el resto plegado con lo justo para elegir sin abrirlo.
export function ListaFunciones({ funciones }) {
  const quieto = useReducedMotion(undefined, { getInitialValueInEffect: false });
  if (!funciones.length) return <Vacio>No hay funciones programadas.</Vacio>;
  const dias = porDia(funciones);
  return (
    <Accordion multiple defaultValue={[dias[0][0]]} transitionDuration={quieto ? 0 : 200} chevronPosition="right"
      styles={{ item: { borderColor: "var(--cine-borde)" }, control: { paddingInline: 0 }, content: { paddingInline: 0 } }}>
      {dias.map(([clave, delDia]) => (
        <Accordion.Item key={clave} value={clave}>
          <Accordion.Control>
            <Group justify="space-between" wrap="nowrap" pr="xs">
              <Text size="sm" fw={700} tt="uppercase" style={{ letterSpacing: "0.04em" }}>{dia(delDia[0].inicio)}</Text>
              <Text size="sm" c="dimmed">{cantidad(delDia.length, "función", "funciones")}</Text>
            </Group>
          </Accordion.Control>
          <Accordion.Panel>
            <Stack gap="xs">
              {delDia.map((f) => <Horario key={f.id} funcion={f} />)}
            </Stack>
          </Accordion.Panel>
        </Accordion.Item>
      ))}
    </Accordion>
  );
}
