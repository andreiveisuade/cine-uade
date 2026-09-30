import { Card, Grid, Group, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, duracion, hora, porDia, precio } from "../api/formato.js";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Volver } from "../componentes/Volver.jsx";

export function Pelicula() {
  const { id } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerPelicula(id), api.obtenerFuncionesDePelicula(id)]), [id]);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [pelicula, funciones] = carga.datos;

  return (
    <Stack gap="lg">
      <Volver a="/">Cartelera</Volver>
      <Grid gap="xl">
        <Grid.Col span={{ base: 12, xs: 4, md: 3 }}>
          <Poster pelicula={pelicula} />
        </Grid.Col>
        <Grid.Col span={{ base: 12, xs: 8, md: 9 }}>
          <Stack gap="xs">
            <Title order={1}>{pelicula.titulo}</Title>
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
          </Stack>
        </Grid.Col>
      </Grid>

      <Title order={2} size="h3">Funciones</Title>
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
