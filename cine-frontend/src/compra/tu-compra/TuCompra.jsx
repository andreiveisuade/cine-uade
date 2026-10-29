import { useRef, useState } from "react";
import { Alert, Badge, Button, Grid, Stack, Text, VisuallyHidden } from "@mantine/core";
import { useInViewport, useMergedRef } from "@mantine/hooks";
import { Link, Navigate, useNavigate, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { hora, precio } from "@/compartido/formato.js";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { avisoDe } from "@/ticket/estadoReserva.js";
import { AlgoDulce } from "./AlgoDulce.jsx";
import { BarraDelPedido } from "./BarraDelPedido.jsx";
import { EntradasDeLaCompra } from "./EntradasDeLaCompra.jsx";
import { PagoDeLaCompra } from "./PagoDeLaCompra.jsx";
import { TuPedido } from "./TuPedido.jsx";
import { useCandyDeLaCompra } from "./useCandyDeLaCompra.js";
import { usePagar, useVueltaDelPago } from "./usePagar.js";

// Una sola pantalla después del mapa: lo elegido a modo de confirmación, el candy para sumar y el pago. Con la entrada
// ya paga es la misma, solo para el candy (R30).
export function TuCompra() {
  const { codigo } = useParams();
  useTitulo("Tu compra");
  const navegar = useNavigate();
  const { usuario } = useSesion();
  const carga = useCargar(() => Promise.all([
    api.obtenerReservaPorCodigo(codigo),
    api.obtenerProductosCandy().catch(() => []),
  ]), [codigo]);
  // El ticket cuenta cómo terminó: cobrado, todavía esperando a MP o con el motivo del rechazo.
  useVueltaDelPago(codigo, () => navegar(`/ticket/${codigo}`, { replace: true }));

  // R21: la cotización, el candy y el pago son del dueño, con su sesión.
  if (usuario === null) return <Navigate to="/ingresar" state={{ volver: `/compra/${codigo}` }} replace />;
  if (!carga.datos || usuario === undefined) return <EsperaOError carga={carga} />;
  const [reserva, productos] = carga.datos;
  if (reserva.ingresadaEn) return <Navigate to={`/ticket/${codigo}`} replace />;
  const sumar = reserva.estado === "PAGADA";
  if (!sumar && !reserva.cobrable) return <CompraTerminada reserva={reserva} />;
  return <Compra reserva={reserva} productos={productos} sumar={sumar} />;
}

// Nombre y precio de cada producto del pedido: de la carta, o del pedido guardado si ya no está a la venta.
function catalogoDe(productos, reserva) {
  const pendiente = (reserva.candy ?? []).find((c) => c.estado === "PENDIENTE");
  const guardados = (pendiente?.items ?? []).map((i) => [i.productoId, { nombre: i.nombre, precio: i.precioUnitario, ahorro: 0 }]);
  return new Map([...guardados, ...productos.map((p) => [p.id, p])]);
}

function Compra({ reserva, productos, sumar }) {
  const navegar = useNavigate();
  const avisar = useAvisar();
  const [guardados, setGuardados] = useState(0);
  const candy = useCandyDeLaCompra(reserva, () => setGuardados((n) => n + 1), (mensaje) => avisar(mensaje, "error"));
  // Paga y sin pedido no hay nada que cotizar: el backend daría 409.
  const cotiza = !sumar || candy.hayPedido;
  const cotizacion = useCargar(() => (cotiza ? api.obtenerCotizacion(reserva.codigo) : Promise.resolve([])),
    [reserva.codigo, cotiza, guardados]);
  const [medio, setMedio] = useState(null);
  const { pagar, pagando, error } = usePagar(reserva.codigo);
  const [abandonando, setAbandonando] = useState(false);
  const { ref: mirarPedido, inViewport: pedidoALaVista } = useInViewport();
  const pedidoRef = useRef(null);

  const catalogo = catalogoDe(productos, reserva);
  const items = Object.entries(candy.cantidades).map(([id, cantidad]) => ({ id: Number(id), cantidad, ...catalogo.get(Number(id)) }));
  const pedido = {
    items,
    total: items.reduce((t, i) => t + i.precio * i.cantidad, 0),
    ahorro: items.reduce((t, i) => t + (i.ahorro ?? 0) * i.cantidad, 0),
  };

  // Si no eligió, el que menos cuesta: no se acumulan promos y va la que más conviene (R15). Mientras el pedido viaja
  // y hasta que llega la cotización nueva, cada medio muestra ya el candy nuevo (la promo no lo toca).
  const cotizados = cotizacion.datos ?? cotizacion.previos ?? [];
  const renglones = candy.actualizando || cotizacion.cargando
    ? cotizados.map((c) => ({ ...c, candy: pedido.total, total: c.entradas - c.descuento + pedido.total }))
    : cotizados;
  const elegido = medio ?? renglones.reduce((barato, c) => (!barato || c.total < barato.total ? c : barato), null)?.medio;
  const total = renglones.find((c) => c.medio === elegido)?.total ?? pedido.total;

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

  const delPedido = { reserva, sumar, pedido, candy };
  const talon = !cotiza ? (
    <Text size="sm">Elegí algo del candy y pagalo acá: lo retirás con el QR de tu entrada.</Text>
  ) : renglones.length ? (
    <PagoDeLaCompra cotizacion={renglones} medio={elegido} alElegir={setMedio}
      actualizando={candy.actualizando || cotizacion.cargando} pagando={pagando} error={error} alPagar={pagar} />
  ) : <EsperaOError carga={cotizacion} />;

  return (
    <div>
      <Encabezado titulo={sumar ? "Sumá candy a tu entrada" : "Tu compra"}>
        {!sumar && (
          <Badge color="ambar" variant="light" size="lg" tt="none" className="cine-cifras" style={{ alignSelf: "flex-start" }}>
            Tus butacas te esperan hasta las {hora(reserva.venceEn)}
          </Badge>
        )}
      </Encabezado>
      <Grid gap="lg" align="flex-start">
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Stack gap="xl">
            <EntradasDeLaCompra reserva={reserva} />
            <AlgoDulce productos={productos} candy={candy} />
          </Stack>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 5 }} style={{ position: "sticky", top: 88 }}>
          <Stack gap="xs" ref={useMergedRef(pedidoRef, mirarPedido)} style={{ scrollMarginTop: 80 }}>
            <TuPedido {...delPedido} talon={talon} />
            <VisuallyHidden role="status" aria-live="polite">{`Total ${precio(total)}`}</VisuallyHidden>
            {sumar ? (
              <Button variant="subtle" component={Link} to={`/ticket/${reserva.codigo}`} style={{ alignSelf: "flex-start" }}>
                Volver a la entrada
              </Button>
            ) : (
              <Button variant="subtle" color="red" loading={abandonando} onClick={abandonar} style={{ alignSelf: "flex-start" }}>
                Abandonar la compra
              </Button>
            )}
          </Stack>
        </Grid.Col>
      </Grid>
      <BarraDelPedido {...delPedido} total={total} visible={!pedidoALaVista}
        alIrAPagar={() => pedidoRef.current?.scrollIntoView({ behavior: "smooth", block: "start" })} />
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
