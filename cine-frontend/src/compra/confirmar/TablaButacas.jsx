import { Table, Text } from "@mantine/core";
import { etiqueta } from "@/api/etiquetas.js";
import { precio } from "@/compartido/formato.js";
import { SelectorTarifa } from "@/compra/SelectorTarifa.jsx";
import { precioConTarifa } from "@/compra/tarifas.js";
import { useCambiarTarifa } from "@/compra/useCambiarTarifa.js";

export function TablaButacas({ elegidas, butacas, total }) {
  const cambiarTarifa = useCambiarTarifa();
  return (
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
  );
}
