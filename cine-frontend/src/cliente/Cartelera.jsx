import { Box, Button, Card, Group, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { duracion } from "../api/formato.js";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { useCargar } from "../componentes/useCargar.js";

// Portada de la home: nombre, propuesta, CTA a la cartelera y hasta 3 destacadas de la misma lista.
function Hero({ peliculas }) {
  const destacadas = peliculas.slice(0, 3);
  return (
    <Box p={{ base: "lg", sm: "xl" }} bg="var(--mantine-color-default-hover)"
      style={{ borderRadius: "var(--mantine-radius-md)" }}>
      <Group justify="space-between" align="center" gap="xl" wrap="wrap">
        <Stack gap="sm" maw={460}>
          <Title order={1} fz={{ base: 32, sm: 44 }} lh={1.1}>CINE UADE</Title>
          <Text size="lg">Tu cine de barrio: salas 2D y 3D, candy y entradas online.</Text>
          <div>
            <Button size="md"
              onClick={() => document.getElementById("cartelera")?.scrollIntoView({ behavior: "smooth" })}>
              Ver cartelera
            </Button>
          </div>
        </Stack>
        {destacadas.length > 0 && (
          <Group gap="md" wrap="nowrap">
            {destacadas.map((p) => (
              <Box key={p.id} component={Link} to={`/pelicula/${p.id}`} w={{ base: 80, xs: 110, sm: 130 }}
                style={{ textDecoration: "none", color: "inherit" }}>
                <Poster pelicula={p} />
                <Text size="xs" fw={600} mt={4} lh={1.2} lineClamp={2}>{p.titulo}</Text>
              </Box>
            ))}
          </Group>
        )}
      </Group>
    </Box>
  );
}

export function Cartelera({ conHero = false }) {
  const { genero } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerCartelera(genero), api.obtenerGeneros()]), [genero]);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [peliculas, generos] = carga.datos;

  return (
    <Stack gap="lg">
      {conHero && <Hero peliculas={peliculas} />}
      <div id="cartelera">
        {conHero ? <Title order={2}>Cartelera</Title> : <Title order={1}>Cartelera</Title>}
        <Text c="dimmed" size="sm">
          {peliculas.length} película{peliculas.length === 1 ? "" : "s"}{" "}
          {genero ? `de ${etiqueta(genero).toLowerCase()}` : "en cartel"}
        </Text>
      </div>

      <Group gap={6}>
        <Button component={Link} to="/cartelera" size="compact-sm" radius="xl" variant={genero ? "default" : "filled"}>
          Todos
        </Button>
        {generos.map((g) => (
          <Button key={g} component={Link} to={`/cartelera/${g}`} size="compact-sm" radius="xl"
            variant={g === genero ? "filled" : "default"}>
            {etiqueta(g)}
          </Button>
        ))}
      </Group>

      {peliculas.length ? (
        <SimpleGrid cols={{ base: 2, sm: 3, md: 4 }} spacing="md">
          {peliculas.map((p) => (
            <Card key={p.id} component={Link} to={`/pelicula/${p.id}`} withBorder padding="sm">
              <Card.Section>
                <Poster pelicula={p} />
              </Card.Section>
              <Text fw={600} mt="sm" lh={1.2}>{p.titulo}</Text>
              <Group gap={6} mt={6}>
                <ChipClasificacion valor={p.clasificacion} />
                <Text size="xs" c="dimmed">{duracion(p.duracionMinutos)}</Text>
              </Group>
              <Group gap={4} mt={6}>
                {p.generos.map((g) => <Chip key={g} valor={g} />)}
              </Group>
            </Card>
          ))}
        </SimpleGrid>
      ) : (
        <Vacio>No hay películas de ese género en cartelera.</Vacio>
      )}
    </Stack>
  );
}
