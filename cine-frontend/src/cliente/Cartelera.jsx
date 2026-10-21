import { useRef, useState } from "react";
import { Box, Button, Group, Skeleton, Stack } from "@mantine/core";
import { useMediaQuery } from "@mantine/hooks";
import { AnimatePresence, m } from "motion/react";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { Encabezado } from "../componentes/Encabezado.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Grilla } from "../componentes/Grilla.jsx";
import { Seccion } from "../componentes/Seccion.jsx";
import { TarjetaPelicula } from "../componentes/TarjetaPelicula.jsx";
import { urlTrailer } from "../componentes/Trailer.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { useTitulo } from "../componentes/useTitulo.js";
import { Portada } from "./Portada.jsx";

// Con mouse, quedarse sobre una tarjeta muestra el trailer muteado arriba del póster; en touch no hay hover.
const ESPERA_PREVIA = 600;

function usePrevia() {
  const conMouse = useMediaQuery("(hover: hover) and (pointer: fine)");
  const [actual, setActual] = useState(null);
  const espera = useRef(null);
  function entrar(pelicula) {
    if (!conMouse || !pelicula.trailerYoutube) return;
    clearTimeout(espera.current);
    espera.current = setTimeout(() => setActual(pelicula.id), ESPERA_PREVIA);
  }
  function salir() {
    clearTimeout(espera.current);
    setActual(null);
  }
  return { actual, entrar, salir };
}

function Previa({ id }) {
  return (
    <Box pos="absolute" inset={0} style={{ pointerEvents: "none", background: "black", overflow: "hidden" }}>
      {/* Un 16:9 a la altura del póster 2:3 mide 8/3 del ancho: centrado, recorta los costados. */}
      <iframe src={urlTrailer(id, { controles: false })} title="Previa del trailer" tabIndex={-1} aria-hidden
        allow="autoplay; encrypted-media" style={{ border: 0, height: "100%", width: "267%", marginLeft: "-83.5%" }} />
    </Box>
  );
}

// Entran escalonadas por posición; al filtrar, las que quedan se corren a su lugar nuevo con `layout`.
const TARJETA = {
  oculta: { opacity: 0, y: 12 },
  visible: (orden) => ({ opacity: 1, y: 0, transition: { duration: 0.25, delay: Math.min(orden, 8) * 0.04 } }),
  saliente: { opacity: 0, scale: 0.95, transition: { duration: 0.15 } },
};

function CarteleraEsqueleto() {
  return (
    <Stack gap="lg">
      <Skeleton h={36} w={220} />
      <Grilla min={220}>
        {Array.from({ length: 8 }, (_, i) => <Skeleton key={i} radius="md" style={{ aspectRatio: "2 / 3" }} />)}
      </Grilla>
    </Stack>
  );
}

function Generos({ generos, genero }) {
  return (
    <Group gap={6} wrap="nowrap" style={{ overflowX: "auto", scrollbarWidth: "none" }} aria-label="Filtrar por género">
      {["", ...generos].map((g) => (
        <Button key={g || "todos"} component={Link} to={g ? `/cartelera/${g}` : "/cartelera"} size="compact-sm" radius="xl"
          variant={(genero || "") === g ? "filled" : "default"} style={{ flexShrink: 0 }}>
          {g ? etiqueta(g) : "Todos"}
        </Button>
      ))}
    </Group>
  );
}

export function Cartelera({ conHero = false }) {
  const { genero } = useParams();
  useTitulo(conHero ? undefined : "Cartelera");
  const previa = usePrevia();
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
        <Box mb="lg" mt={conHero ? undefined : "-md"}><Generos generos={generos} genero={genero} /></Box>
        {peliculas.length ? (
          <Grilla min="clamp(150px, 20vw, 260px)" gap="var(--mantine-spacing-xl) var(--mantine-spacing-lg)">
            <AnimatePresence mode="popLayout" initial={false}>
              {peliculas.map((p, orden) => (
                <m.div key={p.id} layout custom={orden} variants={TARJETA} initial="oculta" animate="visible" exit="saliente"
                  whileHover={{ y: -4 }} transition={{ layout: { duration: 0.25 }, y: { duration: 0.15 } }}
                  onMouseEnter={() => previa.entrar(p)} onMouseLeave={previa.salir}>
                  <TarjetaPelicula pelicula={p} previa={previa.actual === p.id && <Previa id={p.trailerYoutube} />} />
                </m.div>
              ))}
            </AnimatePresence>
          </Grilla>
        ) : (
          <Vacio accion={<Button component={Link} to="/cartelera" variant="default">Ver toda la cartelera</Button>}>
            No hay películas de ese género en cartelera.
          </Vacio>
        )}
      </Box>
    </Stack>
  );
}
