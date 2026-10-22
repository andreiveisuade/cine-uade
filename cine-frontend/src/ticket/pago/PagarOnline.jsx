import { Button, Group, NativeSelect, Paper, Stack, Text } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/compartido/formato.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { usePagarOnline } from "./usePagarOnline.js";

// Elegir el medio, abrir el checkout y recién ahí ir a la pasarela: el monto con la promo del medio lo calcula
// el backend, y se muestra antes de salir del sitio.
export function PagarOnline({ codigo, alVolver }) {
  const { medios, medio, setMedio, checkout, pagando, enviando, error, empezar, abrir, irAPagar, yaPague, cambiarMedio }
    = usePagarOnline(codigo, alVolver);

  if (!medios) {
    return (
      <Stack gap="xs">
        <Button loading={enviando} onClick={empezar}>Pagar online</Button>
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
    );
  }

  return (
    <Paper withBorder p="sm">
      <Stack gap="xs">
        {pagando ? (
          <>
            <Text size="sm">
              Estás pagando <strong>{precio(checkout.monto)}</strong> en Mercado Pago. Al terminar te trae de vuelta
              acá y el ticket se actualiza solo.
            </Text>
            <Group gap="xs">
              <Button onClick={yaPague}>Ya pagué</Button>
              <Button variant="default" onClick={irAPagar}>Abrir Mercado Pago</Button>
              <Button variant="subtle" onClick={cambiarMedio}>Cambiar medio</Button>
            </Group>
          </>
        ) : checkout ? (
          <>
            <Text size="sm">
              Vas a pagar <strong>{precio(checkout.monto)}</strong> con {etiqueta(checkout.medio).toLowerCase()}.
            </Text>
            <Group gap="xs">
              <Button onClick={irAPagar}>Ir a pagar</Button>
              <Button variant="default" onClick={cambiarMedio}>Cambiar medio</Button>
            </Group>
          </>
        ) : (
          <Group gap="xs" align="flex-end">
            <NativeSelect label="Medio de pago" value={medio} onChange={(e) => setMedio(e.currentTarget.value)}
              data={medios.map((m) => ({ value: m.nombre, label: etiqueta(m.nombre) }))} />
            <Button loading={enviando} disabled={!medio} onClick={abrir}>Continuar</Button>
          </Group>
        )}
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
    </Paper>
  );
}
