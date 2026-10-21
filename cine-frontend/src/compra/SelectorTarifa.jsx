import { NativeSelect } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { tarifasCargadas } from "./tarifas.js";

export function SelectorTarifa({ valor, alCambiar }) {
  return (
    <NativeSelect size="xs" value={valor} onChange={(e) => alCambiar(e.currentTarget.value)}
      data={tarifasCargadas().map((t) => ({ value: t.nombre, label: etiqueta(t.nombre) }))} />
  );
}
