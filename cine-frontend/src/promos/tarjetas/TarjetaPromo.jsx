import { Badge, Box, Group, SimpleGrid, Stack, Text, UnstyledButton } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { aplicaHoy, fecha, hoyIso } from "@/promos/diasPromo.js";
import { CondicionesPromo } from "./CondicionesPromo.jsx";
import { DiasDeLaSemana } from "./DiasDeLaSemana.jsx";
import { festejar } from "./festejar.js";
import { Foto } from "./Foto.jsx";

// Destacada, la foto va al costado del texto; si no, arriba. El beneficio es lo que se lee primero.
export function TarjetaPromo({ promo, destacada = false, conCondiciones = true }) {
  const hoy = aplicaHoy(promo);
  const empiezaDespues = promo.vigenciaDesde > hoyIso();
  return (
    <Box h="100%" style={{ background: "var(--cine-superficie)", border: "1px solid var(--cine-borde)", borderRadius: "var(--mantine-radius-lg)", overflow: "hidden" }}>
      <SimpleGrid cols={{ base: 1, md: destacada && promo.imagenUrl ? 2 : 1 }} spacing={0} h="100%">
        {promo.imagenUrl && (
          <Foto src={promo.imagenUrl} alt={promo.nombre} />
        )}
        <Stack gap="md" p={destacada ? "xl" : "lg"} justify="center">
          <Group justify="space-between" gap="sm" mih={28}>
            <Text size="sm" fw={700} tt="uppercase" c="dimmed" style={{ letterSpacing: "0.04em" }}>{promo.nombre}</Text>
            {hoy && (
              <UnstyledButton onClick={festejar}>
                <Badge color="ambar" size="lg" variant="filled" style={{ cursor: "pointer" }}>¡Hoy aplica!</Badge>
              </UnstyledButton>
            )}
            {empiezaDespues && <Badge color="gray" variant="light">Desde {fecha(promo.vigenciaDesde, false)}</Badge>}
          </Group>
          <Text className="cine-display" fw={850} lh={0.9} c="var(--mantine-color-ambar-text)"
            fz={destacada ? "clamp(3.5rem, 2.4rem + 4vw, 6rem)" : "clamp(2.75rem, 2.2rem + 2vw, 3.75rem)"}>
            {promo.beneficio}
          </Text>
          <DiasDeLaSemana promo={promo} />
          <Group gap={6}>
            {promo.horaDesde && (
              <Badge variant="default" tt="none">{promo.horaDesde.slice(0, 5)}–{promo.horaHasta.slice(0, 5)}</Badge>
            )}
            {promo.mediosPago.map((medio) => (
              <Badge key={medio} variant="default" tt="none">{etiqueta(medio)}</Badge>
            ))}
            <Badge variant="default" tt="none">Hasta {fecha(promo.vigenciaHasta, false)}</Badge>
          </Group>
          {conCondiciones && <CondicionesPromo promo={promo} />}
        </Stack>
      </SimpleGrid>
    </Box>
  );
}
