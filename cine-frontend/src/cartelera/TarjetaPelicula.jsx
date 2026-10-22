import { Box, Group, Text } from "@mantine/core";
import { m } from "motion/react";
import { Link } from "react-router";
import { etiqueta } from "@/api/etiquetas.js";
import { duracion } from "@/api/formato.js";
import { ChipClasificacion } from "@/compartido/ui/Chips.jsx";
import { Poster } from "@/compartido/ui/Poster.jsx";
import { useInclinar } from "./useInclinar.js";
import { useVolarPoster } from "./useVolarPoster.js";

// Sin caja alrededor: el póster es la tarjeta y debajo va lo justo para decidir.
export function TarjetaPelicula({ pelicula, previa }) {
  const volar = useVolarPoster();
  const inclinar = useInclinar();
  const a = `/pelicula/${pelicula.id}`;
  return (
    <Box component={Link} to={a} onClick={volar(pelicula.id, a)} display="block" c="inherit" style={{ textDecoration: "none" }}>
      <Box component={m.div} pos="relative" data-poster={pelicula.id} {...inclinar.eventos} style={{ ...inclinar.estilo, borderRadius: "var(--mantine-radius-md)", overflow: "hidden", boxShadow: "var(--cine-sombra)" }}>
        <Poster pelicula={pelicula} />
        {previa}
        <m.div aria-hidden style={{ position: "absolute", inset: 0, pointerEvents: "none", background: inclinar.reflejo }} />
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
