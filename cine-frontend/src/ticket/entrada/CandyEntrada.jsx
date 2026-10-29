import { Badge, Box, Group, Text } from "@mantine/core";
import { precio } from "@/compartido/formato.js";

// El candy va dentro de la entrada y no en un cupón aparte: en el mostrador se escanea el mismo QR.
export function CandyEntrada({ renglones }) {
  return (
    <Box mt="md" px="md" py="sm" style={{ border: "1px dashed var(--mantine-color-ambar-5)", borderRadius: "var(--mantine-radius-md)" }}>
      <Text size="xs" c="dimmed" tt="uppercase" fw={700} style={{ letterSpacing: "0.06em" }}>Candy a retirar</Text>
      {renglones.map((r) => (
        <Group key={r.clave} justify="space-between" wrap="nowrap" py={6} style={{ borderBottom: "1px dashed var(--cine-borde)" }}>
          <Group gap="xs">
            <Text size="sm">{r.cantidad} × {r.nombre}</Text>
            <Badge size="sm" variant="light" color={r.estado.color}>{r.estado.texto}</Badge>
          </Group>
          <Text size="sm" fw={600} className="cine-cifras">{precio(r.subtotal)}</Text>
        </Group>
      ))}
      <Text size="xs" c="dimmed" pt={6}>Se retira en el candy con el mismo QR de la entrada.</Text>
    </Box>
  );
}
