import { Box, Button, Card, Grid, Group, SimpleGrid, Skeleton, Stack, Text, Title } from "@mantine/core";
import { m, useReducedMotion, useScroll, useTransform } from "motion/react";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, duracion, hora, porDia, precio } from "../api/formato.js";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { Trailer } from "../componentes/Trailer.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Volver } from "../componentes/Volver.jsx";

function PeliculaEsqueleto() {
  return (
    <Grid gap="xl" mt={36}>
      <Grid.Col span={{ base: 12, xs: 4, md: 3 }}>
        <Skeleton style={{ aspectRatio: "2 / 3" }} />
      </Grid.Col>
      <Grid.Col span={{ base: 12, xs: 8, md: 9 }}>
        <Stack gap="sm">
          <Skeleton h={36} w="60%" />
          <Skeleton h={14} w="40%" />
          <Skeleton h={60} w="80%" mt="xs" />
        </Stack>
      </Grid.Col>
    </Grid>
  );
}

// Sin trailer, el póster ocupa el lugar del video agrandado y difuminado: la ficha no arranca con un hueco.
function Fondo({ pelicula }) {
  return (
    <Box h={{ base: 180, sm: 320 }} pos="relative"
      style={{ borderRadius: "var(--mantine-radius-lg)", overflow: "hidden", background: "var(--mantine-color-dark-8)" }}>
      {pelicula.posterUrl && (
        <img src={pelicula.posterUrl} alt="" data-testid="fondo-poster"
          style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover", filter: "blur(18px) brightness(0.7)", transform: "scale(1.2)" }} />
      )}
      <Box pos="absolute" inset={0}
        style={{ background: "linear-gradient(to top, var(--mantine-color-body), transparent 70%)" }} />
    </Box>
  );
}

function Ficha({ pelicula, hayFunciones, sobreElFondo }) {
  const quieto = useReducedMotion();
  const { scrollY } = useScroll();
  // El póster sube más lento que la página: da profundidad sin moverse del lugar.
  const y = useTransform(scrollY, [0, 600], [0, quieto ? 0 : -60]);
  return (
    <Grid gap="xl" mt={sobreElFondo ? { base: -60, sm: -100 } : 0} pos="relative">
      <Grid.Col span={{ base: 5, xs: 4, md: 3 }}>
        <m.div style={{ y, boxShadow: "0 18px 40px -16px rgba(0,0,0,0.6)", borderRadius: "var(--mantine-radius-sm)" }}
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.4, delay: 0.15 }}>
          <Poster pelicula={pelicula} />
        </m.div>
      </Grid.Col>
      <Grid.Col span={{ base: 12, xs: 8, md: 9 }} pt={sobreElFondo ? { base: "xs", xs: 70, sm: 110 } : undefined}>
        <m.div initial={{ opacity: 0, x: -16 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.4, delay: 0.25 }}>
          <Stack gap="xs">
            <Title order={1} fz={{ base: 30, sm: 44 }} lh={1.05}>{pelicula.titulo}</Title>
            <Text size="sm" c="dimmed">
              {[pelicula.anio || null, duracion(pelicula.duracionMinutos), pelicula.idiomaOriginal || null]
                .filter(Boolean).join(" · ")}
            </Text>
            {pelicula.director && <Text size="sm">Dirección: {pelicula.director}</Text>}
            <Group gap={6}>
              <ChipClasificacion valor={pelicula.clasificacion} />
              {pelicula.generos.map((g) => <Chip key={g} valor={g} />)}
            </Group>
            {pelicula.sinopsis && <Text size="sm" maw="65ch" mt="xs">{pelicula.sinopsis}</Text>}
            {hayFunciones && (
              <div>
                <Button size="md" radius="xl" mt="sm"
                  onClick={() => document.getElementById("funciones")?.scrollIntoView({ behavior: "smooth" })}>
                  Comprar entradas
                </Button>
              </div>
            )}
          </Stack>
        </m.div>
      </Grid.Col>
    </Grid>
  );
}

export function Pelicula() {
  const { id } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerPelicula(id), api.obtenerFuncionesDePelicula(id)]), [id]);
  if (!carga.datos) return <EsperaOError carga={carga} esqueleto={<PeliculaEsqueleto />} />;
  const [pelicula, funciones] = carga.datos;

  return (
    <Stack gap="lg">
      <Volver a="/">Cartelera</Volver>
      {pelicula.trailerYoutube
        ? <m.div initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.5 }}>
          <Trailer id={pelicula.trailerYoutube} titulo={pelicula.titulo} radio="lg" />
        </m.div>
        : <Fondo pelicula={pelicula} />}
      <Ficha pelicula={pelicula} hayFunciones={funciones.length > 0} sobreElFondo={!pelicula.trailerYoutube} />

      <Title id="funciones" order={2} size="h3">Funciones</Title>
      {funciones.length ? (
        porDia(funciones).map(([clave, delDia]) => (
          <div key={clave}>
            <Text size="sm" fw={700} tt="uppercase" c="dimmed" mb="xs">{dia(delDia[0].inicio)}</Text>
            <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="sm">
              {delDia.map((f) => (
                <Card key={f.id} component={Link} to={`/funcion/${f.id}`} withBorder padding="sm">
                  <Group justify="space-between" align="flex-start" wrap="nowrap">
                    <div>
                      <Text fw={700} fz="lg">{hora(f.inicio)}</Text>
                      <Text size="sm" c="dimmed">{f.sala.nombre} · {etiqueta(f.sala.tipo)}</Text>
                      <Group gap={4} mt={6}>
                        <Chip valor={f.proyeccion} color="indigo" />
                        <Chip valor={f.idioma} color="yellow" />
                      </Group>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <Text size="xs" c="dimmed">desde</Text>
                      <Text fw={600}>{precio(f.precioDesde)}</Text>
                    </div>
                  </Group>
                </Card>
              ))}
            </SimpleGrid>
          </div>
        ))
      ) : (
        <Vacio>No hay funciones programadas.</Vacio>
      )}
    </Stack>
  );
}
