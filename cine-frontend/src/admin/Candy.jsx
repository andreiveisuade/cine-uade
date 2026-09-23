import { useState } from "react";
import { Anchor, Badge, Button, Code, Grid, Group, NumberInput, Paper, Select, SimpleGrid, Stack, Table, Tabs, Text,
         TextInput, Title } from "@mantine/core";
import { Link, useNavigate, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta, TIPOS_PRODUCTO_SUELTO } from "../api/etiquetas.js";
import { fechaHora, hora, hoyISO, precio, precioExacto } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { Chip } from "../componentes/Chips.jsx";
import { ErrorCaja, EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Encabezado, FilaVacia, Nota, useAccion } from "./comun.jsx";

const componentesDe = (producto) =>
  producto.componentes?.length ? producto.componentes.map((c) => `${c.cantidad}× ${c.nombre}`).join(" + ") : "—";

const elegidas = (cantidades) =>
  Object.fromEntries(Object.entries(cantidades).filter(([, n]) => Number(n) > 0).map(([id, n]) => [id, Number(n)]));

// Nombre y precio: lo único que el backend deja cambiar.
function FilaEdicion({ producto: p, alGuardar, alCancelar }) {
  const [nombre, setNombre] = useState(p.nombre);
  const [valor, setValor] = useState(p.precio);
  return (
    <Table.Tr bg="var(--mantine-color-yellow-light)">
      <Table.Td colSpan={6}>
        <form onSubmit={(e) => { e.preventDefault(); alGuardar(p.id, { nombre, precio: valor }); }}>
          <Group align="flex-end" gap="xs">
            <TextInput label="Nombre" required size="xs" style={{ flex: 1 }} value={nombre} onChange={(e) => setNombre(e.currentTarget.value)} />
            <NumberInput label="Precio" required size="xs" w={120} min={1} value={valor} onChange={setValor} />
            <Button type="submit" size="xs">Guardar</Button>
            <Button size="xs" variant="default" onClick={alCancelar}>Cancelar</Button>
          </Group>
          {p.esCombo && <Nota mt={4}>Trae {componentesDe(p)}. Los componentes se fijan al armarlo.</Nota>}
        </form>
      </Table.Td>
    </Table.Tr>
  );
}

function AltaProducto({ alCrear }) {
  const [campos, setCampos] = useState({ nombre: "", tipo: TIPOS_PRODUCTO_SUELTO[0], precio: "" });
  const [error, setError] = useState(null);
  async function crear(evento) {
    evento.preventDefault();
    try {
      await alCrear(() => api.crearProductoCandy(campos), "Producto agregado");
      setCampos({ ...campos, nombre: "", precio: "" });
      setError(null);
    } catch (e) {
      setError(e.message);
    }
  }
  return (
    <Paper withBorder p="md">
      <Title order={2} size="h4" mb="sm">Nuevo producto</Title>
      <form onSubmit={crear}>
        <Stack gap="sm">
          <TextInput label="Nombre" required placeholder="Pochoclos grandes" value={campos.nombre}
            onChange={(e) => setCampos({ ...campos, nombre: e.currentTarget.value })} />
          <Select label="Tipo" allowDeselect={false} value={campos.tipo} onChange={(tipo) => setCampos({ ...campos, tipo })}
            data={TIPOS_PRODUCTO_SUELTO.map((t) => ({ value: t, label: etiqueta(t) }))} />
          <NumberInput label="Precio" required min={1} value={campos.precio} onChange={(v) => setCampos({ ...campos, precio: v })} />
          <Button type="submit">Agregar a la carta</Button>
          {error && <ErrorCaja>{error}</ErrorCaja>}
        </Stack>
      </form>
    </Paper>
  );
}

