import { Badge, Box, Group, Stack, Text, Title } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { Poster } from "@/compartido/ui/Poster.jsx";
import { avisoDe, COLOR_ESTADO } from "@/ticket/estadoReserva.js";

export function CabeceraEntrada({ reserva }) {
  return (
    <Group gap="md" wrap="nowrap" align="flex-start">
      <Box w={64} style={{ flexShrink: 0, borderRadius: "var(--mantine-radius-sm)", overflow: "hidden" }}>
        <Poster pelicula={reserva.pelicula} />
      </Box>
      <Stack gap={6}>
        <Group gap="sm">
          <Badge color={COLOR_ESTADO[reserva.estado]} variant="filled" size="md">{etiqueta(reserva.estado)}</Badge>
          <Text size="xs" c="dimmed" className="cine-cifras">Reserva #{reserva.id}</Text>
        </Group>
        <Title order={1} fz="clamp(1.75rem, 1.3rem + 1.6vw, 2.5rem)">{reserva.pelicula.titulo}</Title>
        {avisoDe(reserva) && <Text size="sm" c="dimmed">{avisoDe(reserva)}</Text>}
      </Stack>
    </Group>
  );
}
