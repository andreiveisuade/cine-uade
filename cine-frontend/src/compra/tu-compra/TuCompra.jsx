import { useState } from "react";
import { Alert, Badge, Button, Grid, Stack } from "@mantine/core";
import { Link, Navigate, useNavigate, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { hora } from "@/compartido/formato.js";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { avisoDe } from "@/ticket/estadoReserva.js";
import { AlgoDulce } from "./AlgoDulce.jsx";
import { EntradasDeLaCompra } from "./EntradasDeLaCompra.jsx";
import { PagoDeLaCompra } from "./PagoDeLaCompra.jsx";
import { useCandyDeLaCompra } from "./useCandyDeLaCompra.js";
import { usePagar, useVueltaDelPago } from "./usePagar.js";

// Una sola pantalla después del mapa: lo elegido a modo de confirmación, el candy para sumar y el pago.
export function TuCompra() {
  const { codigo } = useParams();
  useTitulo("Tu compra");
  const { usuario } = useSesion();
  const carga = useCargar(() => Promise.all([
    api.obtenerReservaPorCodigo(codigo),
    api.obtenerProductosCandy().catch(() => []),
  ]), [codigo]);
  useVueltaDelPago(codigo, carga.recargar);

  // R21: la cotización, el candy y el pago son del dueño, con su sesión.
  if (usuario === null) return <Navigate to="/ingresar" state={{ volver: `/compra/${codigo}` }} replace />;
  if (!carga.datos || usuario === undefined) return <EsperaOError carga={carga} />;
  const [reserva, productos] = carga.datos;
  if (reserva.estado === "PAGADA") return <Navigate to={`/ticket/${codigo}`} replace />;
  if (!reserva.cobrable) return <CompraTerminada reserva={reserva} />;
  return <Compra reserva={reserva} productos={productos} />;
}

function Compra({ reserva, productos }) {
  const navegar = useNavigate();
  const avisar = useAvisar();
  const cotizacion = useCargar(() => api.obtenerCotizacion(reserva.codigo), [reserva.codigo]);
  const candy = useCandyDeLaCompra(reserva, cotizacion.recargar);
  const [medio, setMedio] = useState(null);
  const { pagar, pagando, error } = usePagar(reserva.codigo);
  const [abandonando, setAbandonando] = useState(false);

  // Si no eligió, el que menos cuesta: no se acumulan promos y va la que más conviene (R15).
  const renglones = cotizacion.datos ?? [];
  const elegido = medio ?? renglones.reduce((barato, c) => (!barato || c.total < barato.total ? c : barato), null)?.medio;

  async function abandonar() {
    setAbandonando(true);
    try {
      await api.cancelarReservaPorCodigo(reserva.codigo);
      avisar("Compra abandonada: las butacas quedaron libres");
      navegar(`/funcion/${reserva.funcionId}`);
    } catch (e) {
      avisar(e.message, "error");
      setAbandonando(false);
    }
  }

  return (
    <div>
      <Encabezado titulo="Tu compra">
        <Badge color="ambar" variant="light" size="lg" tt="none" className="cine-cifras" style={{ alignSelf: "flex-start" }}>
          Tus butacas te esperan hasta las {hora(reserva.venceEn)}
        </Badge>
      </Encabezado>
      <Grid gap="lg" align="flex-start">
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Stack gap="xl">
            <EntradasDeLaCompra reserva={reserva} />
            <AlgoDulce productos={productos} candy={candy} />
          </Stack>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 5 }} style={{ position: "sticky", top: 88 }}>
          <Stack gap="xs">
            {cotizacion.datos
              ? <PagoDeLaCompra cotizacion={renglones} medio={elegido} alElegir={setMedio}
                  actualizando={candy.actualizando || cotizacion.cargando} pagando={pagando} error={error} alPagar={pagar} />
              : <EsperaOError carga={cotizacion} />}
            <Button variant="subtle" color="red" loading={abandonando} onClick={abandonar} style={{ alignSelf: "flex-start" }}>
              Abandonar la compra
            </Button>
          </Stack>
        </Grid.Col>
      </Grid>
    </div>
  );
}

// Vencida, cancelada o con la función empezada: no hay nada que pagar, y se vuelve a elegir.
function CompraTerminada({ reserva }) {
  const motivo = ["EXPIRADA", "CANCELADA"].includes(reserva.estado)
    ? avisoDe(reserva)
    : "La función ya empezó: esta compra ya no se puede pagar.";
  return (
    <div>
      <Encabezado titulo="Tu compra" />
      <Alert color="gray" title="Esta compra ya no se puede pagar" maw="40rem">
        {motivo}
        <Button component={Link} to={`/pelicula/${reserva.pelicula.id}`} variant="default" mt="md" display="block">
          Elegir otra función
        </Button>
      </Alert>
    </div>
  );
}
