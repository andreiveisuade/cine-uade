import { Chip, Group } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { DIAS } from "./diasPromo.js";

export function FiltroDias({ dia, alCambiar }) {
  return (
    <Chip.Group value={dia} onChange={alCambiar}>
      <Group gap={6} aria-label="Filtrar por día">
        <Chip value="TODOS" size="sm">Todos</Chip>
        {DIAS.map((d) => <Chip key={d} value={d} size="sm">{etiqueta(d).slice(0, 3)}</Chip>)}
      </Group>
    </Chip.Group>
  );
}
