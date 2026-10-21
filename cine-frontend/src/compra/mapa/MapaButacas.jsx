import { Box, Group, Paper, Stack, Text } from "@mantine/core";
import { m } from "motion/react";
import { BotonButaca } from "./BotonButaca.jsx";
import { PantallaDeLuz } from "./PantallaDeLuz.jsx";
import { useScrollCentrado } from "./useScrollCentrado.js";

// Las filas entran de a una, de la pantalla hacia el fondo, como se llena una sala.
const SALA = { oculta: {}, visible: { transition: { staggerChildren: 0.03 } } };
const FILA = { oculta: { opacity: 0, y: 6 }, visible: { opacity: 1, y: 0, transition: { duration: 0.2 } } };

// pintar: (asiento) => { estilo, deshabilitado, titulo }
export function MapaButacas({ sala, asientos, pintar, alElegir }) {
  const filas = [];
  for (let fila = 1; fila <= sala.filas; fila++) filas.push(asientos.filter((a) => a.fila === fila));
  const { marco, desborda } = useScrollCentrado(sala.filas, asientos.length);

  return (
    <Paper withBorder p={{ base: "md", sm: "xl" }}>
      {desborda && <Text size="xs" c="dimmed" ta="center" mb="xs">Deslizá para ver toda la sala</Text>}
      <Box ref={marco} style={{ overflowX: "auto", ...(desborda && {
        maskImage: "linear-gradient(90deg, transparent, #000 24px, #000 calc(100% - 24px), transparent)" }) }}>
        <PantallaDeLuz />
        <Stack component={m.div} variants={SALA} initial="oculta" animate="visible" gap={6} style={{ minWidth: "max-content" }} mx="auto" w="fit-content">
          {filas.map((deLaFila, i) => (
            <Group component={m.div} variants={FILA} key={i} gap={6} justify="center" wrap="nowrap">
              <Text w={16} ta="right" fz="xs" fw={600} c="dimmed">{deLaFila[0]?.codigo.charAt(0)}</Text>
              {deLaFila.map((a) => <BotonButaca key={a.codigo} asiento={a} {...pintar(a)} alElegir={alElegir} />)}
            </Group>
          ))}
        </Stack>
      </Box>
    </Paper>
  );
}
