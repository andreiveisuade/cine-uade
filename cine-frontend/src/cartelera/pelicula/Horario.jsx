import { Group, Stack, Text, UnstyledButton } from "@mantine/core";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";
import { hora, precio } from "@/api/formato.js";

// Un horario es un botón: la hora manda, el resto la acompaña.
export function Horario({ funcion }) {
  return (
    <UnstyledButton component={Link} to={`/funcion/${funcion.id}`} p="sm"
      style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12,
               border: "1px solid var(--cine-borde)", borderRadius: "var(--mantine-radius-md)", background: "var(--cine-superficie)" }}>
      <Group gap="md" wrap="nowrap">
        <Text className="cine-display cine-cifras" fz={30} fw={850} lh={1}>{hora(funcion.inicio)}</Text>
        <Stack gap={2}>
          <Text size="sm" fw={600}>{funcion.sala.nombre} · {etiqueta(funcion.sala.tipo)}</Text>
          <Text size="xs" c="dimmed">{etiqueta(funcion.proyeccion)} · {etiqueta(funcion.idioma)}</Text>
        </Stack>
      </Group>
      <Stack gap={0} align="flex-end">
        <Text size="xs" c="dimmed">desde</Text>
        <Text fw={700} className="cine-cifras">{precio(funcion.precioDesde)}</Text>
      </Stack>
    </UnstyledButton>
  );
}
