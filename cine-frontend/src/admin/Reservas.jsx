import { useState } from "react";
import { Alert, Anchor, Button, Code, Grid, Group, Paper, Select, Stack, Table, Text, TextInput, Title } from "@mantine/core";
import { useDebouncedValue } from "@mantine/hooks";
import { Link, useNavigate, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, fechaHora, hora, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ChipEstado } from "../componentes/Chips.jsx";
import { ErrorCaja, EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Volver } from "../componentes/Volver.jsx";
import { BarraFiltros, Encabezado, FilaVacia, Nota, opcionesDe, useAccion } from "./comun.jsx";

/**
 * Los estados en el orden en que le importan a quien atiende: primero lo que hay que
 * cobrar hoy, al final lo que ya no se toca.
 */
const ESTADOS = ["RESERVADA", "PAGADA", "EXPIRADA", "CANCELADA"];

const SIN_FILTROS = { q: "", estado: "", dia: "" };

export function Reservas() {
  const [filtros, setFiltros] = useState(SIN_FILTROS);
  // Con espera, porque cada tecla sería un pedido. Los selects no la necesitan: un cambio
  // es una decisión, no un tanteo.
  const [q] = useDebouncedValue(filtros.q, 200);
  const base = useCargar(api.obtenerReservas, []);
  const visibles = useCargar(() => api.obtenerReservas({ ...filtros, q }), [q, filtros.estado, filtros.dia]);
  const accion = useAccion(() => { base.recargar(); visibles.recargar(); });

  if (!base.datos) return <EsperaOError carga={base} />;
  const reservas = base.datos;
  const lista = visibles.datos || reservas;
  const activas = reservas.filter((r) => r.estado !== "CANCELADA");
  const aCobrar = reservas.filter((r) => r.estado === "RESERVADA");

  return (
    <>
      <Encabezado titulo="Reservas">
        {reservas.length} reservas · {activas.length} activas · {aCobrar.length} pendientes de cobro
        {aCobrar.length ? ` (${precio(aCobrar.reduce((s, r) => s + r.total, 0))})` : ""}
      </Encabezado>

      <BarraFiltros visibles={lista.length} total={reservas.length} alLimpiar={() => setFiltros(SIN_FILTROS)}>
        <TextInput label="Buscar" type="search" w={280} placeholder="cliente, email, película o butaca" value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.currentTarget.value })} />
        <Select label="Estado" w={160} value={filtros.estado} onChange={(v) => setFiltros({ ...filtros, estado: v || "" })}
          data={[{ value: "", label: "Todos" }, ...opcionesDe(ESTADOS, etiqueta)]} />
        <TextInput label="Función del día" type="date" value={filtros.dia}
          onChange={(e) => setFiltros({ ...filtros, dia: e.currentTarget.value })} />
      </BarraFiltros>

      <Paper withBorder>
        <Table.ScrollContainer minWidth={760}>
          <Table verticalSpacing="xs" highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>#</Table.Th><Table.Th>Función</Table.Th><Table.Th>Cliente</Table.Th><Table.Th>Butacas</Table.Th>
                <Table.Th ta="right">Total</Table.Th><Table.Th>Estado</Table.Th><Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {lista.length === 0 && <FilaVacia columnas={7}>Ninguna reserva coincide con el filtro.</FilaVacia>}
              {lista.map((r) => (
                <Table.Tr key={r.id} style={{ opacity: r.estado === "CANCELADA" ? 0.55 : 1 }}>
                  <Table.Td>{r.id}</Table.Td>
                  <Table.Td>
                    <Text size="sm">{r.pelicula?.titulo || "—"}</Text>
                    {r.funcion && <Text size="xs" c="dimmed">{dia(r.funcion.inicio)} {hora(r.funcion.inicio)} · {r.sala.nombre}</Text>}
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm">{r.cliente?.nombre || "—"}</Text>
                    <Text size="xs" c="dimmed">{r.cliente?.email || ""}</Text>
                  </Table.Td>
                  <Table.Td ff="monospace" fz="xs">{r.entradas.map((e) => e.codigo).join(", ")}</Table.Td>
                  <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>{precio(r.total)}</Table.Td>
                  <Table.Td>
                    <ChipEstado valor={r.estado} />
                    {r.pago && <Text size="xs" c="dimmed">{etiqueta(r.pago.medio)} · {hora(r.pago.fecha)}</Text>}
                  </Table.Td>
                  <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>
                    {r.estado === "RESERVADA" && (
                      <>
                        <Anchor component={Link} to={`/cobrar/${r.id}`} size="xs" fw={500}>Cobrar</Anchor>
                        <Anchor component="button" size="xs" c="red" ml="sm"
                          onClick={() => accion(() => api.cancelarReserva(r.id), "Reserva cancelada, las butacas quedaron libres")}>
                          Cancelar
                        </Anchor>
                      </>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Paper>
    </>
  );
}

/* ------------------------------------------------------------------- cobrar */

/**
 * El checkout abierto: lo que el cliente tiene que aprobar en la pasarela.
 *
 * `codigoQr` es el *contenido* del QR y no una imagen, así que se muestra tal cual: la
 * pasarela es una emulación y el host no existe, de modo que dibujar el cuadrado o linkear
 * la URL harían parecer real algo que no lo es.
 */
function Checkout({ checkout, alConfirmar }) {
  const [confirmando, setConfirmando] = useState(false);
  return (
    <Alert color="gray" mt="md" title={`Checkout abierto · ${etiqueta(checkout.medio)}`}>
      <Stack gap={6}>
        <Text size="xs" c="dimmed">{checkout.id}</Text>
        <div>
          <Text size="sm">El cliente aprueba</Text>
          <Text fz={26} fw={700}>{precio(checkout.monto)}</Text>
        </div>
        <Text size="xs">Contenido del QR</Text>
        <Code block style={{ wordBreak: "break-all", whiteSpace: "pre-wrap" }}>{checkout.codigoQr}</Code>
        <Text size="xs">Link de pago</Text>
        <Text size="xs" ff="monospace" c="dimmed" style={{ wordBreak: "break-all" }}>{checkout.urlPago}</Text>
        <Button color="green" loading={confirmando}
          onClick={async () => { setConfirmando(true); if (!(await alConfirmar(checkout.id))) setConfirmando(false); }}>
          El cliente pagó · confirmar
        </Button>
        <Nota>
          El monto <strong>ya tiene el descuento aplicado</strong>: es el importe que se aprueba, y si después se cobrara otro
          no coincidirían. La pasarela es una emulación —el host no existe—, así que el aviso de «pagó» lo da esta pantalla;
          en una integración de verdad lo dispara el procesador.
        </Nota>
      </Stack>
    </Alert>
  );
}

function Cobro({ reserva, medios }) {
  const avisar = useAvisar();
  const navegar = useNavigate();
  const [medio, setMedio] = useState(medios[0]?.nombre);
  const [checkout, setCheckout] = useState(null);
  const [enviando, setEnviando] = useState(false);
  /**
   * R11 partido en dos caminos. El código de autorización de un medio electrónico lo
   * devuelve el procesador, así que dejó de tipearse a mano: ese campo era una invitación
   * a inventar un código y registrar un cobro que nadie autorizó. El efectivo no tiene
   * procesador ni código, y se sigue cobrando en la caja.
   */
  const porCheckout = medios.find((m) => m.nombre === medio)?.requiereAutorizacion;

  /** Con descuento no alcanza con decir cuánto entró: hay que poder explicar por qué se
   *  cobró menos que el subtotal, que es justo lo que el cliente va a preguntar. */
  function cobrado(pago) {
    avisar(pago.descuento > 0
      ? `Cobrado ${precio(pago.monto)} con ${etiqueta(pago.medio)} · ${precio(pago.descuento)} de descuento`
      : `Cobrado ${precio(pago.monto)} con ${etiqueta(pago.medio)}`);
    navegar("/caja");
  }

  async function enviar(evento) {
    evento.preventDefault();
    setEnviando(true);
    try {
      if (!porCheckout) cobrado(await api.cobrar(reserva.id, medio, ""));
      // Abrir el checkout todavía no cobra: valida R5, R17 y R19 y devuelve qué tiene que
      // aprobar el cliente. Se valida acá y no al confirmar porque mandar a pagar una
      // reserva que no se puede cobrar termina en plata que hay que devolver.
      else setCheckout(await api.abrirCheckout(reserva.id, medio));
    } catch (e) {
      avisar(e.message, "error");
    }
    setEnviando(false);
  }

  async function confirmar(id) {
    try {
      // Qué se está pagando sale del checkout, no de quien confirma.
      cobrado(await api.confirmarCheckout(id));
      return true;
    } catch (e) {
      avisar(e.message, "error");
      return false;
    }
  }

  return (
    <Paper withBorder p="md">
      <Title order={2} size="h4" mb="sm">Cobro</Title>
      <form onSubmit={enviar}>
        <Stack gap="sm">
          {/* Un checkout es de un medio y un monto concretos: cambiar el medio lo invalida. */}
          <Select label="Medio de pago" allowDeselect={false} value={medio} onChange={(m) => { setMedio(m); setCheckout(null); }}
            data={medios.map((m) => ({ value: m.nombre, label: etiqueta(m.nombre) }))} />
          <Paper bg="var(--mantine-color-default-hover)" p="sm">
            <Text size="sm" c="dimmed">A cobrar</Text>
            <Text fz="xl" fw={700}>{precio(reserva.total)}</Text>
            <Nota>
              Sale del total de las butacas: no se puede cobrar otro importe. Si hay una promoción vigente para este medio de
              pago, el descuento se aplica al cobrar.
            </Nota>
          </Paper>
          <Nota>
            {porCheckout
              ? "El cliente paga en la pasarela y el código de autorización lo devuelve ella."
              : "Se cobra en la caja del cine. El efectivo no lleva código de autorización."}
          </Nota>
          <Button type="submit" loading={enviando}>{porCheckout ? "Abrir checkout" : "Registrar cobro"}</Button>
        </Stack>
      </form>
      {checkout && <Checkout checkout={checkout} alConfirmar={confirmar} />}
    </Paper>
  );
}

export function Cobrar() {
  const { id } = useParams();
  const carga = useCargar(() => Promise.all([api.obtenerReservas(), api.obtenerMediosPago()]), [id]);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [reservas, medios] = carga.datos;
  const reserva = reservas.find((r) => r.id === Number(id));
  if (!reserva) return <ErrorCaja>No existe la reserva {id}</ErrorCaja>;

  if (reserva.estado !== "RESERVADA") {
    return (
      <Stack gap="md">
        <Volver a="/reservas">Reservas</Volver>
        <Alert color="yellow">
          La reserva {reserva.id} está {etiqueta(reserva.estado).toLowerCase()}, no se puede cobrar.{" "}
          {reserva.pago && (
            <>
              Se cobró {precio(reserva.pago.monto)} con {etiqueta(reserva.pago.medio)}
              {reserva.pago.descuento > 0 &&
                ` (subtotal ${precio(reserva.pago.subtotal)} − ${precio(reserva.pago.descuento)} de promoción)`}{" "}
              el {fechaHora(reserva.pago.fecha)}.
            </>
          )}
        </Alert>
      </Stack>
    );
  }

  return (
    <Stack gap="md">
      <Volver a="/reservas">Reservas</Volver>
      <Title order={1}>Cobrar reserva #{reserva.id}</Title>
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Cobro reserva={reserva} medios={medios} />
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Paper withBorder p="md">
            <Title order={2} size="h4">{reserva.pelicula?.titulo || "—"}</Title>
            {reserva.funcion && (
              <Text size="sm" c="dimmed">
                {dia(reserva.funcion.inicio)} {hora(reserva.funcion.inicio)} · {reserva.sala.nombre} ({etiqueta(reserva.sala.tipo)})
              </Text>
            )}
            <Group mt="sm" mb="sm" gap={4} align="baseline">
              <Text size="sm">{reserva.cliente?.nombre || "—"}</Text>
              <Text size="xs" c="dimmed">{reserva.cliente?.email || ""}</Text>
            </Group>
            <Table>
              <Table.Thead>
                <Table.Tr><Table.Th>Butaca</Table.Th><Table.Th>Tarifa</Table.Th><Table.Th ta="right">Precio</Table.Th></Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {reserva.entradas.map((e) => {
                  const reducida = e.tarifa && e.tarifa !== "GENERAL";
                  return (
                    <Table.Tr key={e.codigo}>
                      <Table.Td fw={500}>{e.codigo}</Table.Td>
                      <Table.Td fz="xs" fw={reducida ? 600 : 400} c={reducida ? "yellow" : "dimmed"}>{etiqueta(e.tarifa || "GENERAL")}</Table.Td>
                      <Table.Td ta="right">{precio(e.precio)}</Table.Td>
                    </Table.Tr>
                  );
                })}
              </Table.Tbody>
              <Table.Tfoot>
                <Table.Tr><Table.Th colSpan={2}>Subtotal</Table.Th><Table.Th ta="right">{precio(reserva.total)}</Table.Th></Table.Tr>
              </Table.Tfoot>
            </Table>
          </Paper>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
