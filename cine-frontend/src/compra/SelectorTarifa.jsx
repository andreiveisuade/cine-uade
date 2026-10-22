import { NativeSelect } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";

// Las tarifas que se ofrecen son las que el backend le puso precio a esa butaca.
export function SelectorTarifa({ asiento, valor, alCambiar }) {
  return (
    <NativeSelect size="xs" value={valor} onChange={(e) => alCambiar(e.currentTarget.value)}
      data={Object.keys(asiento.precios).map((t) => ({ value: t, label: etiqueta(t) }))} />
  );
}
