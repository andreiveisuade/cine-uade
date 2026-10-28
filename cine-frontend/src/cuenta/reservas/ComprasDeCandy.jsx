import { Badge, Group, Paper, Stack, Text, Title } from "@mantine/core";
import * as api from "@/api/api-http.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { fechaHora, precio } from "@/compartido/formato.js";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { estadoDelCandy } from "@/ticket/estadoReserva.js";

// CU-16: lo cobrado del candy, con o sin entrada, y si ya se retiró.
export function ComprasDeCandy() {
  const carga = useCargar(() => api.obtenerMisComprasDeCandy(), []);
  if (carga.error) return <ErrorCaja>{carga.error}</ErrorCaja>;
  if (!carga.datos?.length) return null;
  return (
    <Stack component="section" gap="sm" maw={880} mt="xl" aria-label="Tus compras del candy">
      <Title order={2} fz="h3">Tus compras del candy</Title>
      {carga.datos.map((c) => {
        const estado = estadoDelCandy(c);
        return (
          <Paper key={c.id} withBorder p="md">
            <Group justify="space-between" gap="sm">
              <Stack gap={2}>
                <Text fw={650}>{c.items.map((i) => `${i.cantidad} × ${i.nombre}`).join(" · ")}</Text>
                <Text size="sm" c="dimmed" className="cine-cifras">{fechaHora(c.cobradaEn)} · {precio(c.total)}</Text>
              </Stack>
              <Badge color={estado.color} variant="light">{estado.texto}</Badge>
            </Group>
          </Paper>
        );
      })}
    </Stack>
  );
}
