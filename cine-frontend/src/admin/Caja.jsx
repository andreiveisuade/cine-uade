import { useState } from "react";
import { Group, Paper, Stack, Table, Text, TextInput, Title } from "@mantine/core";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { hora, hoyISO, precio } from "../api/formato.js";
import { EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { TablaCompras } from "./Candy.jsx";
import { Encabezado, FilaVacia } from "./comun.jsx";

function Cifra({ titulo, valor }) {
  return (
    <Paper withBorder px="md" py={6} miw={140}>
      <Text size="xs" tt="uppercase" c="dimmed">{titulo}</Text>
      <Text fz={24} fw={700}>{valor}</Text>
    </Paper>
  );
}

export function Caja() {
  const [fecha, setFecha] = useState(hoyISO());
  // Boletería y candy se cuentan por separado: el candy de mostrador no tiene función ni reserva.
  const carga = useCargar(() => Promise.all([api.obtenerArqueo(fecha), api.obtenerArqueoCandy(fecha)]), [fecha]);

  return (
    <>
      <Encabezado titulo="Arqueo">Lo cobrado en el día, por medio de pago: boletería y candy, cada una con su caja.</Encabezado>
      <Group align="flex-end" mb="md">
        <TextInput label="Fecha" type="date" value={fecha} onChange={(e) => setFecha(e.currentTarget.value)} />
      </Group>
      {!carga.datos ? <EsperaOError carga={carga} /> : (() => {
        const [arqueo, candy] = carga.datos;
        const medios = Object.entries(arqueo.porMedio);
        return (
          <Stack gap="lg">
            <Group gap="sm">
              <Cifra titulo="Boletería" valor={precio(arqueo.total)} />
              <Cifra titulo="Candy" valor={precio(candy.total)} />
              <Cifra titulo="Operaciones" valor={arqueo.pagos.length} />
              <Cifra titulo="Entradas" valor={arqueo.entradas} />
            </Group>
            {medios.length > 0 && (
              <Group gap="xs">
                {medios.map(([medio, datos]) => (
                  <Paper key={medio} withBorder px="sm" py={6}>
                    <Text size="sm">
                      <strong>{etiqueta(medio)}</strong> <Text span c="dimmed">· {datos.cantidad}</Text>{" "}
                      <Text span fw={600} ml={6}>{precio(datos.total)}</Text>
                    </Text>
                  </Paper>
                ))}
              </Group>
            )}
            <div>
              <Title order={2} size="h4" mb="xs">Boletería</Title>
              <Paper withBorder>
                <Table.ScrollContainer minWidth={760}>
                  <Table verticalSpacing="xs">
                    <Table.Thead>
                      <Table.Tr>
                        <Table.Th>Hora</Table.Th><Table.Th>Reserva</Table.Th><Table.Th>Película</Table.Th><Table.Th>Cliente</Table.Th>
                        <Table.Th>Medio</Table.Th><Table.Th>Autorización</Table.Th><Table.Th ta="right">Descuento</Table.Th>
                        <Table.Th ta="right">Monto</Table.Th>
                      </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                      {arqueo.pagos.length === 0 && <FilaVacia columnas={8}>No se cobró nada ese día.</FilaVacia>}
                      {arqueo.pagos.map((p) => (
                        <Table.Tr key={p.id}>
                          <Table.Td style={{ whiteSpace: "nowrap" }}>{hora(p.fecha)}</Table.Td>
                          <Table.Td>#{p.reservaId}</Table.Td>
                          <Table.Td>{p.pelicula?.titulo || "—"}</Table.Td>
                          <Table.Td>{p.cliente?.nombre || "—"}</Table.Td>
                          <Table.Td>{etiqueta(p.medio)}</Table.Td>
                          <Table.Td ff="monospace" fz="xs">{p.codigoAutorizacion || "—"}</Table.Td>
                          <Table.Td ta="right" fz="xs" c={p.descuento > 0 ? "green" : "dimmed"} style={{ whiteSpace: "nowrap" }}>
                            {p.descuento > 0 ? "−" + precio(p.descuento) : "—"}
                          </Table.Td>
                          <Table.Td ta="right" fw={500} style={{ whiteSpace: "nowrap" }}>{precio(p.monto)}</Table.Td>
                        </Table.Tr>
                      ))}
                    </Table.Tbody>
                  </Table>
                </Table.ScrollContainer>
              </Paper>
            </div>
            <div>
              <Title order={2} size="h4" mb="xs">Candy · {candy.compras.length} ventas</Title>
              <Paper withBorder><TablaCompras compras={candy.compras} /></Paper>
            </div>
          </Stack>
        );
      })()}
    </>
  );
}
