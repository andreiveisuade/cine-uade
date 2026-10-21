import { Button, Group, Paper, ScrollArea, Text, Title } from "@mantine/core";
import { precio } from "@/api/formato.js";
import { MontoAnimado } from "@/compartido/ui/MontoAnimado.jsx";
import { SelectorTarifa } from "@/compra/SelectorTarifa.jsx";
import { precioConTarifa } from "@/compra/tarifas.js";
import { useCambiarTarifa } from "@/compra/useCambiarTarifa.js";

// Lo elegido con su tarifa y el total, al lado del mapa.
export function TuSeleccion({ elegidas, butacas, total, alContinuar }) {
  const cambiarTarifa = useCambiarTarifa();
  return (
    <Paper withBorder p="lg">
      <Title order={2} fz="h4" mb="sm">Tu selección</Title>
      {elegidas.length ? (
        <ScrollArea.Autosize mah={280}>
          {elegidas.map((a) => (
            <Group key={a.codigo} justify="space-between" py={6} wrap="nowrap"
              style={{ borderBottom: "1px solid var(--cine-borde)" }}>
              <Text fw={700} className="cine-cifras">{a.codigo}</Text>
              <Group gap="xs" wrap="nowrap">
                <SelectorTarifa valor={butacas[a.codigo]} alCambiar={(t) => cambiarTarifa(a.codigo, t)} />
                <Text size="sm" w={80} ta="right" className="cine-cifras">{precio(precioConTarifa(a, butacas[a.codigo]))}</Text>
              </Group>
            </Group>
          ))}
        </ScrollArea.Autosize>
      ) : (
        <Text size="sm" c="dimmed">Elegí una o más butacas</Text>
      )}
      <Group justify="space-between" align="baseline" mt="md" mb="md">
        <Text size="sm" fw={600}>
          {elegidas.length ? `${elegidas.length} butaca${elegidas.length > 1 ? "s" : ""}` : "Total"}
        </Text>
        <Text className="cine-display cine-cifras" fz={34} fw={850} lh={1}><MontoAnimado valor={total} /></Text>
      </Group>
      <Button fullWidth size="md" disabled={!elegidas.length} onClick={alContinuar}>
        Continuar
      </Button>
    </Paper>
  );
}
