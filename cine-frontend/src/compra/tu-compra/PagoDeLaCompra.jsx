import { Box, Button, Group, Stack, Text } from "@mantine/core";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { precio } from "@/compartido/formato.js";
import { LogoMercadoPago } from "@/compartido/ui/LogoMercadoPago.jsx";
import { MontoAnimado } from "@/compartido/ui/MontoAnimado.jsx";

// La web cobra con Mercado Pago: la tarjeta o el saldo se eligen allá. El total lo calcula el backend, la pantalla
// solo lo muestra con su desglose.
export function PagoDeLaCompra({ cotizacion, actualizando, pagando, error, alPagar }) {
  return (
    <Box>
      <Stack gap={4} className="cine-cifras">
        {cotizacion.entradas > 0 && <Group justify="space-between"><Text size="sm">Entradas</Text><Text size="sm">{precio(cotizacion.entradas)}</Text></Group>}
        {cotizacion.descuento > 0 && (
          <Group justify="space-between" c="green"><Text size="sm">{cotizacion.promocion}</Text><Text size="sm">−{precio(cotizacion.descuento)}</Text></Group>
        )}
        {cotizacion.candy > 0 && <Group justify="space-between"><Text size="sm">Candy</Text><Text size="sm">{precio(cotizacion.candy)}</Text></Group>}
        <Group justify="space-between" align="baseline" mt={4}>
          <Text fw={650}>Total</Text>
          <Text className="cine-display" fz={40} fw={850} lh={1} aria-hidden><MontoAnimado valor={cotizacion.total} /></Text>
        </Group>
      </Stack>

      <Button fullWidth size="md" mt="md" h="auto" py={10} disabled={actualizando} loading={pagando} onClick={alPagar}
        leftSection={<LogoMercadoPago size={24} />} styles={{ label: { whiteSpace: "normal", lineHeight: 1.3 } }}>
        {`Pagar ${precio(cotizacion.total)} con Mercado Pago`}
      </Button>
      <Text size="xs" c="dimmed" mt="xs">
        En Mercado Pago elegís tarjeta de crédito, débito o dinero en cuenta, y volvés acá. Si el pago no pasa, probás
        de nuevo sin perder las butacas.
      </Text>
      {error && <ErrorCaja>{error}</ErrorCaja>}
    </Box>
  );
}
