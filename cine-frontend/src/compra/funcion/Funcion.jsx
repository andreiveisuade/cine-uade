import { Grid } from "@mantine/core";
import { useNavigate, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { sesionDeCompra, sostenerSeleccion } from "@/compra/bloqueo.js";
import { useCompra } from "@/compra/CompraEnCurso.jsx";
import { DatosFuncion } from "@/compra/DatosFuncion.jsx";
import { MapaButacas } from "@/compra/mapa/MapaButacas.jsx";
import { Referencia } from "@/compra/mapa/Referencia.jsx";
import { resumenCompra } from "@/compra/tarifas.js";
import { useRenovarBloqueo } from "@/compra/useRenovarBloqueo.js";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { FuncionEsqueleto } from "./FuncionEsqueleto.jsx";
import { pintarParaComprar, REFERENCIAS } from "./pintarParaComprar.js";
import { TuSeleccion } from "./TuSeleccion.jsx";
import { useAlternarButaca } from "./useAlternarButaca.js";
import { useReservar } from "./useReservar.js";

// Primer paso de la compra: elegir butacas en el mapa de la sala.
export function Funcion() {
  const { id } = useParams();
  const navegar = useNavigate();
  const { seleccion } = useCompra();
  const { usuario } = useSesion();
  // Con la sesión, las butacas que uno mismo bloqueó no vuelven como ocupadas.
  const carga = useCargar(() => api.obtenerFuncion(id, sesionDeCompra()), [id]);
  const funcion = carga.datos;
  // Otra función es otra compra: lo elegido para la anterior no se arrastra. Se deriva acá y no con un efecto que
  // la vacíe; el primer clic la reemplaza porque `alternar` escribe la selección entera con este funcionId.
  const butacas = funcion && seleccion.funcionId === funcion.id ? seleccion.butacas : {};
  const alternar = useAlternarButaca(funcion, butacas, carga.recargar);
  const { reservar, enviando, error } = useReservar(funcion?.id, carga.recargar);

  useRenovarBloqueo(funcion?.id);
  useTitulo(funcion && `Butacas de ${funcion.pelicula.titulo}`);

  if (!funcion) return <EsperaOError carga={carga} esqueleto={<FuncionEsqueleto />} />;
  const { elegidas, total } = resumenCompra(funcion, butacas);

  // R28: se reserva con la sesión de Google. Sin ella se ingresa y se vuelve acá con lo elegido; el bloqueo se renueva
  // antes de salir, así la vuelta tiene sus 3 minutos enteros, y si igual se pierde una butaca, reservar da 409.
  async function continuar() {
    if (usuario) return reservar();
    await sostenerSeleccion(funcion.id, butacas).catch(() => {});
    navegar("/ingresar", { state: { volver: `/funcion/${funcion.id}` } });
  }

  return (
    <div>
      <Encabezado titulo={funcion.pelicula.titulo} volver={{ a: `/pelicula/${funcion.peliculaId}`, texto: "Otras funciones" }}>
        <DatosFuncion funcion={funcion} size="md" fw={500} />
      </Encabezado>

      <Grid gap="lg" align="flex-start">
        {/* Selección de ancho fijo: lo que sobra es del mapa, para que la fila más ancha (20 butacas) entre en escritorio. */}
        <Grid.Col span={{ base: 12, lg: "auto" }} miw={0}>
          <MapaButacas sala={funcion.sala} asientos={funcion.asientos}
            pintar={(a) => pintarParaComprar(a, butacas)} alElegir={alternar} />
          <Referencia items={REFERENCIAS} />
        </Grid.Col>

        <Grid.Col span={{ base: 12, lg: "content" }} w={{ lg: 320 }} style={{ position: "sticky", top: 88 }}>
          <TuSeleccion elegidas={elegidas} butacas={butacas} total={total}
            enviando={enviando} error={error} alContinuar={continuar} />
        </Grid.Col>
      </Grid>
    </div>
  );
}
