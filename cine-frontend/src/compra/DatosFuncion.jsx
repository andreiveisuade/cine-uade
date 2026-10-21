import { Text } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { dia, hora } from "@/api/formato.js";

/** Cuándo, dónde y en qué formato: el encabezado de la función en el mapa y en la confirmación. */
export function DatosFuncion({ funcion, ...props }) {
  return (
    <Text size="sm" {...props}>
      {dia(funcion.inicio)} {hora(funcion.inicio)} · {funcion.sala.nombre} ({etiqueta(funcion.sala.tipo)}) ·{" "}
      {etiqueta(funcion.proyeccion)} · {etiqueta(funcion.idioma)}
    </Text>
  );
}
