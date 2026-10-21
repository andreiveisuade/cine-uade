import { Group, Stack, Text, Title } from "@mantine/core";
import { Volver } from "./Volver.jsx";

// La cabeza de cada pantalla: de dónde se viene, qué es y qué se puede hacer, siempre en el mismo lugar.
export function Encabezado({ titulo, bajada, volver, acciones, children }) {
  return (
    <Stack gap="sm" mb="xl">
      {volver && <Volver a={volver.a}>{volver.texto}</Volver>}
      <Group justify="space-between" align="flex-end" gap="lg">
        <Stack gap={6} maw="44rem">
          <Title order={1}>{titulo}</Title>
          {bajada && <Text c="dimmed">{bajada}</Text>}
          {children}
        </Stack>
        {acciones}
      </Group>
    </Stack>
  );
}
