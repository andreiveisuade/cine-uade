import { Badge, Button, Group, Paper, Radio, Stack, Text, Title } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { precio } from "@/compartido/formato.js";
import { iconoMedio } from "@/compartido/iconos.js";
import { MontoAnimado } from "@/compartido/ui/MontoAnimado.jsx";

// Cada medio con lo que se paga con él, ya con su promo: el total lo calcula el backend, la pantalla solo lo muestra.
export function PagoDeLaCompra({ cotizacion, medio, alElegir, actualizando, pagando, error, alPagar }) {
  const elegido = cotizacion.find((c) => c.medio === medio);
  return (
    <Paper withBorder p="lg">
      <Title order={2} fz="h4" mb="sm">¿Con qué pagás?</Title>
      <Radio.Group value={medio} onChange={alElegir}>
        <Stack gap="xs">
          {cotizacion.map((c) => {
            const Icono = iconoMedio(c.medio);
            return (
              <Radio.Card key={c.medio} value={c.medio} p="sm" radius="md">
                <Group justify="space-between" wrap="nowrap" gap="sm">
                  <Group gap="sm" wrap="nowrap">
                    <Radio.Indicator />
                    <Icono size={20} stroke={1.5} aria-hidden />
                    <Stack gap={0}>
                      <Text fw={650}>{etiqueta(c.medio)}</Text>
                      {c.promocion && <Text size="xs" c="dimmed">{c.promocion}, en las entradas</Text>}
                    </Stack>
                  </Group>
                  <Stack gap={0} align="flex-end">
                    <Text fw={800} className="cine-cifras">{precio(c.total)}</Text>
                    {c.descuento > 0 && <Badge color="green" variant="light" size="sm">−{precio(c.descuento)}</Badge>}
                  </Stack>
                </Group>
              </Radio.Card>
            );
          })}
        </Stack>
      </Radio.Group>

      {elegido && (
        <Stack gap={4} mt="md" className="cine-cifras">
          <Group justify="space-between"><Text size="sm">Entradas</Text><Text size="sm">{precio(elegido.entradas)}</Text></Group>
          {elegido.descuento > 0 && (
            <Group justify="space-between" c="green"><Text size="sm">{elegido.promocion}</Text><Text size="sm">−{precio(elegido.descuento)}</Text></Group>
          )}
          {elegido.candy > 0 && <Group justify="space-between"><Text size="sm">Candy</Text><Text size="sm">{precio(elegido.candy)}</Text></Group>}
          <Group justify="space-between" align="baseline" mt={4}>
            <Text fw={650}>Total</Text>
            <Text className="cine-display" fz={34} fw={850} lh={1}><MontoAnimado valor={elegido.total} /></Text>
          </Group>
        </Stack>
      )}

      <Button fullWidth size="md" mt="md" disabled={!elegido || actualizando} loading={pagando} onClick={() => alPagar(medio)}>
        {elegido ? `Pagar ${precio(elegido.total)} con Mercado Pago` : "Pagar con Mercado Pago"}
      </Button>
      <Text size="xs" c="dimmed" mt="xs">
        Te llevamos a Mercado Pago y volvés acá. Si el pago no pasa, probás con otro medio sin perder las butacas.
      </Text>
      {error && <ErrorCaja>{error}</ErrorCaja>}
    </Paper>
  );
}
