import { Box, Button, Stack } from "@mantine/core";
import { Link, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { Seccion } from "@/compartido/layout/Seccion.jsx";
import { CarteleraEsqueleto } from "./CarteleraEsqueleto.jsx";
import { FiltroGeneros } from "./FiltroGeneros.jsx";
import { GrillaPeliculas } from "./GrillaPeliculas.jsx";
import { Portada } from "@/cartelera/portada/Portada.jsx";

// La home es la cartelera con la portada arriba.
export function Cartelera({ conHero = false }) {
  const { genero } = useParams();
  useTitulo(conHero ? undefined : "Cartelera");
  const carga = useCargar(() => Promise.all([api.obtenerCartelera(genero), api.obtenerGeneros()]), [genero]);
  // Mientras llega otro género se queda la lista anterior: las tarjetas se reordenan en vez de pasar por el esqueleto.
  const datos = carga.datos ?? carga.previos;
  if (!datos) return <EsperaOError carga={carga} esqueleto={<CarteleraEsqueleto />} />;
  const [peliculas, generos] = datos;
  const cuenta = `${peliculas.length} película${peliculas.length === 1 ? "" : "s"} ${genero ? `de ${etiqueta(genero).toLowerCase()}` : "en cartel"}`;
  const Cabeza = conHero ? Seccion : Encabezado;

  return (
    <Stack gap="var(--cine-seccion)">
      {conHero && <Portada peliculas={peliculas} />}
      <Box id="cartelera">
        <Cabeza titulo="Cartelera" bajada={cuenta}>{null}</Cabeza>
        <Box mb="lg" mt={conHero ? undefined : "-md"}><FiltroGeneros generos={generos} genero={genero} /></Box>
        {peliculas.length ? (
          <GrillaPeliculas peliculas={peliculas} />
        ) : (
          <Vacio accion={<Button component={Link} to="/cartelera" variant="default">Ver toda la cartelera</Button>}>
            No hay películas de ese género en cartelera.
          </Vacio>
        )}
      </Box>
    </Stack>
  );
}
