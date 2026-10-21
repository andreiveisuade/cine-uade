import { Group, Tooltip } from "@mantine/core";
import { etiqueta } from "../api/etiquetas.js";
import { iconoMedio } from "../api/iconos.js";

const NOTA = { EFECTIVO: "en la boletería" };

// En fila y sin cajas: el ícono dice el medio y el tooltip (y el lector de pantalla) su nombre.
export function MediosDePago({ medios, tamanio = 26 }) {
  return (
    <Group gap={tamanio < 22 ? "sm" : "md"} component="ul" p={0} m={0} style={{ listStyle: "none" }} aria-label="Medios de pago">
      {medios.map((medio) => {
        const Icono = iconoMedio(medio);
        const texto = NOTA[medio] ? `${etiqueta(medio)}, ${NOTA[medio]}` : etiqueta(medio);
        return (
          <Tooltip key={medio} label={texto} withArrow>
            <li aria-label={texto} tabIndex={0} style={{ display: "flex", color: "var(--mantine-color-dimmed)" }}>
              <Icono size={tamanio} stroke={1.5} aria-hidden />
            </li>
          </Tooltip>
        );
      })}
    </Group>
  );
}
