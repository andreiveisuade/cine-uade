import { Grid } from "@mantine/core";
import { Navigate, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { sesionDeCompra } from "@/compra/bloqueo.js";
import { useCompra } from "@/compra/CompraEnCurso.jsx";
import { resumenCompra } from "@/compra/tarifas.js";
import { useRenovarBloqueo } from "@/compra/useRenovarBloqueo.js";
import { DetalleCompra } from "./DetalleCompra.jsx";
import { QuienCompra } from "./QuienCompra.jsx";
import { useReservar } from "./useReservar.js";

// Segundo paso de la compra: el detalle de lo elegido y con qué cuenta se reserva.
export function Confirmar() {
  const { id } = useParams();
  useTitulo("Confirmar reserva");
  const { seleccion } = useCompra();
  const { usuario } = useSesion();
  const carga = useCargar(() => Promise.all([api.obtenerFuncion(id, sesionDeCompra()), api.obtenerTarifas()]), [id]);
  const [funcion, tarifas] = carga.datos || [];
  const { reservar, enviando, error, reservada } = useReservar(funcion?.id);

  // Completar el formulario lleva más de lo que dura un bloqueo.
  useRenovarBloqueo(funcion?.id);

  if (!funcion) return <EsperaOError carga={carga} />;
  // Sin nada elegido para esta función (otra pestaña, un link viejo): volver al mapa.
  if (!reservada && (seleccion.funcionId !== funcion.id || Object.keys(seleccion.butacas).length === 0)) {
    return <Navigate to={`/funcion/${funcion.id}`} replace />;
  }
  // R28: se compra con la cuenta de Google; desde el mapa ya se pasó por el ingreso, esto cubre un link directo.
  if (usuario === null) return <Navigate to="/ingresar" state={{ volver: `/confirmar/${funcion.id}` }} replace />;
  if (usuario === undefined) return <EsperaOError carga={carga} />;
  const { elegidas, total } = resumenCompra(funcion, seleccion.butacas);

  return (
    <div>
      <Encabezado titulo="Confirmar reserva" volver={{ a: `/funcion/${funcion.id}`, texto: "Cambiar butacas" }} />

      <Grid gap="lg" align="flex-start">
        <Grid.Col span={{ base: 12, md: 5 }} order={{ base: 2, md: 1 }}>
          <QuienCompra usuario={usuario} enviando={enviando} error={error} alConfirmar={reservar} />
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }} order={{ base: 1, md: 2 }}>
          <DetalleCompra funcion={funcion} tarifas={tarifas} butacas={seleccion.butacas} elegidas={elegidas} total={total} />
        </Grid.Col>
      </Grid>
    </div>
  );
}
