import { Alert, Anchor, Badge, Button, Code, Grid, Group, Paper, Skeleton, Stack, Text } from "@mantine/core";
import { m, useReducedMotion } from "motion/react";
import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useParams, useSearchParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { fechaHora, precio, precioExacto } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { PagarOnline } from "./PagarOnline.jsx";
import { useSesion } from "./sesion.jsx";

// Qué le decimos al comprador según el estado que manda el backend; el front no decide vencimientos.
const AVISO_POR_ESTADO = {
  RESERVADA: { color: "green", texto: "Reserva confirmada. Pagala online o en boletería antes de que venza." },
  PAGADA: { color: "green", texto: "Entrada paga. Mostrá el código en la puerta." },
  EXPIRADA: { color: "gray", texto: "Esta reserva venció. Las butacas ya se liberaron." },
  CANCELADA: { color: "gray", texto: "Esta reserva está cancelada." },
};

const CODIGO_POR_ESTADO = {
  RESERVADA: "Todavía no está paga. Sirve una sola vez cuando se pague.",
  PAGADA: "Mostralo en la puerta. Sirve una sola vez.",
  EXPIRADA: "Ya no sirve: la reserva venció.",
  CANCELADA: "Ya no sirve: la reserva se canceló.",
};

// El pago se concilia solo en el backend: mientras espera, se vuelve a pedir para que aparezca "Pagada" sin recargar.
const CADA_MS = 10_000;

const LINEA = "=".repeat(44);

const renglon = (etiquetaTexto, valor) => ` ${etiquetaTexto.padEnd(13)}: ${valor}`;

const centrar = (texto) => " ".repeat(Math.max(Math.floor((LINEA.length - texto.length) / 2), 0)) + texto;

/** Mismo contenido que tickets/ticket-<id>.txt del backend, con las constantes traducidas a su etiqueta. */
function armarTicket(reserva) {
  return [
    LINEA,
    centrar("CINE UADE"),
    centrar("TICKET #" + reserva.id),
    LINEA,
    renglon("Película", reserva.pelicula.titulo),
    renglon("Sala", `${reserva.sala.nombre} (${etiqueta(reserva.sala.tipo)})`),
    renglon("Función", fechaHora(reserva.funcion.inicio)),
    renglon("Formato", `${etiqueta(reserva.funcion.proyeccion)} ${etiqueta(reserva.funcion.idioma).toLowerCase()}`),
    renglon("Cliente", reserva.cliente.nombre),
    LINEA,
    ...reserva.entradas.map((e) => renglon(
      "Butaca " + e.codigo,
      precioExacto(e.precio) + (e.tarifa && e.tarifa !== "GENERAL" ? "  " + etiqueta(e.tarifa) : ""))),
    LINEA,
    renglon("Entradas", String(reserva.entradas.length)),
    renglon("Total", precioExacto(reserva.total)),
    renglon("Estado", etiqueta(reserva.estado)),
    LINEA,
    centrar("CÓDIGO DE ACCESO"),
    centrar(reserva.codigo || ""),
    LINEA,
    centrar("Presentar en boletería"),
    LINEA,
  ].join("\n");
}

// El sello cae solo si el pago se vio llegar en esta pantalla; un ticket que ya abrió pago lo muestra quieto.
const SELLO_CAYENDO = { opacity: 0, scale: 2.2, rotate: -24 };
const SELLO = { opacity: 1, scale: 1, rotate: -8 };

// El ticket sale de la impresora: se destapa de arriba hacia abajo.
const IMPRESION = {
  initial: { clipPath: "inset(0 0 100% 0)" },
  animate: { clipPath: "inset(0 0 0% 0)", transition: { duration: 0.4, ease: "easeOut" } },
};

// No es un QR dibujado: el código en claro se escanea o se tipea igual, sin sumar una librería.
function TarjetaCodigo({ reserva, pagoEnVivo }) {
  if (!reserva.codigo) return null;
  const usada = reserva.ingresadaEn;
  return (
    <Paper withBorder p="lg" ta="center" pos="relative" style={{ borderWidth: 2, opacity: usada ? 0.6 : 1 }}>
      {reserva.estado === "PAGADA" && (
        <Badge component={m.div} initial={pagoEnVivo ? SELLO_CAYENDO : false} animate={SELLO}
          transition={{ type: "spring", stiffness: 400, damping: 18 }}
          color="green" variant="outline" size="lg" pos="absolute" top={12} right={12} style={{ borderWidth: 2 }}>
          Pagada
        </Badge>
      )}
      <Text size="xs" tt="uppercase" c="dimmed" style={{ letterSpacing: "0.15em" }}>Código de acceso</Text>
      <Text ff="monospace" fz={32} fw={700} style={{ letterSpacing: "0.2em", textDecoration: usada ? "line-through" : "none" }}>
        {reserva.codigo.slice(0, 4)} {reserva.codigo.slice(4)}
      </Text>
      <Text size="xs" c="dimmed">
        {usada ? `Ya se usó el ${fechaHora(usada)}` : CODIGO_POR_ESTADO[reserva.estado]}
      </Text>
    </Paper>
  );
}

