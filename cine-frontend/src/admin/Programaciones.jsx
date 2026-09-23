import { useState } from "react";
import { Alert, Anchor, Badge, Button, Checkbox, Grid, Group, NumberInput, Paper, ScrollArea, Select, Stack, Table,
         Text, TextInput, Title } from "@mantine/core";
import * as api from "../api/api-http.js";
import { DIAS_SEMANA, etiqueta } from "../api/etiquetas.js";
import { fechaHora, hoyISO, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ErrorCaja, EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { BarraFiltros, Encabezado, FilaVacia, Nota, opcionesDe, useAccion } from "./comun.jsx";

const SIN_FILTROS = { peliculaId: "", salaId: "", activa: "" };

/** Sin días no quiere decir ninguno: quiere decir todos los del rango. */
const diasDeLaGrilla = (grilla) =>
  grilla.diasSemana?.length ? grilla.diasSemana.map((d) => etiqueta(d).slice(0, 3)).join(", ") : "todos";

function InformePlan({ plan, aplicado }) {
  return (
    <Alert color={aplicado ? "green" : "gray"} mt="sm"
      title={`${aplicado ? "Se generaron" : "Se van a generar"} ${plan.generadas} funciones${
        plan.salteadas ? `, ${plan.salteadas} se ${aplicado ? "saltearon" : "saltean"}` : ""}`}>
      <ScrollArea.Autosize mah={260}>
        <Stack gap={4}>
          {plan.funciones.map((f) => f.choca ? (
            <Text key={f.inicio} size="xs" c="yellow">
              <strong>{fechaHora(f.inicio)}</strong> · {f.motivo || "se pisa con otra función"}
            </Text>
          ) : (
            <Text key={f.inicio} size="xs" c="dimmed">{fechaHora(f.inicio)}</Text>
          ))}
        </Stack>
      </ScrollArea.Autosize>
    </Alert>
  );
}

function FuncionesGeneradas({ grilla }) {
  if (!grilla.funciones?.length) {
    return <Text size="sm" c="dimmed" p="sm">Esta grilla no generó ninguna función: todas sus fechas chocaban con algo ya programado.</Text>;
  }
  return (
    <div style={{ padding: "var(--mantine-spacing-sm)" }}>
      <Text size="sm" fw={600} mb="xs">Funciones de la grilla {grilla.id} ({grilla.funciones.length})</Text>
      <Group gap={4}>
        {grilla.funciones.map((f) => <Badge key={f.id} variant="default" tt="none" fw={400}>{fechaHora(f.inicio)}</Badge>)}
      </Group>
    </div>
  );
}

// Confirmar se habilita solo con una previsualización de estos mismos datos: cambiar un campo la invalida.
function FormularioGrilla({ peliculas, salas, idiomas, proyecciones, alCrear }) {
  const avisar = useAvisar();
  const [campos, setCampos] = useState({
    peliculaId: String(peliculas[0]?.id ?? ""), salaId: String(salas[0]?.id ?? ""), desde: hoyISO(), hasta: "",
    horaInicio: "20:30", diasSemana: [], idioma: idiomas[0], proyeccion: proyecciones[0], precio: 5000,
  });
  const [informe, setInforme] = useState(null);
  const [error, setError] = useState(null);
  const cambiar = (clave) => (valor) => { setCampos((c) => ({ ...c, [clave]: valor })); setInforme(null); };
  // Vacío viaja como null: es una grilla abierta, no una fecha que falta.
  const pedido = () => ({ ...campos, hasta: campos.hasta || null });

  async function previsualizar(evento) {
    const formulario = evento.currentTarget.form;
    if (!formulario.reportValidity()) return;
    setError(null);
    try {
      setInforme({ plan: await api.previsualizarProgramacion(pedido()), aplicado: false });
    } catch (e) {
      setInforme(null);
      setError(e.message);
    }
  }

  async function confirmar(evento) {
    evento.preventDefault();
    setError(null);
    try {
      const plan = await api.crearProgramacion(pedido());
      avisar(`Grilla creada: ${plan.generadas} funciones` + (plan.salteadas ? `, ${plan.salteadas} salteadas` : ""));
      setInforme({ plan, aplicado: true });
      alCrear();
    } catch (e) {
      setError(e.message);
    }
  }

  const listo = informe && !informe.aplicado && informe.plan.generadas > 0;
  return (
    <form onSubmit={confirmar}>
      <Stack gap="sm">
        <Select label="Película" allowDeselect={false} searchable value={campos.peliculaId} onChange={cambiar("peliculaId")}
          data={peliculas.map((p) => ({ value: String(p.id), label: `${p.titulo} (${p.duracionMinutos}′)` }))} />
        <Select label="Sala" allowDeselect={false} value={campos.salaId} onChange={cambiar("salaId")}
          data={salas.map((s) => ({ value: String(s.id), label: `${s.nombre} — ${etiqueta(s.tipo)}` }))} />
        <Group grow>
          <TextInput label="Desde" type="date" required value={campos.desde} onChange={(e) => cambiar("desde")(e.currentTarget.value)} />
          <TextInput label={<>Hasta <Text span size="xs" c="dimmed">(vacío = sin fin)</Text></>} type="date" value={campos.hasta}
            onChange={(e) => cambiar("hasta")(e.currentTarget.value)} />
        </Group>
        <TextInput label="Hora de la función" type="time" required value={campos.horaInicio}
          onChange={(e) => cambiar("horaInicio")(e.currentTarget.value)} />
        <Checkbox.Group label="Días (ninguno = todos)" value={campos.diasSemana} onChange={cambiar("diasSemana")}>
          <Group gap="xs" mt={6}>
            {DIAS_SEMANA.map((d) => <Checkbox key={d} value={d} label={etiqueta(d).slice(0, 3)} size="xs" />)}
          </Group>
        </Checkbox.Group>
        <Group grow>
          <Select label="Idioma" allowDeselect={false} value={campos.idioma} onChange={cambiar("idioma")} data={opcionesDe(idiomas, etiqueta)} />
          <Select label="Proyección" allowDeselect={false} value={campos.proyeccion} onChange={cambiar("proyeccion")}
            data={opcionesDe(proyecciones, etiqueta)} />
        </Group>
        <NumberInput label="Precio base" required min={100} step={100} value={campos.precio} onChange={cambiar("precio")} />
        <Group grow>
          <Button variant="default" onClick={previsualizar}>Previsualizar</Button>
          <Button type="submit" disabled={!listo}>Confirmar</Button>
        </Group>
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
      {informe && <InformePlan {...informe} />}
    </form>
  );
}

export function Programaciones() {
  const [filtros, setFiltros] = useState(SIN_FILTROS);
  const [detalle, setDetalle] = useState(null);
  const base = useCargar(() => Promise.all([
    api.obtenerProgramaciones(), api.obtenerPeliculas(), api.obtenerSalas(), api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]), []);
  const visibles = useCargar(() => api.obtenerProgramaciones(filtros), [JSON.stringify(filtros)]);
  const recargar = () => { base.recargar(); visibles.recargar(); };
  const accion = useAccion(recargar);
  const verDetalle = useAccion();

  if (!base.datos) return <EsperaOError carga={base} />;
  const [programaciones, peliculas, salas, idiomas, proyecciones] = base.datos;
  const lista = visibles.datos || programaciones;
  const tituloDe = (id) => peliculas.find((p) => p.id === id)?.titulo || `Película ${id}`;
  const salaDe = (id) => salas.find((s) => s.id === id)?.nombre || `Sala ${id}`;
  const filtro = (clave) => (valor) => setFiltros({ ...filtros, [clave]: valor || "" });

  return (
    <>
      <Encabezado titulo="Grilla de funciones">
        Una grilla genera las funciones del rango de una sola vez. Las que chocan con algo ya programado en esa sala se
        saltean, y el informe dice cuáles.
      </Encabezado>

      <BarraFiltros visibles={lista.length} total={programaciones.length} alLimpiar={() => setFiltros(SIN_FILTROS)}>
        <Select label="Película" w={220} searchable value={filtros.peliculaId} onChange={filtro("peliculaId")}
          data={[{ value: "", label: "Todas" }, ...peliculas.map((p) => ({ value: String(p.id), label: p.titulo }))]} />
        <Select label="Sala" w={150} value={filtros.salaId} onChange={filtro("salaId")}
          data={[{ value: "", label: "Todas" }, ...salas.map((s) => ({ value: String(s.id), label: s.nombre }))]} />
        <Select label="Estado" w={170} value={filtros.activa} onChange={filtro("activa")}
          data={[{ value: "", label: "Todas" }, { value: "true", label: "Activas" }, { value: "false", label: "Dadas de baja" }]} />
      </BarraFiltros>

      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 8 }}>
          <Paper withBorder>
            <Table.ScrollContainer minWidth={620}>
              <Table verticalSpacing="xs" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Película</Table.Th><Table.Th>Sala</Table.Th><Table.Th>Cuándo</Table.Th><Table.Th>Días</Table.Th>
                    <Table.Th ta="right">Precio</Table.Th><Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {lista.length === 0 && <FilaVacia columnas={6}>Ninguna grilla coincide con el filtro.</FilaVacia>}
                  {lista.map((p) => (
                    <Table.Tr key={p.id} style={{ cursor: "pointer", opacity: p.activa ? 1 : 0.55 }}
                      onClick={() => verDetalle(async () => setDetalle(await api.obtenerProgramacion(p.id)))}>
                      <Table.Td fw={500}>{tituloDe(p.peliculaId)}</Table.Td>
                      <Table.Td style={{ whiteSpace: "nowrap" }}>{salaDe(p.salaId)}</Table.Td>
                      <Table.Td fz="xs" style={{ whiteSpace: "nowrap" }}>
                        {p.desde} {p.hasta ? `al ${p.hasta}` : <strong>en adelante</strong>} · <strong>{p.horaInicio.slice(0, 5)}</strong>
                        {!p.hasta && <Text size="xs" c="dimmed">generada hasta {p.generadaHasta || "—"}</Text>}
                      </Table.Td>
                      <Table.Td fz="xs">{diasDeLaGrilla(p)}</Table.Td>
                      <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>{precio(p.precio)}</Table.Td>
                      <Table.Td ta="right" style={{ whiteSpace: "nowrap" }} onClick={(e) => e.stopPropagation()}>
                        <Anchor component="button" size="xs" c={p.activa ? "red" : "green"}
                          onClick={() => accion(() => (p.activa ? api.darDeBajaProgramacion(p.id) : api.darDeAltaProgramacion(p.id)))}>
                          {p.activa ? "Dar de baja" : "Reactivar"}
                        </Anchor>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
            <div style={{ padding: "var(--mantine-spacing-sm)", borderTop: "1px solid var(--mantine-color-default-border)" }}>
              <Nota>
                Dar de baja una grilla <strong>no borra las funciones que ya generó</strong>: pueden tener entradas vendidas.
                Solo evita que genere nuevas. Hacé clic en una fila para ver qué funciones creó.
              </Nota>
            </div>
            {detalle && <FuncionesGeneradas grilla={detalle} />}
          </Paper>
        </Grid.Col>
        <Grid.Col span={{ base: 12, lg: 4 }}>
          <Paper withBorder p="md">
            <Title order={2} size="h4" mb="sm">Nueva grilla</Title>
            <FormularioGrilla peliculas={peliculas} salas={salas} idiomas={idiomas} proyecciones={proyecciones} alCrear={recargar} />
            <Nota mt="md">
              Al confirmar, el servidor <strong>vuelve a revisar</strong> cada fecha: entre que mirás el informe y
              confirmás, otro puede haber programado algo en esa sala.
            </Nota>
          </Paper>
        </Grid.Col>
      </Grid>
    </>
  );
}
