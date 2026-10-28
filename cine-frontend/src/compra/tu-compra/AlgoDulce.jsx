import { Group, Mark, Stack, Text, Title } from "@mantine/core";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { Grilla } from "@/compartido/layout/Grilla.jsx";
import { TarjetaCombo } from "@/promos/tarjetas/TarjetaCombo.jsx";
import { TarjetaProducto } from "@/promos/tarjetas/TarjetaProducto.jsx";
import { Contador } from "./Contador.jsx";

// El candy de la carta, combos primero: se paga con la entrada y se retira con el mismo QR (R30).
export function AlgoDulce({ productos, candy }) {
  const combos = productos.filter((p) => p.esCombo);
  const sueltos = productos.filter((p) => !p.esCombo);
  if (!productos.length) return null;

  const contador = (p) => (
    <Contador nombre={p.nombre} cantidad={candy.cantidades[p.id] ?? 0} maximo={candy.maximo}
      alCambiar={(cuanto) => candy.cambiar(p.id, cuanto)} />
  );

  return (
    <Stack component="section" gap="md" aria-label="Candy para sumar">
      <div>
        <Title order={2} className="cine-display" fz="clamp(1.9rem, 1.4rem + 2vw, 2.8rem)">
          ¡Agregale <Mark color="ambar" px={4}>algo dulce</Mark>!
        </Title>
        <Text c="dimmed" mt={4}>Lo pagás con la entrada y lo retirás en el candy con el mismo QR, sin hacer la fila.</Text>
      </div>
      {candy.error && <ErrorCaja>{candy.error}</ErrorCaja>}
      {combos.length > 0 && (
        <Grilla min={260} gap="var(--mantine-spacing-md)">
          {combos.map((c) => (
            <Stack key={c.id} gap="xs">
              <TarjetaCombo combo={c} />
              <Group justify="flex-end">{contador(c)}</Group>
            </Stack>
          ))}
        </Grilla>
      )}
      {sueltos.length > 0 && (
        <Grilla min="clamp(150px, 15vw, 200px)" gap="var(--mantine-spacing-md)">
          {sueltos.map((p) => (
            <Stack key={p.id} gap="xs">
              <TarjetaProducto producto={p} />
              <Group justify="flex-end">{contador(p)}</Group>
            </Stack>
          ))}
        </Grilla>
      )}
    </Stack>
  );
}
