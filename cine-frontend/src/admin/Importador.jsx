import { useState } from "react";
import { Alert, Anchor, Button, Code, Group, Paper, Select, Skeleton, Stack, Table, Text, Title } from "@mantine/core";
import { Link } from "react-router";
import * as api from "../api/api-http.js";
import { fechaHora } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ChipEstado } from "../componentes/Chips.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Encabezado } from "./comun.jsx";

const PAGINAS = [
  { value: "1", label: "Una página (20 títulos)" },
  { value: "2", label: "Dos páginas (40 títulos)" },
  { value: "3", label: "Tres páginas (60 títulos)" },
];

function resumen(corrida) {
  if (corrida.estado === "FALLIDA") return corrida.detalle || "La importación falló";
  if (corrida.nuevas === 0) return "No había nada nuevo en TMDB";
  return `${corrida.nuevas} película${corrida.nuevas === 1 ? "" : "s"} nueva${corrida.nuevas === 1 ? "" : "s"} en Por revisar`;
}

/**
 * El log de la corrida, plegado. Son veinte líneas y casi nunca se miran: lo que se mira
 * son los números. Pero cuando una película no entró, la única respuesta a "por qué" está
 * acá adentro, con el mensaje que tiró la regla del backend.
 */
function Detalle({ corrida, abierto }) {
  if (!corrida.detalle) return <Text c="dimmed">—</Text>;
  return (
    <details open={abierto}>
      <summary style={{ cursor: "pointer" }}><Text span size="xs" c="dimmed">ver</Text></summary>
      <Code block fz="xs" mt={4} mah={260} style={{ overflow: "auto", whiteSpace: "pre-wrap" }}>{corrida.detalle}</Code>
    </details>
  );
}

/**
 * El botón que sale a buscar la cartelera real.
 *
 * Sin consultas repetidas: la corrida tarda diez o quince segundos y el botón espera esa
 * respuesta, que ya trae los contadores. La alternativa —contestar "ya te aviso" y
 * preguntar cada dos segundos si terminó— serían treinta pedidos al backend para
 * enterarse de algo que uno solo puede contar.
 */
export function Importador() {
  const avisar = useAvisar();
  const carga = useCargar(() => Promise.all([api.obtenerImportaciones(), api.estadoImportador()]), []);
  const [paginas, setPaginas] = useState("1");
  const [trayendo, setTrayendo] = useState(false);
  // La corrida recién hecha, para dejarle el detalle abierto.
  const [destacada, setDestacada] = useState(null);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [corridas, estado] = carga.datos;

  async function traer() {
    setTrayendo(true);
    try {
      const corrida = await api.importarAhora(paginas);
      avisar(resumen(corrida), corrida.estado === "FALLIDA" ? "error" : "ok");
      setDestacada(corrida.id);
      carga.recargar();
    } catch (e) {
      // Un 400 del backend —ya hay una corriendo, corrió recién— no es una pantalla rota:
      // es una respuesta. Se muestra y la pantalla queda como estaba.
      avisar(e.message, "error");
    }
    setTrayendo(false);
  }

  return (
    <>
      <Encabezado titulo="Importador">
        Trae de TMDB las películas que están hoy en cartelera en Argentina. Nada se publica: todo cae en{" "}
        <Anchor component={Link} to="/pendientes" size="sm">Por revisar</Anchor> y espera que alguien lo confirme.
      </Encabezado>

      <Stack gap="lg">
        {/* Si puede correr no se dice nada: que las cosas anden es lo esperable. El detalle
            ya viene redactado por el backend y dice qué hacer. */}
        {!estado.disponible && (
          <Alert color="yellow" title="El importador no está disponible">{estado.detalle}</Alert>
        )}

        <Paper withBorder p="md">
          <Group align="flex-end">
            <Select label="Cuánto traer" w={240} data={PAGINAS} value={paginas} allowDeselect={false}
              onChange={setPaginas} disabled={trayendo} />
            <Button onClick={traer} loading={trayendo} disabled={!estado.disponible}>Traer cartelera</Button>
          </Group>
        </Paper>

        {/* Quieta, una espera de quince segundos se lee como una pantalla rota y lo que hace
            el encargado es volver a apretar. */}
        {trayendo && (
          <Paper withBorder p="md">
            <Stack gap="sm">
              <Text size="sm" c="dimmed">Preguntándole a TMDB qué se está dando, y cargando lo que falte. Son unos segundos.</Text>
              <Skeleton h={14} w="66%" /><Skeleton h={14} w="50%" /><Skeleton h={14} w="60%" />
            </Stack>
          </Paper>
        )}

        <div>
          <Title order={2} size="h5" tt="uppercase" c="dimmed" mb="xs">Corridas anteriores</Title>
          {corridas.length === 0 ? (
            <Paper withBorder p="xl"><Vacio>Todavía no se pidió ninguna importación.</Vacio></Paper>
          ) : (
            <Paper withBorder>
              <Table.ScrollContainer minWidth={640}>
                <Table verticalSpacing="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Cuándo</Table.Th><Table.Th>Estado</Table.Th><Table.Th ta="right">Nuevas</Table.Th>
                      <Table.Th ta="right">Salteadas</Table.Th><Table.Th ta="right">Fallidas</Table.Th><Table.Th>Detalle</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {corridas.map((c) => (
                        <Table.Tr key={c.id} bg={c.id === destacada ? "var(--mantine-color-default-hover)" : undefined}>
                          <Table.Td style={{ whiteSpace: "nowrap" }}>{fechaHora(c.pedidaEn)}</Table.Td>
                          <Table.Td><ChipEstado valor={c.estado} /></Table.Td>
                          <Table.Td ta="right" fw={500}>{c.nuevas}</Table.Td>
                          <Table.Td ta="right" c="dimmed">{c.salteadas}</Table.Td>
                          <Table.Td ta="right" c={c.fallidas > 0 ? "red" : "dimmed"}>{c.fallidas}</Table.Td>
                          <Table.Td><Detalle corrida={c} abierto={c.id === destacada} /></Table.Td>
                        </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            </Paper>
          )}
        </div>
      </Stack>
    </>
  );
}
