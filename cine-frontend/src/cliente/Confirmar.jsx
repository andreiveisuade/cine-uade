import { useRef, useState } from "react";
import { Alert, Anchor, Button, Grid, Paper, Stack, Table, Text, TextInput, Title } from "@mantine/core";
import { Link, Navigate, useNavigate, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, hora, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ErrorCaja, EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Volver } from "../componentes/Volver.jsx";
import { catalogoTarifas, clienteRecordado, precioConTarifa, recordarCliente, SelectorTarifa, sesionDeCompra,
         tarifaPorNombre, useCompra, useRenovarBloqueo } from "./compra.jsx";

export function Confirmar() {
  const { id } = useParams();
  const navegar = useNavigate();
  const avisar = useAvisar();
  const { seleccion, setSeleccion } = useCompra();
  const [recordado] = useState(clienteRecordado);
  const [nombre, setNombre] = useState(recordado?.nombre || "");
  const [email, setEmail] = useState(recordado?.email || "");
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  // La reserva ya está hecha y se va al ticket: vaciar la selección no es "se perdió".
  const reservada = useRef(false);
  const carga = useCargar(() => Promise.all([api.obtenerFuncion(id, sesionDeCompra()), catalogoTarifas()]), [id]);
  const funcion = carga.datos?.[0];

  // Completar el formulario lleva más de lo que dura un bloqueo.
  useRenovarBloqueo(funcion?.id);

  if (!funcion) return <EsperaOError carga={carga} />;
  // Si se recargó la página la selección se perdió: volver al mapa.
  if (!reservada.current && (seleccion.funcionId !== funcion.id || Object.keys(seleccion.butacas).length === 0)) {
    return <Navigate to={`/funcion/${funcion.id}`} replace />;
  }

  const butacas = seleccion.butacas;
  const elegidas = funcion.asientos.filter((a) => butacas[a.codigo]);
  const total = elegidas.reduce((suma, a) => suma + precioConTarifa(a, butacas[a.codigo]), 0);
  const aAcreditar = elegidas.filter((a) => tarifaPorNombre(butacas[a.codigo]).requiereAcreditacion);

  async function confirmar(evento) {
    evento.preventDefault();
    setEnviando(true);
    try {
      const reserva = await api.crearReserva({
        funcionId: funcion.id, nombre, email, butacas,
        // La misma sesión que bloqueó: si no, el propio bloqueo rebotaría la reserva.
        sesion: sesionDeCompra(),
      });
      recordarCliente({ nombre: nombre.trim(), email: email.trim() });
      reservada.current = true;
      setSeleccion({ funcionId: null, butacas: {} });
      // El alta ya trae la reserva entera: el ticket la dibuja sin volver a pedirla.
      navegar(`/ticket/${reserva.codigo}`, { state: { reserva } });
    } catch (e) {
      setEnviando(false);
      // 409: alguien tomó la butaca en el medio; vuelve al mapa recargado.
      if (e.status === 409) {
        setSeleccion({ funcionId: funcion.id, butacas: {} });
        avisar(e.message, "error");
        navegar(`/funcion/${funcion.id}`);
        return;
      }
      setError(e.message);
    }
  }

  return (
    <Stack gap="md">
      <Volver a={`/funcion/${funcion.id}`}>Cambiar butacas</Volver>
      <Title order={1}>Confirmar reserva</Title>

      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Paper withBorder p="md">
            <Title order={2} size="h4" mb="sm">Tus datos</Title>
            <form onSubmit={confirmar}>
              <Stack gap="sm">
                <TextInput label="Nombre" required value={nombre} onChange={(e) => setNombre(e.currentTarget.value)} />
                <TextInput label="Email" type="email" required value={email} onChange={(e) => setEmail(e.currentTarget.value)} />
                <Button type="submit" loading={enviando}>Confirmar reserva</Button>
                {error && <ErrorCaja>{error}</ErrorCaja>}
                {!recordado && (
                  <Text size="xs" c="dimmed">
                    ¿Ya compraste antes? <Anchor component={Link} to="/registro" size="xs">Registrate</Anchor> para
                    no cargar los datos cada vez.
                  </Text>
                )}
              </Stack>
            </form>
          </Paper>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }}>
          <Paper withBorder p="md">
            <Title order={2} size="h4">{funcion.pelicula.titulo}</Title>
            <Text size="sm" c="dimmed" mb="sm">
              {dia(funcion.inicio)} {hora(funcion.inicio)} · {funcion.sala.nombre} ({etiqueta(funcion.sala.tipo)}) ·{" "}
              {etiqueta(funcion.proyeccion)} · {etiqueta(funcion.idioma)}
            </Text>
            <Table>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Butaca</Table.Th><Table.Th>Tipo</Table.Th><Table.Th>Tarifa</Table.Th>
                  <Table.Th ta="right">Precio</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {elegidas.map((a) => (
                  <Table.Tr key={a.codigo}>
                    <Table.Td fw={500}>{a.codigo}</Table.Td>
                    <Table.Td>{etiqueta(a.tipo)}</Table.Td>
                    <Table.Td>
                      <SelectorTarifa valor={butacas[a.codigo]}
                        alCambiar={(t) => setSeleccion((s) => ({ ...s, butacas: { ...s.butacas, [a.codigo]: t } }))} />
                    </Table.Td>
                    <Table.Td ta="right">{precio(precioConTarifa(a, butacas[a.codigo]))}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
              <Table.Tfoot>
                <Table.Tr>
                  <Table.Th colSpan={3}>Total</Table.Th>
                  <Table.Th ta="right">{precio(total)}</Table.Th>
                </Table.Tr>
              </Table.Tfoot>
            </Table>
            <Text size="xs" c="dimmed" mt="sm">
              Precio base {precio(funcion.precio)} × sala {etiqueta(funcion.sala.tipo)} × tipo de butaca × tarifa.
            </Text>
            <Text size="xs" c="dimmed" mt={4}>
              Si hay promociones vigentes, el descuento se aplica al pagar: depende del medio de pago, así que el
              total definitivo aparece recién ahí.
            </Text>
            {aAcreditar.length > 0 && (
              <Alert color="yellow" mt="sm" title="Acordate del carnet">
                En la puerta te van a pedir que acredites la tarifa de{" "}
                {aAcreditar.map((a) => `${a.codigo} (${etiqueta(butacas[a.codigo]).toLowerCase()})`).join(", ")}.
              </Alert>
            )}
          </Paper>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
