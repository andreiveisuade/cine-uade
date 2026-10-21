import { Badge, Group } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { corre, DIAS, hoyEnLaSemana } from "@/promos/diasPromo.js";

export function DiasDeLaSemana({ promo }) {
  const hoy = hoyEnLaSemana();
  return (
    <Group gap={4} wrap="nowrap" aria-label="Días">
      {DIAS.map((dia) => {
        const prendido = corre(promo, dia);
        return (
          <Badge key={dia} size="md" radius="sm" tt="none" w={34} px={0}
            variant={prendido ? "filled" : "default"} color={dia === hoy && prendido ? "ambar" : "dark"}
            style={{ opacity: prendido ? 1 : 0.45 }} aria-label={`${etiqueta(dia)}${prendido ? "" : " (no aplica)"}`}>
            {etiqueta(dia).slice(0, 2)}
          </Badge>
        );
      })}
    </Group>
  );
}
