import { useState } from "react";
import { Skeleton, Stack } from "@mantine/core";
import * as api from "@/api/api-http.js";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { Seccion } from "@/compartido/layout/Seccion.jsx";
import { CartaCandy } from "./CartaCandy.jsx";
import { FiltroDias } from "./FiltroDias.jsx";
import { ListaPromos } from "./ListaPromos.jsx";

// Las promos de entradas filtradas por día y, debajo, la carta del candy.
export function Promociones() {
  useTitulo("Promos");
  const [dia, setDia] = useState("TODOS");
  const promos = useCargar(() => api.obtenerPromocionesVigentes(), []);
  const carta = useCargar(() => api.obtenerProductosCandy(), []);

  return (
    <Stack gap="var(--cine-seccion)">
      <div>
        <Encabezado titulo="Promos" bajada="No se acumulan: va la que más te conviene." />
        <Stack gap="lg" mt="-md">
          <FiltroDias dia={dia} alCambiar={setDia} />
          {promos.datos
            ? <ListaPromos promos={promos.datos} dia={dia} />
            : <EsperaOError carga={promos} esqueleto={<Skeleton h={360} radius="lg" />} />}
        </Stack>
      </div>
      <Seccion titulo="Candy" bajada="Se compra en el mostrador antes de entrar a la sala.">
        {carta.datos
          ? <CartaCandy productos={carta.datos} />
          : <EsperaOError carga={carta} esqueleto={<Skeleton h={260} radius="lg" />} />}
      </Seccion>
    </Stack>
  );
}
