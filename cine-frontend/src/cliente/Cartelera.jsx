import { Button, Card, Group, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { duracion } from "../api/formato.js";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { useCargar } from "../componentes/useCargar.js";

export function Cartelera() {
  const { genero } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerCartelera(genero), api.obtenerGeneros()]), [genero]);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [peliculas, generos] = carga.datos;

  return (
    <Stack gap="lg">
      <div>
        <Title order={1}>Cartelera</Title>
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
