import { useState } from "react";
import { Anchor, Button, Card, Group, SimpleGrid, Stack, Text } from "@mantine/core";
import { Link } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { duracion } from "../api/formato.js";
import { ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Encabezado, useAccion } from "./comun.jsx";

/**
 * Una tarjeta y no una fila de tabla: para decidir hay que ver de qué se trata, y eso es
 * el poster y la sinopsis. En una tabla de siete columnas la sinopsis no entra, y sin
 * sinopsis la decisión se toma leyendo un título suelto.
 *
 * Los dos botones quedan deshabilitados mientras la llamada viaja: sin eso, dos clics
 * seguidos mandan confirmar y descartar sobre la misma película.
 */
function Tarjeta({ pelicula, accion }) {
  const [decidiendo, setDecidiendo] = useState(false);
  const decidir = async (pedido, mensaje) => {
    setDecidiendo(true);
    await accion(pedido, mensaje);
    setDecidiendo(false);
  };
  return (
    <Card withBorder padding="sm" style={{ opacity: decidiendo ? 0.6 : 1 }}>
      <Group align="flex-start" wrap="nowrap" gap="sm">
        <Poster pelicula={pelicula} w={88} />
        <div style={{ minWidth: 0 }}>
          <Text fw={600} lh={1.2}>{pelicula.titulo}</Text>
          <Text size="xs" c="dimmed" mt={4}>{pelicula.anio || "—"} · {duracion(pelicula.duracionMinutos)}</Text>
          <Text size="xs" c="dimmed">{pelicula.director || "Sin director"}</Text>
          <Group gap={6} mt={6}>
            <ChipClasificacion valor={pelicula.clasificacion} />
            {pelicula.generos.map((g) => <Text key={g} size="xs" c="dimmed">{etiqueta(g)}</Text>)}
          </Group>
        </div>
      </Group>
      <Text size="xs" mt="sm" lineClamp={6}>{pelicula.sinopsis || "Sin sinopsis."}</Text>
      <Group mt="auto" pt="sm" grow>
        <Button size="xs" disabled={decidiendo}
          onClick={() => decidir(() => api.confirmarPelicula(pelicula.id), `${pelicula.titulo} confirmada: ya se puede programar`)}>
          Confirmar
        </Button>
        <Button size="xs" variant="default" disabled={decidiendo}
          onClick={() => decidir(() => api.descartarPelicula(pelicula.id), `${pelicula.titulo} descartada`)}>
          Descartar
        </Button>
      </Group>
    </Card>
  );
}

/**
 * Lo que trajo el importador de TMDB y todavía nadie miró.
 *
 * Es una pantalla aparte de Películas y no un filtro más de aquella lista, por lo mismo
 * que estadoRevision es un campo aparte de enCartelera: acá no se está administrando el
 * catálogo, se está decidiendo qué entra.
 */
export function Pendientes() {
  const carga = useCargar(api.obtenerPeliculasPendientes, []);
  const accion = useAccion(carga.recargar);
  if (!carga.datos) return <EsperaOError carga={carga} />;

  return (
    <>
      <Encabezado titulo="Por revisar">
        Lo que trajo el importador de TMDB y todavía nadie miró. Hasta que las confirmes no se pueden programar ni las ve
        el cliente. Lo que descartes queda descartado: el importador no lo vuelve a proponer.
      </Encabezado>
      {carga.datos.length === 0 ? (
        <Card withBorder p="xl">
          <Stack align="center" gap="xs">
            <Text size="sm" c="dimmed">No hay nada esperando. Cuando el importador traiga títulos nuevos van a aparecer acá.</Text>
            <Anchor component={Link} to="/importador" size="sm">Traer cartelera ahora</Anchor>
          </Stack>
        </Card>
      ) : (
        <SimpleGrid cols={{ base: 1, sm: 2, xl: 3 }}>
          {carga.datos.map((p) => <Tarjeta key={p.id} pelicula={p} accion={accion} />)}
        </SimpleGrid>
      )}
    </>
  );
}
