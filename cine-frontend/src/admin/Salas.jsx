import { useState } from "react";
import { Anchor, Button, Grid, NumberInput, Paper, Select, Stack, Table, Text, TextInput, Title } from "@mantine/core";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { Chip } from "../componentes/Chips.jsx";
import { EsperaOError } from "../componentes/Estado.jsx";
import { ESTILO, estiloTipo, MapaButacas, Referencia } from "../componentes/MapaButacas.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Volver } from "../componentes/Volver.jsx";
import { Encabezado, useAccion } from "./comun.jsx";

const parsearNumeros = (texto) => String(texto || "").split(",").map((n) => Number(n.trim())).filter((n) => !Number.isNaN(n));

const parsearCodigos = (texto) => String(texto || "").split(",").map((c) => c.trim().toUpperCase()).filter(Boolean);

const VACIA = { nombre: "", tipo: null, distribucion: "", vip: "", pareja: "", accesibles: "", limpieza: 15 };

function FormularioSala({ tipos, alCrear }) {
  const [campos, setCampos] = useState({ ...VACIA, tipo: tipos[0]?.nombre });
  const texto = (clave) => (e) => setCampos({ ...campos, [clave]: e.currentTarget.value });
  const filas = parsearNumeros(campos.distribucion);

  async function crear(evento) {
    evento.preventDefault();
    const creada = await alCrear({
      nombre: campos.nombre,
      tipo: campos.tipo,
      butacasPorFila: filas,
      codigosVip: parsearCodigos(campos.vip),
      codigosPareja: parsearCodigos(campos.pareja),
      codigosAccesibles: parsearCodigos(campos.accesibles),
      minutosLimpieza: Number(campos.limpieza),
    });
    if (creada) setCampos({ ...VACIA, tipo: tipos[0]?.nombre });
  }

  const mono = { input: { fontFamily: "var(--mantine-font-family-monospace)" } };
  return (
    <form onSubmit={crear}>
      <Stack gap="sm">
        <TextInput label="Nombre" required value={campos.nombre} onChange={texto("nombre")} />
        <Select label="Tipo" allowDeselect={false} value={campos.tipo} onChange={(tipo) => setCampos({ ...campos, tipo })}
          data={tipos.map((t) => ({ value: t.nombre, label: `${etiqueta(t.nombre)} (×${t.multiplicador})` }))} />
        <TextInput label="Butacas por fila" required placeholder="8,10,12,12,14" styles={mono}
          description="Una fila por número. La primera es la A." value={campos.distribucion} onChange={texto("distribucion")} />
        {campos.distribucion.trim() && (
          <Text size="xs" c="dimmed">
            {filas.length} filas (A–{String.fromCharCode(64 + filas.length)}), {filas.reduce((a, b) => a + b, 0)} butacas
          </Text>
        )}
        <TextInput label="Butacas VIP" placeholder="I1,I2,J1" styles={mono} value={campos.vip} onChange={texto("vip")} />
        <TextInput label="Butacas de pareja" placeholder="A1,A2" styles={mono} value={campos.pareja} onChange={texto("pareja")} />
        <TextInput label="Butacas accesibles" placeholder="A1,A8" styles={mono} value={campos.accesibles} onChange={texto("accesibles")} />
        <NumberInput label="Minutos de limpieza" min={0} value={campos.limpieza}
          description="Lo que hay que esperar entre dos funciones. Una sala chica se levanta más rápido."
          onChange={(limpieza) => setCampos({ ...campos, limpieza })} />
        <Button type="submit">Crear sala</Button>
      </Stack>
    </form>
  );
}

