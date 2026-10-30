import { Badge } from "@mantine/core";
import { COLOR_CLASIFICACION, etiqueta } from "@/api/etiquetas.js";

export function Chip({ valor, color = "gray", variant = color === "gray" ? "default" : "light", ...props }) {
  return (
    // Sin el mínimo, en una columna angosta de tabla Mantine lo recorta a "A…".
    <Badge color={color} variant={variant} tt="none" miw="max-content" {...props}>
      {etiqueta(valor)}
    </Badge>
  );
}

// Relleno y con texto negro: el autoContrast le ponía blanco al +18 en claro (2,8:1); negro da AA en los cuatro.
export const ChipClasificacion = ({ valor }) => <Chip valor={valor} color={COLOR_CLASIFICACION[valor]} variant="filled" c="black" />;
