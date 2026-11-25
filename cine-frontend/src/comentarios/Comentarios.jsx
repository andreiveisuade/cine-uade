import { useRef } from "react";
import { Button, Skeleton, Stack } from "@mantine/core";
import * as api from "@/api/api-http.js";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { Seccion } from "@/compartido/layout/Seccion.jsx";
import { FormularioComentario } from "./FormularioComentario.jsx";
import { ListaComentarios } from "./ListaComentarios.jsx";
import { ResumenPuntajes } from "./ResumenPuntajes.jsx";

// Lo que opinan los clientes y, debajo, el formulario para sumar una reseña.
export function Comentarios() {
  useTitulo("Comentarios");
  const formulario = useRef(null);
  const carga = useCargar(() => api.obtenerComentarios(), []);

  return (
    <Stack gap="var(--cine-seccion)" maw={720} mx="auto" pt={{ base: "md", sm: "xl" }}>
      <div>
        <Encabezado titulo="Comentarios" bajada="Lo que dicen quienes vinieron al cine."
          acciones={<Button variant="default" onClick={() => formulario.current.scrollIntoView({ block: "center" })}>Dejar una reseña</Button>} />
        {!carga.datos && <EsperaOError carga={carga} esqueleto={<Skeleton h={360} radius="lg" />} />}
        {carga.datos?.length === 0 && <Vacio>Todavía no hay comentarios: dejá el primero</Vacio>}
        {carga.datos?.length > 0 && (
          <Stack gap="xl">
            <ResumenPuntajes comentarios={carga.datos} />
            <ListaComentarios comentarios={carga.datos} />
          </Stack>
        )}
      </div>
      <Seccion titulo="Dejá tu reseña">
        <div ref={formulario}><FormularioComentario alPublicar={carga.recargar} /></div>
      </Seccion>
    </Stack>
  );
}