function AltaCombo({ sueltos, alCrear }) {
  const [nombre, setNombre] = useState("");
  const [valor, setValor] = useState("");
  const [cantidades, setCantidades] = useState({});
  const [error, setError] = useState(null);

  async function crear(evento) {
    evento.preventDefault();
    const componentes = elegidas(cantidades);
    // El mínimo de dos se ataja antes; R14 no, porque el precio de referencia lo sabe la carta.
    if (Object.keys(componentes).length < 2) {
      setError("Un combo tiene que juntar al menos dos productos distintos");
      return;
    }
    try {
      await alCrear(() => api.armarComboCandy({ nombre, precio: valor, componentes }), "Combo armado");
      setNombre(""); setValor(""); setCantidades({}); setError(null);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <Paper withBorder p="md">
      <Title order={2} size="h4">Armar combo</Title>
      <Nota mb="sm">
        Al menos dos productos. El combo tiene que salir menos que sus componentes sueltos (R14): si no, no habría motivo para
        ofrecerlo.
      </Nota>
      <form onSubmit={crear}>
        <Stack gap="sm">
          <TextInput label="Nombre" required placeholder="Combo clásico" value={nombre} onChange={(e) => setNombre(e.currentTarget.value)} />
          <Text size="sm" fw={500}>Qué trae (cantidad)</Text>
          {sueltos.length ? sueltos.map((p) => (
            <Group key={p.id} justify="space-between" wrap="nowrap">
              <Text size="sm" c={p.disponible ? undefined : "dimmed"}>
                {p.nombre} <Text span size="xs" c="dimmed">· {precio(p.precio)}</Text>
              </Text>
              <NumberInput size="xs" w={72} min={0} value={cantidades[p.id] ?? 0}
                onChange={(n) => setCantidades({ ...cantidades, [p.id]: n })} />
            </Group>
          )) : <Nota>Primero cargá productos sueltos.</Nota>}
          <NumberInput label="Precio del combo" required min={1} value={valor} onChange={setValor} />
          <Button type="submit">Armar combo</Button>
          {error && <ErrorCaja>{error}</ErrorCaja>}
        </Stack>
      </form>
    </Paper>
  );
}

function Carta() {
  const avisar = useAvisar();
  const carga = useCargar(() => api.obtenerProductosCandy(true), []);
  const accion = useAccion(carga.recargar);
  const [editando, setEditando] = useState(null);
  const [error, setError] = useState(null);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const productos = carga.datos;

  // Las altas muestran el error en su propio formulario, así que lo dejan subir.
  const crear = async (pedido, mensaje) => {
    await pedido();
    avisar(mensaje);
    carga.recargar();
  };

  async function guardar(id, cambios) {
    try {
      await api.editarProductoCandy(id, cambios);
      avisar("Producto actualizado");
      setEditando(null);
      setError(null);
      carga.recargar();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <Grid gap="md" align="flex-start">
      <Grid.Col span={{ base: 12, lg: 8 }}>
        <Paper withBorder>
          <Table.ScrollContainer minWidth={620}>
            <Table verticalSpacing="xs">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Producto</Table.Th><Table.Th>Tipo</Table.Th><Table.Th>Trae</Table.Th>
                  <Table.Th ta="right">Precio</Table.Th><Table.Th>Estado</Table.Th><Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {productos.length === 0 && <FilaVacia columnas={6}>Todavía no hay productos.</FilaVacia>}
                {productos.map((p) => (p.id === editando ? (
                  <FilaEdicion key={p.id} producto={p} alGuardar={guardar} alCancelar={() => setEditando(null)} />
                ) : (
                  <Table.Tr key={p.id} style={{ opacity: p.disponible ? 1 : 0.55 }}>
                    <Table.Td fw={500}>{p.nombre}</Table.Td>
                    <Table.Td><Chip valor={p.tipo} color={p.esCombo ? "yellow" : "gray"} /></Table.Td>
                    <Table.Td fz="xs">{componentesDe(p)}</Table.Td>
                    <Table.Td ta="right" fw={600} style={{ whiteSpace: "nowrap" }}>{precio(p.precio)}</Table.Td>
                    <Table.Td fz="xs" style={{ whiteSpace: "nowrap" }}>{p.disponible ? "A la venta" : "Fuera de la carta"}</Table.Td>
                    <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>
                      <Anchor component="button" size="xs" onClick={() => setEditando(p.id)}>Editar</Anchor>
                      <Anchor component="button" size="xs" ml="sm" c={p.disponible ? "red" : "green"}
                        onClick={() => accion(() => api.cambiarDisponibilidadCandy(p.id, !p.disponible))}>
                        {p.disponible ? "Sacar de la carta" : "Reponer"}
                      </Anchor>
                    </Table.Td>
                  </Table.Tr>
                )))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
          {error && <ErrorCaja>{error}</ErrorCaja>}
          <Nota p="sm" style={{ borderTop: "1px solid var(--mantine-color-default-border)" }}>
            Los productos no se borran: se sacan de la carta. Uno que ya se vendió tiene que seguir existiendo para que el
            ticket de esa venta diga qué se llevó. Cambiar un precio no toca las ventas hechas: cada una guardó el precio que
            tenía.
          </Nota>
        </Paper>
      </Grid.Col>
      <Grid.Col span={{ base: 12, lg: 4 }}>
        <Stack gap="md">
          <AltaProducto alCrear={crear} />
          <AltaCombo sueltos={productos.filter((p) => !p.esCombo)} alCrear={crear} />
        </Stack>
      </Grid.Col>
    </Grid>
  );
}

const LINEA = "=".repeat(40);

function armarTicket(compra) {
  const renglon = (izquierda, derecha) => ` ${izquierda.padEnd(26)}${derecha.padStart(12)}`;
  return [
    LINEA,
    "  CINE UADE · CANDY",
    `  ${fechaHora(compra.fecha)}`,
    LINEA,
    ...compra.items.map((i) => renglon(`${i.cantidad}x ${i.nombre}`.slice(0, 26), precioExacto(i.subtotal))),
    LINEA,
    renglon("TOTAL", precioExacto(compra.total)),
    ...(compra.ahorro > 0 ? [renglon("Ahorro por combos", precioExacto(compra.ahorro))] : []),
    renglon("Medio", etiqueta(compra.medio)),
    ...(compra.codigoAutorizacion ? [renglon("Autorizacion", compra.codigoAutorizacion)] : []),
    ...(compra.reservaId ? [renglon("Reserva", "#" + compra.reservaId)] : []),
    LINEA,
  ].join("\n");
}

function Venta() {
  const avisar = useAvisar();
  const carga = useCargar(() => Promise.all([api.obtenerProductosCandy(false), api.obtenerMediosPago()]), []);
  const [cantidades, setCantidades] = useState({});
  const [medio, setMedio] = useState(null);
  const [codigo, setCodigo] = useState("");
  const [email, setEmail] = useState("");
  const [reservaId, setReservaId] = useState("");
  const [error, setError] = useState(null);
  const [ticket, setTicket] = useState(null);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [productos, medios] = carga.datos;
  const medioElegido = medio || medios[0]?.nombre;
  const requiereCodigo = medios.find((m) => m.nombre === medioElegido)?.requiereAutorizacion;

  async function cobrar(evento) {
    evento.preventDefault();
    setError(null);
    const pedidas = elegidas(cantidades);
    if (!Object.keys(pedidas).length) return setError("Hay que elegir al menos un producto");
    if (requiereCodigo && !codigo.trim()) {
      return setError(`El pago con ${etiqueta(medioElegido)} necesita código de autorización`);
    }
    try {
      let clienteId = null;
      // Con reserva el email sobra: el backend toma el cliente de la reserva.
      if (email.trim() && !String(reservaId).trim()) {
        const cliente = await api.buscarClientePorEmail(email);
        if (!cliente) return setError(`No hay ningún cliente con el email ${email.trim()}`);
        clienteId = cliente.id;
      }
      const compra = await api.venderCandy({
        clienteId, reservaId, cantidades: pedidas, medio: medioElegido,
        codigoAutorizacion: requiereCodigo ? codigo.trim() : "",
      });
      avisar(`Cobrado ${precio(compra.total)}`);
      setTicket(compra);
      setCantidades({});
      setCodigo("");
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <Grid gap="md" align="flex-start">
      <Grid.Col span={{ base: 12, lg: 8 }}>
        <Paper withBorder>
          <form onSubmit={cobrar}>
            <Table.ScrollContainer minWidth={520}>
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Producto</Table.Th><Table.Th>Trae</Table.Th><Table.Th ta="right">Precio</Table.Th>
                    <Table.Th ta="right">Cantidad</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {productos.length === 0 && <FilaVacia columnas={4}>No hay nada a la venta. Cargá productos en la carta.</FilaVacia>}
                  {productos.map((p) => (
                    <Table.Tr key={p.id}>
                      <Table.Td fw={500}>{p.nombre}</Table.Td>
                      <Table.Td fz="xs">{componentesDe(p)}</Table.Td>
                      <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>{precio(p.precio)}</Table.Td>
                      <Table.Td ta="right">
                        <NumberInput size="xs" w={80} ml="auto" min={0} value={cantidades[p.id] ?? 0}
                          onChange={(n) => setCantidades({ ...cantidades, [p.id]: n })} />
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
            <SimpleGrid cols={{ base: 1, sm: 2 }} p="md">
              <Select label="Medio de pago" allowDeselect={false} value={medioElegido} onChange={setMedio}
                data={medios.map((m) => ({ value: m.nombre, label: etiqueta(m.nombre) }))} />
              {requiereCodigo ? (
                <TextInput label="Código de autorización" autoComplete="off" value={codigo}
                  description="El que da el posnet o la app (R11). En efectivo no hace falta."
                  onChange={(e) => setCodigo(e.currentTarget.value)} />
              ) : <div />}
              <TextInput label="Cliente (opcional)" type="email" placeholder="email del cliente" value={email}
                onChange={(e) => setEmail(e.currentTarget.value)} />
              <NumberInput label="Reserva (opcional)" min={1} value={reservaId} onChange={setReservaId}
                description="Con reserva, el cliente sale de ella y la venta suma al informe de esa función." />
            </SimpleGrid>
            <Stack px="md" pb="md" gap="sm">
              <Button type="submit">Cobrar</Button>
              {error && <ErrorCaja>{error}</ErrorCaja>}
            </Stack>
          </form>
        </Paper>
      </Grid.Col>
      <Grid.Col span={{ base: 12, lg: 4 }}>
        <Paper withBorder p="md">
          {ticket ? (
            <>
              <Text size="sm" fw={600} mb="xs">Venta #{ticket.id}</Text>
              <Code block fz="xs">{armarTicket(ticket)}</Code>
            </>
          ) : (
            <Text size="sm" c="dimmed">
              El total lo calcula el backend con los precios de la carta: acá no se tipea. Al cobrar aparece el ticket.
            </Text>
          )}
        </Paper>
      </Grid.Col>
    </Grid>
  );
}

export function TablaCompras({ compras }) {
  return (
    <Table.ScrollContainer minWidth={760}>
      <Table verticalSpacing="xs">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Hora</Table.Th><Table.Th>Venta</Table.Th><Table.Th>Qué se llevó</Table.Th><Table.Th>Reserva</Table.Th>
            <Table.Th>Medio</Table.Th><Table.Th>Autorización</Table.Th><Table.Th ta="right">Ahorro</Table.Th>
            <Table.Th ta="right">Total</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {compras.length === 0 && <FilaVacia columnas={8}>No se vendió nada ese día.</FilaVacia>}
          {compras.map((c) => (
            <Table.Tr key={c.id}>
              <Table.Td style={{ whiteSpace: "nowrap" }}>{hora(c.fecha)}</Table.Td>
              <Table.Td>#{c.id}</Table.Td>
              <Table.Td fz="xs">{c.items.map((i) => `${i.cantidad}× ${i.nombre}`).join(", ")}</Table.Td>
              <Table.Td>{c.reservaId ? `#${c.reservaId}` : "—"}</Table.Td>
              <Table.Td>{etiqueta(c.medio)}</Table.Td>
              <Table.Td ff="monospace" fz="xs">{c.codigoAutorizacion || "—"}</Table.Td>
              <Table.Td ta="right" fz="xs" c={c.ahorro > 0 ? "green" : "dimmed"} style={{ whiteSpace: "nowrap" }}>
                {c.ahorro > 0 ? precio(c.ahorro) : "—"}
              </Table.Td>
              <Table.Td ta="right" fw={500} style={{ whiteSpace: "nowrap" }}>{precio(c.total)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

function Ventas() {
  const [fecha, setFecha] = useState(hoyISO());
  const carga = useCargar(() => api.obtenerComprasCandy({ fecha }), [fecha]);
  return (
    <Stack gap="md">
      <Group align="flex-end">
        <TextInput label="Fecha" type="date" value={fecha} onChange={(e) => setFecha(e.currentTarget.value)} />
        {carga.datos && (
          <Paper withBorder px="md" py={6}>
            <Text size="xs" tt="uppercase" c="dimmed">Ventas</Text>
            <Text fz={24} fw={700}>{carga.datos.length}</Text>
          </Paper>
        )}
      </Group>
      {carga.datos ? <Paper withBorder><TablaCompras compras={carga.datos} /></Paper> : <EsperaOError carga={carga} />}
      <Nota>
        El total cobrado del día está en <Anchor component={Link} to="/caja" size="xs">Caja</Anchor>, al lado de la boletería.
      </Nota>
    </Stack>
  );
}

// Pestañas y no una pantalla larga: cobrar un pochoclo no puede pedir scrollear entre combos.
export function Candy() {
  const { pestana = "carta" } = useParams();
  const navegar = useNavigate();
  return (
    <>
      <Encabezado titulo="Candy">
        La otra caja del cine: se cobra en el mostrador y se entrega, sin reserva de por medio.
      </Encabezado>
      <Tabs value={pestana} onChange={(p) => navegar(p === "carta" ? "/candy" : `/candy/${p}`)} keepMounted={false}>
        <Tabs.List mb="md">
          <Tabs.Tab value="carta">Carta</Tabs.Tab>
          <Tabs.Tab value="venta">Venta de mostrador</Tabs.Tab>
          <Tabs.Tab value="ventas">Ventas del día</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="carta"><Carta /></Tabs.Panel>
        <Tabs.Panel value="venta"><Venta /></Tabs.Panel>
        <Tabs.Panel value="ventas"><Ventas /></Tabs.Panel>
      </Tabs>
    </>
  );
}
