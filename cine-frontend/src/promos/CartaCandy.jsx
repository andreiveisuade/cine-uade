import { Stack } from "@mantine/core";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { Destacados } from "@/compartido/layout/Destacados.jsx";
import { Grilla } from "@/compartido/layout/Grilla.jsx";
import { TarjetaCombo } from "@/promos/tarjetas/TarjetaCombo.jsx";
import { TarjetaProducto } from "@/promos/tarjetas/TarjetaProducto.jsx";

// Los combos primero y destacados; los sueltos, en grilla.
export function CartaCandy({ productos }) {
  const combos = productos.filter((p) => p.esCombo);
  const sueltos = productos.filter((p) => !p.esCombo);
  if (productos.length === 0) return <Vacio>La carta del candy está vacía por ahora.</Vacio>;
  return (
    <Stack gap="xl">
      <Destacados items={combos} clave={(c) => c.id} min={360}
        render={(combo, destacada) => <TarjetaCombo combo={combo} destacada={destacada} />} />
      {sueltos.length > 0 && (
        <Grilla min="clamp(150px, 15vw, 220px)" gap="var(--mantine-spacing-md)">
          {sueltos.map((p) => <TarjetaProducto key={p.id} producto={p} />)}
        </Grilla>
      )}
    </Stack>
  );
}
