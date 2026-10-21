import { Center, Group, Loader, Text } from "@mantine/core";

export function Cargando({ mensaje = "Cargando…" }) {
  return (
    <Center py="xl">
      <Group gap="sm">
        <Loader size="sm" />
        <Text c="dimmed">{mensaje}</Text>
      </Group>
    </Center>
  );
}
