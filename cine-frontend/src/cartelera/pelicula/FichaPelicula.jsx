import { Button, Group, Stack, Text, Title } from "@mantine/core";
import { duracion } from "@/compartido/formato.js";
import { Chip, ChipClasificacion } from "@/compartido/ui/Chips.jsx";

export function FichaPelicula({ pelicula, hayFunciones }) {
  return (
    <Stack gap="sm">
      <Title order={1} fz="clamp(2.5rem, 1.8rem + 2.8vw, 4.25rem)" lh={0.95}>{pelicula.titulo}</Title>
      <Group gap="sm">
        <ChipClasificacion valor={pelicula.clasificacion} />
        <Text size="sm" c="dimmed">
          {[pelicula.anio || null, duracion(pelicula.duracionMinutos), pelicula.idiomaOriginal || null].filter(Boolean).join(" · ")}
        </Text>
      </Group>
      <Group gap={6}>{pelicula.generos.map((g) => <Chip key={g} valor={g} />)}</Group>
      {pelicula.director && <Text size="sm">Dirección: {pelicula.director}</Text>}
      {pelicula.sinopsis && <Text maw="62ch" mt="xs" lh={1.6}>{pelicula.sinopsis}</Text>}
      {hayFunciones && (
        <Button size="md" mt="sm" w="fit-content" hiddenFrom="md"
          onClick={() => document.getElementById("funciones")?.scrollIntoView({ behavior: "smooth" })}>
          Comprar entradas
        </Button>
      )}
    </Stack>
  );
}
