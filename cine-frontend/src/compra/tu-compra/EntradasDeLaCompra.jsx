import { Alert, Paper, Table, Text, Title } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/compartido/formato.js";
import { DatosFuncion } from "@/compra/DatosFuncion.jsx";
import { conAcreditacion } from "@/ticket/estadoReserva.js";

// Lo elegido en el mapa, ya reservado, a modo de confirmación: cada butaca con su tarifa y el precio de las entradas.
export function EntradasDeLaCompra({ reserva }) {
  const aAcreditar = reserva.entradas.filter(conAcreditacion);
  const funcion = { ...reserva.funcion, sala: reserva.sala || reserva.funcion.sala };
  return (
    <Paper withBorder p="lg">
      <Title order={2} fz="h3">{reserva.pelicula.titulo}</Title>
      <DatosFuncion funcion={funcion} c="dimmed" mb="md" />
      <Table verticalSpacing="xs" className="cine-cifras">
        <Table.Thead>
          <Table.Tr><Table.Th>Butaca</Table.Th><Table.Th>Tarifa</Table.Th><Table.Th ta="right">Precio</Table.Th></Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {reserva.entradas.map((e) => (
            <Table.Tr key={e.codigo}>
              <Table.Td fw={600}>{e.codigo}</Table.Td>
              <Table.Td>{etiqueta(e.tarifa)}</Table.Td>
              <Table.Td ta="right">{precio(e.precio)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
        <Table.Tfoot>
          <Table.Tr>
            <Table.Th colSpan={2}>Entradas</Table.Th>
            <Table.Th ta="right"><Text span fw={800}>{precio(reserva.total)}</Text></Table.Th>
          </Table.Tr>
        </Table.Tfoot>
      </Table>
      {aAcreditar.length > 0 && (
        <Alert color="ambar" variant="light" mt="md" title="Acordate del carnet">
          En la puerta te van a pedir que acredites la tarifa de{" "}
          {aAcreditar.map((e) => `${e.codigo} (${etiqueta(e.tarifa).toLowerCase()})`).join(", ")}.
        </Alert>
      )}
    </Paper>
  );
}
