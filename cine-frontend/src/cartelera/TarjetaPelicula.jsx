import { Box, Group, Text } from "@mantine/core";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";
import { duracion } from "@/api/formato.js";
import { ChipClasificacion } from "@/compartido/ui/Chips.jsx";
import { Poster } from "@/compartido/ui/Poster.jsx";

// Sin caja alrededor: el póster es la tarjeta y debajo va lo justo para decidir.
export function TarjetaPelicula({ pelicula, previa }) {
  return (
    <Box component={Link} to={`/pelicula/${pelicula.id}`} display="block" c="inherit" style={{ textDecoration: "none" }}>
      <Box pos="relative" style={{ borderRadius: "var(--mantine-radius-md)", overflow: "hidden", boxShadow: "var(--cine-sombra)" }}>
        <Poster pelicula={pelicula} />
        {previa}
      </Box>
      <Text fw={650} fz="lg" mt="sm" lh={1.2} lineClamp={2}>{pelicula.titulo}</Text>
      <Group gap={8} mt={6} wrap="nowrap">
        <ChipClasificacion valor={pelicula.clasificacion} />
        <Text size="sm" c="dimmed" lineClamp={1}>
          {[duracion(pelicula.duracionMinutos), ...pelicula.generos.map(etiqueta)].join(" · ")}
        </Text>
      </Group>
    </Box>
  );
}
