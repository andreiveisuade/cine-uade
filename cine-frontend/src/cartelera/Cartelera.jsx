import { Box, Button, Stack } from "@mantine/core";
import { Link, useParams, useSearchParams } from "react-router";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { cantidad } from "@/compartido/formato.js";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { Seccion } from "@/compartido/layout/Seccion.jsx";
import { CarteleraEsqueleto } from "./CarteleraEsqueleto.jsx";
import { FiltrosCartelera } from "./FiltrosCartelera.jsx";
import { GrillaPeliculas } from "./GrillaPeliculas.jsx";
import { Portada } from "@/cartelera/portada/Portada.jsx";

// La home es la cartelera con la portada arriba.
export function Cartelera({ conHero = false }) {
  const { genero } = useParams();
  useTitulo(conHero ? undefined : "Cartelera");
  const [consulta] = useSearchParams();
  const tipo = consulta.get("tipo") || undefined;
  const carga = useCargar(
    () => Promise.all([api.obtenerCartelera(genero, tipo), api.obtenerGeneros(), api.obtenerTiposPelicula()]),
    [genero, tipo],
  );
  // Mientras llega otro filtro se queda la lista anterior: las tarjetas se reordenan en vez de pasar por el esqueleto.
  const datos = carga.datos ?? carga.previos;
  if (!datos) return <EsperaOError carga={carga} esqueleto={<CarteleraEsqueleto />} />;
  const [peliculas, generos, tipos] = datos;
  const filtros = [genero && `de ${etiqueta(genero).toLowerCase()}`, tipo && `en ${etiqueta(tipo)}`].filter(Boolean).join(" ");
  const cuenta = `${cantidad(peliculas.length, "película")} ${filtros || "en cartel"}`;
  const Cabeza = conHero ? Seccion : Encabezado;

  return (
    <Stack gap="var(--cine-seccion)">
      {conHero && <Portada peliculas={peliculas} />}
      <Box id="cartelera">
        <Cabeza titulo="Cartelera" bajada={cuenta}>{null}</Cabeza>
        <Box mb="lg" mt={conHero ? undefined : "-md"}><FiltrosCartelera generos={generos} genero={genero} tipos={tipos} tipo={tipo} /></Box>
        {peliculas.length ? (
          <GrillaPeliculas peliculas={peliculas} />
        ) : genero || tipo ? (
          <Vacio accion={<Button component={Link} to="/cartelera" variant="default">Ver toda la cartelera</Button>}>
            {tipo ? "No hay películas con ese filtro en cartelera." : "No hay películas de ese género en cartelera."}
          </Vacio>
        ) : (
          <Vacio>Todavía no hay películas en cartelera.</Vacio>
        )}
      </Box>
    </Stack>
  );
}
