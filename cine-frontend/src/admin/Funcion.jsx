import { useState } from "react";
import { Alert, Button, Divider, Grid, Group, Paper, Stack, Table, Text, Title } from "@mantine/core";
import { useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, fechaHora, hora, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Volver } from "../componentes/Volver.jsx";
import { Nota } from "./comun.jsx";

function Renglon({ etiqueta: texto, detalle, valor, c, fuerte }) {
  return (
    <Group justify="space-between" align="flex-start" wrap="nowrap">
      <div>
        <Text size={fuerte ? "lg" : "sm"} fw={fuerte ? 700 : 400}>{texto}</Text>
        {detalle && <Text size="xs" c="dimmed">{detalle}</Text>}
      </div>
      <Text size={fuerte ? "lg" : "sm"} fw={fuerte ? 700 : 500} c={c}>{valor}</Text>
    </Group>
  );
}

/**
 * @param emitido si se acaba de escribir el archivo. Consultar el borderó y declararlo no
 *                son lo mismo, así que la pantalla tampoco los muestra igual.
 */
function Bordero({ bordero, emitido, alEmitir, emitiendo }) {
  const tarifas = Object.entries(bordero.porTarifa);
  return (
    <Paper withBorder p="md">
      <Title order={2} size="h4">Borderó</Title>
      <Text size="xs" c="dimmed" mb="sm">
        Lo que se declara al INCAA. Cuenta lo <strong>cobrado</strong>: una reserva sin pagar retiene butacas pero no
        vendió ninguna entrada.
      </Text>
      {tarifas.length ? (
        <Table mb="sm">
          <Table.Thead>
            <Table.Tr><Table.Th>Tarifa</Table.Th><Table.Th ta="right">Entradas</Table.Th><Table.Th ta="right">Total</Table.Th></Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {tarifas.map(([tarifa, total]) => (
              <Table.Tr key={tarifa}>
                <Table.Td>{etiqueta(tarifa)}</Table.Td>
                <Table.Td ta="right">{total.cantidad}</Table.Td>
                <Table.Td ta="right">{precio(total.total)}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
          <Table.Tfoot>
            <Table.Tr><Table.Th colSpan={3}>{bordero.espectadores} espectadores</Table.Th></Table.Tr>
          </Table.Tfoot>
        </Table>
      ) : (
        <Alert color="gray" mb="sm">
          Todavía no se cobró ninguna entrada de esta función. No es un error: es un borderó en cero, y se puede declarar
          igual.
        </Alert>
      )}
      <Stack gap={6}>
        <Renglon etiqueta="Recaudación bruta" valor={precio(bordero.recaudacionBruta)} />
        <Renglon etiqueta="Descuentos" valor={`− ${precio(bordero.descuentos)}`} c={bordero.descuentos > 0 ? "yellow" : "dimmed"} />
        <Divider />
        <Renglon etiqueta="Recaudación neta" valor={precio(bordero.recaudacionNeta)} fuerte />
      </Stack>
      <Nota>
        Las tres van separadas porque cuentan cosas distintas: la <strong>bruta</strong> es a precio de lista —el valor
        declarado de cada localidad—, los <strong>descuentos</strong> son lo que resignó el cine por una promoción suya, y
        la <strong>neta</strong> es lo que entró en la caja. Con una sola, la diferencia no se podría explicar.
      </Nota>
      <Divider my="sm" />
      <Group gap="sm">
        <Button onClick={alEmitir} loading={emitiendo}>Emitir para el INCAA</Button>
        <Text size="xs" c="dimmed">
          {emitido
            ? `Emitido el ${fechaHora(bordero.generadoEn)}. Se guardó el archivo en el servidor.`
            : `Consultado el ${fechaHora(bordero.generadoEn)}.`}
        </Text>
      </Group>
      <Nota>
        Emitir <strong>escribe el archivo</strong> y pisa el anterior: el borderó de una función es uno solo y vale el
        último, porque las entradas se siguen vendiendo hasta que la película arranca.
      </Nota>
    </Paper>
  );
}

function Informe({ informe }) {
  return (
    <Paper withBorder p="md">
      <Title order={2} size="h4">Informe de la función</Title>
      <Text size="xs" c="dimmed" mb="sm">Cuánto dejó la función entre las dos cajas del cine: boletería y candy.</Text>
      <Stack gap="xs">
        <Renglon etiqueta="Boletería" valor={precio(informe.boleteria.recaudacionNeta)}
          detalle={`${informe.boleteria.espectadores} entradas cobradas, ya con los descuentos`} />
        <Renglon etiqueta="Candy" valor={precio(informe.candy)}
          detalle={`${informe.comprasCandy} ${informe.comprasCandy === 1 ? "compra atribuida" : "compras atribuidas"} a esta función`} />
        <Divider />
        <Renglon etiqueta="Total" valor={precio(informe.total)} fuerte />
      </Stack>
      <Alert color="yellow" mt="md" title="El candy de mostrador no entra, a propósito.">
        <Text size="xs">
          Solo se le atribuye a una función lo que se compró junto con la entrada, porque esa reserva es lo único que dice
          de qué función se trata: quien compra un balde en el mostrador puede estar yendo a cualquiera de las cuatro
          funciones de las 22:00, o a ninguna, y repartirlo sería inventar el dato. Consecuencia al leer los números:{" "}
          <strong>la suma de los informes de todas las funciones de un día da menos que el arqueo de ese día</strong>, y la
          diferencia es el mostrador. Esa plata se cuenta donde sí es cierta, en el arqueo del candy.
        </Text>
      </Alert>
    </Paper>
  );
}

/**
 * El detalle de una función: cuánto se vendió y cuánto dejó.
 *
 * Los dos informes cuelgan de acá y no de una pantalla propia porque es donde el
 * encargado ya está parado cuando los necesita, y porque los dos se piden por
 * `funcionId`: una pantalla separada empezaría pidiendo que eligiera la función de nuevo.
 */
export function Funcion() {
  const { id } = useParams();
  const avisar = useAvisar();
  const carga = useCargar(() => Promise.all([api.obtenerFuncion(id), api.obtenerBordero(id), api.obtenerInformeDeFuncion(id)]), [id]);
  const [emitido, setEmitido] = useState(null);
  const [emitiendo, setEmitiendo] = useState(false);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [funcion, bordero, informe] = carga.datos;

  async function emitir() {
    setEmitiendo(true);
    try {
      // Emitir no es consultar: escribe el archivo que se sube al organismo. Se muestra
      // lo que devuelve porque ahí viene el `generadoEn` que fecha lo declarado.
      const declarado = await api.emitirBordero(funcion.id);
      setEmitido(declarado);
      avisar(`Borderó emitido: ${declarado.espectadores} espectadores, ${precio(declarado.recaudacionNeta)}`);
    } catch (e) {
      avisar(e.message, "error");
    }
    setEmitiendo(false);
  }

  return (
    <Stack gap="md">
      <Volver a="/funciones">Funciones</Volver>
      <div>
        <Title order={1}>{funcion.pelicula.titulo}</Title>
        <Text size="sm" c="dimmed">
          {dia(funcion.inicio)} {hora(funcion.inicio)} · {funcion.sala.nombre} ({etiqueta(funcion.sala.tipo)}) ·{" "}
          {etiqueta(funcion.proyeccion)} · {etiqueta(funcion.idioma)} · precio base {precio(funcion.precio)} ·{" "}
          {funcion.libres} de {funcion.sala.capacidadSala} butacas libres
        </Text>
      </div>
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Bordero bordero={emitido || bordero} emitido={!!emitido} alEmitir={emitir} emitiendo={emitiendo} />
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Informe informe={informe} />
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
