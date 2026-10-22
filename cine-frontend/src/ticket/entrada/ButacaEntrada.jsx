import { createElement } from "react";
import { Badge, Group, Text } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/compartido/formato.js";
import { iconoTarifa } from "@/compartido/iconos.js";
import { conAcreditacion } from "@/ticket/estadoReserva.js";

// La tarifa reducida lleva su ícono y la nota del carnet al lado de la butaca, no en un aviso aparte.
export function ButacaEntrada({ entrada }) {
  return (
    <Group gap="sm" wrap="nowrap" justify="space-between" py={8} style={{ borderBottom: "1px dashed var(--cine-borde)" }}>
      <Group gap="sm" wrap="nowrap">
        <Badge size="lg" variant="default" radius="sm" className="cine-cifras" miw={52}>{entrada.codigo}</Badge>
        {conAcreditacion(entrada) ? (
          <Group gap={6} wrap="nowrap">
            {createElement(iconoTarifa(entrada.tarifa), { size: 18, stroke: 1.75, "aria-hidden": true, color: "var(--mantine-color-ambar-text)" })}
            <Text size="sm" fw={600}>{etiqueta(entrada.tarifa)}</Text>
            <Text size="xs" c="dimmed">· traé el carnet</Text>
          </Group>
        ) : (
          <Text size="sm" c="dimmed">{etiqueta(entrada.tarifa || "GENERAL")}</Text>
        )}
      </Group>
      <Text size="sm" fw={600} className="cine-cifras">{precio(entrada.precio)}</Text>
    </Group>
  );
}
