import { Box, Button, Grid, Group, Skeleton, Stack, Text, Title, UnstyledButton } from "@mantine/core";
import { m } from "motion/react";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, duracion, hora, porDia, precio } from "../api/formato.js";
import { Banda } from "../componentes/Banda.jsx";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { Trailer } from "../componentes/Trailer.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { useTitulo } from "../componentes/useTitulo.js";
import { Volver } from "../componentes/Volver.jsx";

function PeliculaEsqueleto() {
  return (
    <Stack gap="xl">
      <Skeleton h={{ base: 220, sm: 480 }} radius="lg" />
      <Grid gap="xl">
        <Grid.Col span={{ base: 4, md: 3 }}><Skeleton radius="md" style={{ aspectRatio: "2 / 3" }} /></Grid.Col>
        <Grid.Col span={{ base: 8, md: 5 }}>
          <Stack gap="sm"><Skeleton h={44} w="70%" /><Skeleton h={14} w="40%" /><Skeleton h={90} mt="xs" /></Stack>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}

// La sala a oscuras con el trailer en pantalla. Sin trailer, el póster difuminado ocupa su lugar.
function Pantalla({ pelicula }) {
  const fondo = pelicula.posterUrl && (
    <img src={pelicula.posterUrl} alt="" data-testid="fondo-poster"
      style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover", zIndex: -1,
               filter: "blur(28px) brightness(0.45)", transform: "scale(1.2)" }} />
  );
  return (
    <Banda mt="calc(-1 * var(--cine-gutter))" fondo={fondo}>
      <Box py={{ base: "md", sm: "xl" }} mih={pelicula.trailerYoutube ? undefined : { base: 160, sm: 280 }}>
        <Volver a="/">Cartelera</Volver>
        {pelicula.trailerYoutube && (
          <m.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.5, ease: "easeOut" }}
            style={{ maxWidth: 1100, margin: "var(--mantine-spacing-md) auto 0" }}>
            <Trailer id={pelicula.trailerYoutube} titulo={pelicula.titulo} radio="lg" />
          </m.div>
        )}
      </Box>
    </Banda>
  );
}

function Ficha({ pelicula, hayFunciones }) {
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

// Un horario es un botón: la hora manda, el resto la acompaña.
function Horario({ funcion }) {
  return (
    <UnstyledButton component={Link} to={`/funcion/${funcion.id}`} p="sm"
      style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12,
               border: "1px solid var(--cine-borde)", borderRadius: "var(--mantine-radius-md)", background: "var(--cine-superficie)" }}>
      <Group gap="md" wrap="nowrap">
        <Text className="cine-display cine-cifras" fz={30} fw={850} lh={1}>{hora(funcion.inicio)}</Text>
        <Stack gap={2}>
          <Text size="sm" fw={600}>{funcion.sala.nombre} · {etiqueta(funcion.sala.tipo)}</Text>
          <Text size="xs" c="dimmed">{etiqueta(funcion.proyeccion)} · {etiqueta(funcion.idioma)}</Text>
        </Stack>
      </Group>
      <Stack gap={0} align="flex-end">
        <Text size="xs" c="dimmed">desde</Text>
        <Text fw={700} className="cine-cifras">{precio(funcion.precioDesde)}</Text>
      </Stack>
    </UnstyledButton>
  );
}

function Funciones({ funciones }) {
  if (!funciones.length) return <Vacio>No hay funciones programadas.</Vacio>;
  return (
    <Stack gap="lg">
      {porDia(funciones).map(([clave, delDia]) => (
        <Stack key={clave} gap="xs">
          <Text size="sm" fw={700} tt="uppercase" c="dimmed" style={{ letterSpacing: "0.04em" }}>{dia(delDia[0].inicio)}</Text>
          {delDia.map((f) => <Horario key={f.id} funcion={f} />)}
        </Stack>
      ))}
    </Stack>
  );
}

export function Pelicula() {
  const { id } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerPelicula(id), api.obtenerFuncionesDePelicula(id)]), [id]);
  useTitulo(carga.datos?.[0].titulo);
  if (!carga.datos) return <EsperaOError carga={carga} esqueleto={<PeliculaEsqueleto />} />;
  const [pelicula, funciones] = carga.datos;

  return (
    <Stack gap="xl">
      <Pantalla pelicula={pelicula} />
      <Grid gap={{ base: "lg", md: "xl" }}>
        <Grid.Col span={{ base: 12, sm: 3 }} visibleFrom="sm">
          <Box style={{ borderRadius: "var(--mantine-radius-md)", overflow: "hidden", boxShadow: "var(--cine-sombra)" }}>
            <Poster pelicula={pelicula} />
          </Box>
        </Grid.Col>
        <Grid.Col span={{ base: 12, sm: 9, md: 5 }}>
          <Ficha pelicula={pelicula} hayFunciones={funciones.length > 0} />
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 4 }} id="funciones">
          <Box pos={{ md: "sticky" }} top={88}>
            <Title order={2} fz="h3" mb="md">Funciones</Title>
            <Funciones funciones={funciones} />
          </Box>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
