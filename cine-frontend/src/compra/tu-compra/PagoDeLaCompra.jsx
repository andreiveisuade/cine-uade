import { Badge, Box, Button, Group, Radio, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { precio } from "@/compartido/formato.js";
import { iconoMedio } from "@/compartido/iconos.js";
import { MontoAnimado } from "@/compartido/ui/MontoAnimado.jsx";
import clases from "./Candy.module.css";

// Cada medio con lo que se paga con él, ya con su promo: el total lo calcula el backend, la pantalla solo lo muestra.
export function PagoDeLaCompra({ cotizacion, medio, alElegir, actualizando, pagando, error, alPagar }) {
  const elegido = cotizacion.find((c) => c.medio === medio);
  return (
    <Box>
      <Title order={3} fz="h4" mb="sm">¿Con qué pagás?</Title>
      <Radio.Group value={medio} onChange={alElegir}>
        {/* De a dos cuando entran: los cuatro medios y el botón de pagar quedan a la vista sin bajar. */}
        <SimpleGrid type="container" cols={{ base: 1, "330px": 2 }} spacing="xs">
          {cotizacion.map((c) => {
            const Icono = iconoMedio(c.medio);
            return (
              <Radio.Card key={c.medio} value={c.medio} px="sm" py="xs" radius="md" className={clases.medio}>
                <div className={clases.medioFila}>
                  <Group gap="xs" wrap="nowrap">
                    <Radio.Indicator size="sm" />
                    <Icono size={18} stroke={1.5} aria-hidden />
                    <Text fw={650} size="sm">{etiqueta(c.medio)}</Text>
                  </Group>
                  <Group gap={6} align="center" wrap="nowrap">
                    <Text fw={800} className="cine-cifras">{precio(c.total)}</Text>
                    {c.descuento > 0 && <Badge color="green" variant="light" size="sm">−{precio(c.descuento)}</Badge>}
                  </Group>
                </div>
                {c.promocion && <Text size="xs" c="dimmed">{c.promocion}, en las entradas</Text>}
              </Radio.Card>
            );
          })}
        </SimpleGrid>
      </Radio.Group>

      {elegido && (
        <Stack gap={4} mt="md" className="cine-cifras">
          {elegido.entradas > 0 && <Group justify="space-between"><Text size="sm">Entradas</Text><Text size="sm">{precio(elegido.entradas)}</Text></Group>}
          {elegido.descuento > 0 && (
            <Group justify="space-between" c="green"><Text size="sm">{elegido.promocion}</Text><Text size="sm">−{precio(elegido.descuento)}</Text></Group>
          )}
          {elegido.candy > 0 && <Group justify="space-between"><Text size="sm">Candy</Text><Text size="sm">{precio(elegido.candy)}</Text></Group>}
          <Group justify="space-between" align="baseline" mt={4}>
            <Text fw={650}>Total</Text>
            <Text className="cine-display" fz={40} fw={850} lh={1} aria-hidden><MontoAnimado valor={elegido.total} /></Text>
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
    </Box>
  );
}
