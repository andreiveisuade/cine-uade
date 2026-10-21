import { Stack, Text } from "@mantine/core";

export function DatoEntrada({ titulo, children, grande }) {
  return (
    <Stack gap={2}>
      <Text size="xs" c="dimmed" tt="uppercase" fw={700} style={{ letterSpacing: "0.06em" }}>{titulo}</Text>
      <Text className={grande ? "cine-display cine-cifras" : "cine-cifras"} fz={grande ? 30 : "md"} fw={grande ? 850 : 600} lh={1.1}>
        {children}
      </Text>
    </Stack>
  );
}
