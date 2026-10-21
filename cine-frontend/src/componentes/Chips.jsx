import { Badge } from "@mantine/core";
import { COLOR_CLASIFICACION, etiqueta } from "../api/etiquetas.js";

export function Chip({ valor, color = "gray", variant = "light" }) {
  return (
    // Sin el mínimo, en una columna angosta de tabla Mantine lo recorta a "A…".
    <Badge color={color} variant={variant} tt="none" miw="max-content">
      {etiqueta(valor)}
    </Badge>
  );
}

// Relleno y no translúcido: con autoContrast el +13 amarillo da AA en claro y en oscuro.
export const ChipClasificacion = ({ valor }) => <Chip valor={valor} color={COLOR_CLASIFICACION[valor]} variant="filled" />;
