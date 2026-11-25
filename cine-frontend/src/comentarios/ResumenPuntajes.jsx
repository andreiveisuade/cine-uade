import { Group, Paper, Progress, Rating, Stack, Text } from "@mantine/core";
import { cantidad } from "@/compartido/formato.js";

const PUNTAJES = [5, 4, 3, 2, 1];

export function ResumenPuntajes({ comentarios }) {
  const total = comentarios.length;
  const promedio = comentarios.reduce((suma, c) => suma + c.puntaje, 0) / total;
  const cuantos = (puntaje) => comentarios.filter((c) => c.puntaje === puntaje).length;

  return (
    <Paper withBorder p={{ base: "md", sm: "lg" }}>
      <Group gap="xl" align="center" justify="center">
        <Stack gap={4} align="center">
          <Text className="cine-display" fz={56} fw={800} lh={1}>{promedio.toFixed(1).replace(".", ",")}</Text>
          <Rating value={promedio} fractions={4} readOnly color="var(--cine-foco)" aria-label="Puntaje promedio" />
          <Text size="sm" c="dimmed">{cantidad(total, "comentario")}</Text>
        </Stack>
        <Stack gap={6} flex={1} miw={220}>
          {PUNTAJES.map((puntaje) => (
            <Group key={puntaje} gap="sm" wrap="nowrap">
              <Text size="sm" c="dimmed" className="cine-cifras" w={12} ta="center">{puntaje}</Text>
              <Progress flex={1} value={(cuantos(puntaje) / total) * 100} color="var(--cine-foco)" radius="xl"
                aria-label={`${puntaje} de 5: ${cantidad(cuantos(puntaje), "comentario")}`} />
            </Group>
          ))}
        </Stack>
      </Group>
    </Paper>
  );
}