// Solo para mirar: el candy nace cobrado en el mostrador y no tiene circuito de reserva.
function CartaCandy({ productos, reservaId }) {
  if (!productos.length) return null;
  return (
    <Paper withBorder p="md">
      <Text fw={600}>¿Pochoclos para la función?</Text>
      <Text size="sm" c="dimmed" mb="sm">
        Comprá en el mostrador del candy antes de entrar. Con la reserva ya pagada, si decís su número (#{reservaId}),
        la compra queda a tu nombre.
      </Text>
      <Stack gap={6}>
        {productos.map((p) => (
          <Group key={p.id} justify="space-between" align="baseline" wrap="nowrap">
            <div>
              <Text size="sm">{p.nombre}</Text>
              {p.componentes?.length > 0 && (
                <Text size="xs" c="dimmed">{p.componentes.map((c) => `${c.cantidad}× ${c.nombre}`).join(" + ")}</Text>
              )}
            </div>
            <Text size="sm" fw={500} style={{ whiteSpace: "nowrap" }}>{precio(p.precio)}</Text>
          </Group>
        ))}
      </Stack>
    </Paper>
  );
}

export function Ticket() {
  const { codigo } = useParams();
  const avisar = useAvisar();
  const { usuario } = useSesion();
  // El resultado del pago: la vuelta de Mercado Pago redirige acá con ?aviso= si no se pudo cobrar.
  const aviso = useSearchParams()[0].get("aviso");
  const recien = useLocation().state?.reserva;
  // Recién comprada viene en la navegación; si no (recarga, Mis reservas) se pide por código,
  // que es la única llave del cliente: el id se adivina. La carta es opcional.
  const carga = useCargar(() => Promise.all([
    recien?.codigo === codigo ? recien : api.obtenerReservaPorCodigo(codigo),
    api.obtenerProductosCandy().catch(() => []),
  ]), [codigo]);
  const [actualizada, setActualizada] = useState(null);
  const inicial = carga.datos?.[0];
  const esperandoPago = inicial?.estado === "RESERVADA" && inicial.cobrable
    && !(actualizada?.codigo === codigo && actualizada.estado !== "RESERVADA");

  useEffect(() => {
    if (!esperandoPago) return;
    const intervalo = setInterval(() => {
      api.obtenerReservaPorCodigo(codigo).then(setActualizada, () => {});
    }, CADA_MS);
    return () => clearInterval(intervalo);
  }, [esperandoPago, codigo]);

  const reserva = actualizada?.codigo === codigo ? actualizada : inicial;
  // Lo refrescado solo existe si se estaba esperando el pago: si dice PAGADA, el cambio pasó con la pantalla abierta.
  const pagoEnVivo = actualizada?.codigo === codigo && actualizada.estado === "PAGADA";
  const quieto = useReducedMotion();
  const celebrada = useRef(null);

  useEffect(() => {
    if (!pagoEnVivo || quieto || celebrada.current === codigo) return;
    celebrada.current = codigo;
    // Se baja recién cuando hace falta: casi ningún ticket la usa.
    let vigente = true;
    let confetti;
    import("canvas-confetti").then(({ default: lanzar }) => {
      if (!vigente) return;
      confetti = lanzar;
      lanzar({ particleCount: 120, spread: 75, origin: { y: 0.3 }, disableForReducedMotion: true });
    });
    return () => {
      vigente = false;
      confetti?.reset();
    };
  }, [pagoEnVivo, quieto, codigo]);

  if (!carga.datos) {
    return <EsperaOError carga={carga} esqueleto={<Stack gap="md" maw={640}><Skeleton h={48} /><Skeleton h={130} /><Skeleton h={360} /></Stack>} />;
  }
  const avisoEstado = AVISO_POR_ESTADO[reserva.estado];
  const productos = carga.datos[1];
  const conAcreditacion = reserva.entradas.filter((e) => e.tarifa && e.tarifa !== "GENERAL");

  async function copiar() {
    await navigator.clipboard.writeText(armarTicket(reserva));
    avisar("Comprobante copiado");
  }

  return (
    <Stack gap="md">
      {aviso && <Alert color="yellow" title="El pago no se completó">{aviso}</Alert>}
      {avisoEstado && <Alert color={avisoEstado.color}>{avisoEstado.texto}</Alert>}
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Stack gap="md">
            <TarjetaCodigo reserva={reserva} pagoEnVivo={pagoEnVivo} />
            {conAcreditacion.length > 0 && (
              <Alert color="yellow" title="Traé el carnet">
                En la puerta se acredita la tarifa de{" "}
                {conAcreditacion.map((e) => `${e.codigo} (${etiqueta(e.tarifa).toLowerCase()})`).join(", ")}.
              </Alert>
            )}
            <m.div {...IMPRESION}>
              <Code block fz="xs">{armarTicket(reserva)}</Code>
            </m.div>
            <Group>
              <Button component={Link} to="/">Volver a la cartelera</Button>
              <Button variant="default" onClick={copiar}>Copiar</Button>
            </Group>
            {/* Sin cancelar acá: el ticket es público y se muestra en la puerta; cancelar es del dueño, en Mis reservas. */}
            {usuario && reserva.cobrable && <PagarOnline codigo={reserva.codigo} />}
            {usuario === null && recien && reserva.cobrable && (
              <Text size="sm" c="dimmed">
                Para pagarla online,{" "}
                <Anchor component={Link} to="/registro" state={{ email: reserva.cliente.email }} size="sm">registrate</Anchor>{" "}
                o <Anchor component={Link} to="/ingresar" state={{ email: reserva.cliente.email }} size="sm">ingresá</Anchor>{" "}
                con {reserva.cliente.email}. Si no, se paga en la boletería.
              </Text>
            )}
          </Stack>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 5 }}>
          <CartaCandy productos={productos} reservaId={reserva.id} />
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
