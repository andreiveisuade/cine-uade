import { Box, Group, Text } from "@mantine/core";

export function Referencia({ items }) {
  return (
    <Group gap="md" mt="md" justify="center">
      {items.map(([estilo, texto]) => (
        <Group key={texto} gap={6} wrap="nowrap">
          <Box w={12} h={12} style={{ borderRadius: 3, ...estilo }} />
          <Text fz="xs" c="dimmed">{texto}</Text>
        </Group>
      ))}
    </Group>
  );
}