function ListaSalas() {
  const carga = useCargar(() => Promise.all([api.obtenerSalas(), api.obtenerTiposSala()]), []);
  const accion = useAccion(carga.recargar);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [salas, tipos] = carga.datos;

  return (
    <>
      <Encabezado titulo="Salas" />
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 8 }}>
          <Paper withBorder>
            <Table.ScrollContainer minWidth={560}>
              <Table verticalSpacing="sm" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Sala</Table.Th><Table.Th>Tipo</Table.Th><Table.Th>Distribución</Table.Th>
                    <Table.Th>Butacas</Table.Th><Table.Th>Limpieza</Table.Th><Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {salas.map((s) => (
                    <Table.Tr key={s.id}>
                      <Table.Td fw={500}>{s.nombre}</Table.Td>
                      <Table.Td><Chip valor={s.tipo} color="indigo" /></Table.Td>
                      <Table.Td ff="monospace" fz="xs">{s.butacasPorFila.join(",")}</Table.Td>
                      <Table.Td>{s.capacidadSala}</Table.Td>
                      <Table.Td style={{ whiteSpace: "nowrap" }}>{s.minutosLimpieza} min</Table.Td>
                      <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>
                        <Anchor component={Link} to={`/salas/${s.id}`} size="xs">Butacas</Anchor>
                        <Anchor component="button" size="xs" c="red" ml="sm"
                          onClick={() => accion(() => api.eliminarSala(s.id), "Sala borrada")}>Borrar</Anchor>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Paper>
        </Grid.Col>
        <Grid.Col span={{ base: 12, lg: 4 }}>
          <Paper withBorder p="md">
            <Title order={2} size="h4" mb="sm">Nueva sala</Title>
            <FormularioSala tipos={tipos}
              alCrear={(sala) => accion(() => api.crearSala(sala), (s) => `${s.nombre} creada con ${s.capacidadSala} butacas`)} />
          </Paper>
        </Grid.Col>
      </Grid>
    </>
  );
}

/** Mapa de la sala para marcar y reponer butacas: acá no hay ocupación, es física. */
function MapaSala({ id }) {
  const avisar = useAvisar();
  const carga = useCargar(() => api.obtenerSala(id), [id]);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const sala = carga.datos;
  const rotas = sala.asientos.filter((a) => a.estado === "FUERA_DE_SERVICIO");

  function pintar(asiento) {
    if (asiento.estado === "FUERA_DE_SERVICIO") {
      return { estilo: { ...ESTILO.fueraDeServicio, cursor: "pointer" }, deshabilitado: false,
               titulo: `${asiento.codigo} · fuera de servicio · clic para reponer` };
    }
    return { estilo: estiloTipo(asiento.tipo), deshabilitado: false,
             titulo: `${asiento.codigo} · ${etiqueta(asiento.tipo)} · clic para marcar fuera de servicio` };
  }

  async function alternar(asiento) {
    const nuevo = asiento.estado === "FUERA_DE_SERVICIO" ? "HABILITADO" : "FUERA_DE_SERVICIO";
    try {
      await api.cambiarEstadoAsiento(sala.id, asiento.codigo, nuevo);
      avisar(`${asiento.codigo}: ${etiqueta(nuevo).toLowerCase()}`);
      carga.recargar();
    } catch (e) {
      avisar(e.message, "error");
    }
  }

  return (
    <Stack gap="md">
      <Volver a="/salas">Salas</Volver>
      <div>
        <Title order={1}>{sala.nombre}</Title>
        <Text size="sm">
          {etiqueta(sala.tipo)} · {sala.filas} filas · {sala.capacidadSala} butacas · {rotas.length} fuera de servicio
        </Text>
        <Text size="sm" c="dimmed">
          Clic en una butaca para marcarla fuera de servicio o reponerla. Una butaca rota no se vende en ninguna función.
        </Text>
      </div>
      <div>
        <MapaButacas sala={sala} asientos={sala.asientos} pintar={pintar} alElegir={alternar} />
        <Referencia items={[
          [estiloTipo("ESTANDAR"), "disponible"],
          [ESTILO.fueraDeServicio, "fuera de servicio"],
          [estiloTipo("VIP"), "* VIP"],
          [estiloTipo("PAREJA"), "& pareja"],
          [estiloTipo("ACCESIBLE"), "+ accesible"],
        ]} />
      </div>
    </Stack>
  );
}

export function Salas() {
  const { id } = useParams();
  return id ? <MapaSala id={id} /> : <ListaSalas />;
}
