import { Badge } from "@mantine/core";
import { COLOR_CLASIFICACION, etiqueta } from "../api/etiquetas.js";

export function Chip({ valor, color = "gray" }) {
  return (
    // Sin el mínimo, en una columna angosta de tabla Mantine lo recorta a "A…".
    <Badge color={color} variant="light" tt="none" miw="max-content">
      {etiqueta(valor)}
    </Badge>
  );
}

export const ChipClasificacion = ({ valor }) => <Chip valor={valor} color={COLOR_CLASIFICACION[valor]} />;
