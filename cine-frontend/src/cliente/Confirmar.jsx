import { useState } from "react";
import { Alert, Anchor, Button, Grid, Paper, Stack, Table, Text, TextInput, Title } from "@mantine/core";
import { Link, Navigate, useNavigate, useParams } from "react-router";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/api/formato.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { useTitulo } from "@/compartido/hooks/useTitulo.js";
import { useSesion } from "./sesion.jsx";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { catalogoTarifas, clienteRecordado, DatosFuncion, faltantes, precioConTarifa, recordarCliente, resumenCompra,
         SelectorTarifa, sesionDeCompra, tarifaPorNombre, useCambiarTarifa, useCompra,
         useRenovarBloqueo } from "./compra.jsx";

export function Confirmar() {
  const { id } = useParams();
  const navegar = useNavigate();
  const avisar = useAvisar();
  useTitulo("Confirmar reserva");
  const { seleccion, setSeleccion } = useCompra();
  const cambiarTarifa = useCambiarTarifa();
  const { usuario } = useSesion();
  // Con sesión, los datos de la cuenta: reservar con ese email la deja en Mis reservas.
  const [recordado] = useState(() => usuario || clienteRecordado());
  const [nombre, setNombre] = useState(recordado?.nombre || "");
  const [email, setEmail] = useState(recordado?.email || "");
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);
  // La reserva ya está hecha y se va al ticket: vaciar la selección no es "se perdió". Es estado y no ref porque
  // el render lo lee (esta pantalla sigue montada mientras dura la animación de salida).
  const [reservada, setReservada] = useState(false);
  const carga = useCargar(() => Promise.all([api.obtenerFuncion(id, sesionDeCompra()), catalogoTarifas()]), [id]);
  const funcion = carga.datos?.[0];

  // Completar el formulario lleva más de lo que dura un bloqueo.
  useRenovarBloqueo(funcion?.id);

  if (!funcion) return <EsperaOError carga={carga} />;
  // Si se recargó la página la selección se perdió: volver al mapa.
  if (!reservada && (seleccion.funcionId !== funcion.id || Object.keys(seleccion.butacas).length === 0)) {
    return <Navigate to={`/funcion/${funcion.id}`} replace />;
  }

  const butacas = seleccion.butacas;
  const { elegidas, total } = resumenCompra(funcion, butacas);
  const aAcreditar = elegidas.filter((a) => tarifaPorNombre(butacas[a.codigo]).requiereAcreditacion);

  async function confirmar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      nombre: { valor: nombre, etiqueta: "tu nombre" },
      email: { valor: email, etiqueta: "tu email" },
    });
    setFaltan(vacios);
    if (Object.keys(vacios).length) return;
    setError(null);
    setEnviando(true);
    try {
      const reserva = await api.crearReserva({
        funcionId: funcion.id, nombre: nombre.trim(), email: email.trim(), butacas,
        // La misma sesión que bloqueó: si no, el propio bloqueo rebotaría la reserva.
        sesion: sesionDeCompra(),
      });
      recordarCliente({ nombre: nombre.trim(), email: email.trim() });
      setReservada(true);
      setSeleccion({ funcionId: null, butacas: {} });
      // El alta ya trae la reserva entera: el ticket la dibuja sin volver a pedirla.
      navegar(`/ticket/${reserva.codigo}`, { state: { reserva } });
    } catch (e) {
      setEnviando(false);
      // 409: otra compra ganó una butaca en el medio (vendida o bloqueada); vuelve al mapa recargado.
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
    <div>
      <Encabezado titulo="Confirmar reserva" volver={{ a: `/funcion/${funcion.id}`, texto: "Cambiar butacas" }} />

      <Grid gap="lg" align="flex-start">
        <Grid.Col span={{ base: 12, md: 5 }} order={{ base: 2, md: 1 }}>
          <Paper withBorder p="lg">
            <Title order={2} fz="h4" mb="md">Tus datos</Title>
            <form onSubmit={confirmar}>
              <Stack gap="sm">
                <TextInput label="Nombre" required value={nombre} error={faltan.nombre}
                  onChange={(e) => setNombre(e.currentTarget.value)} />
                <TextInput label="Email" type="email" required value={email} error={faltan.email}
                  onChange={(e) => setEmail(e.currentTarget.value)} />
                <Button type="submit" size="md" mt="xs" loading={enviando}>Confirmar reserva</Button>
                {error && <ErrorCaja>{error}</ErrorCaja>}
                {usuario === null && (
                  <Text size="xs" c="dimmed">
                    Para pagar online hace falta una cuenta:{" "}
                    <Anchor component={Link} to="/ingresar" size="xs">ingresá</Anchor> o{" "}
                    <Anchor component={Link} to="/registro" size="xs">registrate</Anchor>, antes o después de reservar.
                  </Text>
                )}
              </Stack>
            </form>
          </Paper>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }} order={{ base: 1, md: 2 }}>
          <Paper withBorder p="lg">
            <Title order={2} fz="h3">{funcion.pelicula.titulo}</Title>
            <DatosFuncion funcion={funcion} c="dimmed" mb="md" />
            <Table verticalSpacing="sm" className="cine-cifras">
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
                      <SelectorTarifa valor={butacas[a.codigo]} alCambiar={(t) => cambiarTarifa(a.codigo, t)} />
                    </Table.Td>
                    <Table.Td ta="right">{precio(precioConTarifa(a, butacas[a.codigo]))}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
              <Table.Tfoot>
                <Table.Tr>
                  <Table.Th colSpan={3} fz="md">Total</Table.Th>
                  <Table.Th ta="right"><Text span className="cine-display" fz={30} fw={850} style={{ whiteSpace: "nowrap" }}>{precio(total)}</Text></Table.Th>
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
              <Alert color="ambar" variant="light" mt="md" title="Acordate del carnet">
                En la puerta te van a pedir que acredites la tarifa de{" "}
                {aAcreditar.map((a) => `${a.codigo} (${etiqueta(butacas[a.codigo]).toLowerCase()})`).join(", ")}.
              </Alert>
            )}
          </Paper>
        </Grid.Col>
      </Grid>
    </div>
  );
}
