import { Box, Grid, Stack, Title } from "@mantine/core";
import { useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Poster } from "@/compartido/ui/Poster.jsx";
import { FichaPelicula } from "./FichaPelicula.jsx";
import { ListaFunciones } from "./ListaFunciones.jsx";
import { PantallaTrailer } from "./PantallaTrailer.jsx";
import { PeliculaEsqueleto } from "./PeliculaEsqueleto.jsx";

// Trailer arriba; debajo póster, ficha y funciones para elegir.
export function Pelicula() {
  const { id } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerPelicula(id), api.obtenerFuncionesDePelicula(id)]), [id]);
  useTitulo(carga.datos?.[0].titulo);
  if (!carga.datos) return <EsperaOError carga={carga} esqueleto={<PeliculaEsqueleto />} />;
  const [pelicula, funciones] = carga.datos;

  return (
    <Stack gap="xl">
      <PantallaTrailer pelicula={pelicula} />
      <Grid gap={{ base: "lg", md: "xl" }}>
        <Grid.Col span={{ base: 12, sm: 3 }} visibleFrom="sm">
          <Box style={{ borderRadius: "var(--mantine-radius-md)", overflow: "hidden", boxShadow: "var(--cine-sombra)" }}>
            <Poster pelicula={pelicula} />
          </Box>
        </Grid.Col>
        <Grid.Col span={{ base: 12, sm: 9, md: 5 }}>
          <FichaPelicula pelicula={pelicula} hayFunciones={funciones.length > 0} />
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 4 }} id="funciones">
          <Box pos={{ md: "sticky" }} top={88}>
            <Title order={2} fz="h3" mb="md">Funciones</Title>
            <ListaFunciones funciones={funciones} />
          </Box>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
