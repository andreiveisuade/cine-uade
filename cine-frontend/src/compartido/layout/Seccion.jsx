import { Box, Group, Stack, Text, Title } from "@mantine/core";

// Un bloque de la página: título con su acción a la derecha y el contenido debajo, con el mismo ritmo en todas.
export function Seccion({ titulo, bajada, accion, id, children, ...props }) {
  return (
    <Box component="section" id={id} {...props}>
      <Group justify="space-between" align="flex-end" gap="md" mb="lg">
        <Stack gap={4}>
          <Title order={2}>{titulo}</Title>
          {bajada && <Text c="dimmed" size="sm">{bajada}</Text>}
        </Stack>
        {accion}
      </Group>
      {children}
    </Box>
  );
}
