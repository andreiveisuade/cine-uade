import { useEffect, useState } from "react";
import { Alert, Anchor, Button, Grid, Group, NumberInput, Paper, Select, Stack, Table, Text, TextInput,
         Title } from "@mantine/core";
import { Link, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, hora, precio } from "../api/formato.js";
import { EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { BarraFiltros, Encabezado, FilaVacia, opcionesDe, useAccion } from "./comun.jsx";

const SIN_FILTROS = { peliculaId: "", salaId: "", desde: "", hasta: "" };

// R8 y R3 los valida el backend; acá se anticipan para no mandar algo que va a fallar.
function revisarReglas({ peliculaId, salaId, inicio }, { peliculas, salas, funciones, soporta3D }) {
  const sala = salas.find((s) => s.id === Number(salaId));
  const pelicula = peliculas.find((p) => p.id === Number(peliculaId));
  if (!sala || !pelicula) return { avisos: [], puede3D: true };
  const avisos = [];
  const puede3D = soporta3D.get(sala.tipo);
  if (!puede3D) avisos.push(["R8", `${sala.nombre} es ${etiqueta(sala.tipo)} y no puede proyectar en 3D.`]);

  if (inicio) {
    const desde = new Date(inicio);
    const hasta = new Date(desde.getTime() + pelicula.duracionMinutos * 60000);
    const pisada = funciones.filter((f) => f.salaId === sala.id).find((f) => {
      const otraDesde = new Date(f.inicio);
      const otraHasta = new Date(otraDesde.getTime() + f.pelicula.duracionMinutos * 60000);
      return desde < otraHasta && otraDesde < hasta;
    });
    if (pisada) avisos.push(["R3", `Se pisa con ${pisada.pelicula.titulo} de las ${hora(pisada.inicio)}.`]);
    else avisos.push(["ok", `Termina ${hora(hasta)} · sala libre en ese rango.`]);
  }
  return { avisos, puede3D };
}

function FormularioFuncion({ datos, alProgramar }) {
  const { peliculas, salas, idiomas, proyecciones } = datos;
  const [campos, setCampos] = useState({
    peliculaId: String(peliculas[0]?.id ?? ""), salaId: String(salas[0]?.id ?? ""), inicio: "",
    idioma: idiomas[0], proyeccion: proyecciones[0], precio: "",
  });
  const cambiar = (clave) => (valor) => setCampos((c) => ({ ...c, [clave]: valor }));
  const { avisos, puede3D } = revisarReglas(campos, datos);

  useEffect(() => {
    if (!puede3D && campos.proyeccion === "TRES_D") setCampos((c) => ({ ...c, proyeccion: "DOS_D" }));
  }, [puede3D, campos.proyeccion]);

  return (
    <form onSubmit={(e) => { e.preventDefault(); alProgramar(campos); }}>
      <Stack gap="sm">
        <Select label="Película" allowDeselect={false} searchable value={campos.peliculaId} onChange={cambiar("peliculaId")}
          data={peliculas.map((p) => ({ value: String(p.id), label: `${p.titulo} (${p.duracionMinutos}′)` }))} />
        <Select label="Sala" allowDeselect={false} value={campos.salaId} onChange={cambiar("salaId")}
          data={salas.map((s) => ({ value: String(s.id), label: `${s.nombre} — ${etiqueta(s.tipo)}` }))} />
        <TextInput label="Inicio" type="datetime-local" required value={campos.inicio}
          onChange={(e) => cambiar("inicio")(e.currentTarget.value)} />
        <Group grow>
          <Select label="Idioma" allowDeselect={false} value={campos.idioma} onChange={cambiar("idioma")}
            data={opcionesDe(idiomas, etiqueta)} />
          <Select label="Proyección" allowDeselect={false} value={campos.proyeccion} onChange={cambiar("proyeccion")}
            data={proyecciones.map((p) => ({ value: p, label: etiqueta(p), disabled: p === "TRES_D" && !puede3D }))} />
        </Group>
        <NumberInput label="Precio base" required min={100} step={100} value={campos.precio} onChange={cambiar("precio")} />
        {avisos.map(([regla, texto]) => (
          <Alert key={regla} color={regla === "ok" ? "green" : "yellow"} p="xs">
            <Text size="xs">{regla !== "ok" && <strong>{regla} · </strong>}{texto}</Text>
          </Alert>
        ))}
        <Button type="submit">Programar</Button>
      </Stack>
    </form>
  );
}

// `destacada`: la fila a la que saltar al llegar desde la agenda.
export function Funciones() {
  const { destacada } = useParams();
  const [filtros, setFiltros] = useState(SIN_FILTROS);
  const base = useCargar(() => Promise.all([
    api.obtenerFunciones(), api.obtenerPeliculas(), api.obtenerSalas(),
    api.obtenerTiposSala(), api.obtenerIdiomas(), api.obtenerProyecciones(),
  ]), []);
  const visibles = useCargar(() => api.obtenerFunciones(filtros), [JSON.stringify(filtros)]);
  const recargar = () => { base.recargar(); visibles.recargar(); };
  const accion = useAccion(recargar);

  // El salto va después de pintar la tabla: antes de eso la fila no existe.
  const listas = !!base.datos;
  useEffect(() => {
    if (listas && destacada) document.getElementById(`funcion-${destacada}`)?.scrollIntoView({ block: "center" });
  }, [listas, destacada]);

  if (!base.datos) return <EsperaOError carga={base} />;
  const [funciones, peliculas, salas, tipos, idiomas, proyecciones] = base.datos;
  const datos = { funciones, peliculas, salas, idiomas, proyecciones,
                  soporta3D: new Map(tipos.map((t) => [t.nombre, t.soportaTresD])) };
  const lista = visibles.datos || funciones;
  const filtro = (clave) => (valor) => setFiltros({ ...filtros, [clave]: valor || "" });

  return (
    <>
      <Encabezado titulo="Funciones">
        {funciones.length} programadas. Es la lista más larga del panel: una semana de seis salas pasa de cien funciones.
      </Encabezado>

      <BarraFiltros visibles={lista.length} total={funciones.length} alLimpiar={() => setFiltros(SIN_FILTROS)}>
        <Select label="Película" w={220} searchable value={filtros.peliculaId} onChange={filtro("peliculaId")}
          data={[{ value: "", label: "Todas" }, ...peliculas.map((p) => ({ value: String(p.id), label: p.titulo }))]} />
        <Select label="Sala" w={150} value={filtros.salaId} onChange={filtro("salaId")}
          data={[{ value: "", label: "Todas" }, ...salas.map((s) => ({ value: String(s.id), label: s.nombre }))]} />
        <TextInput label="Desde" type="date" value={filtros.desde} onChange={(e) => filtro("desde")(e.currentTarget.value)} />
        <TextInput label="Hasta" type="date" value={filtros.hasta} onChange={(e) => filtro("hasta")(e.currentTarget.value)} />
      </BarraFiltros>

      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 8 }}>
          <Paper withBorder>
            <Table.ScrollContainer minWidth={620}>
              <Table verticalSpacing="xs" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Cuándo</Table.Th><Table.Th>Película</Table.Th><Table.Th>Sala</Table.Th>
                    <Table.Th>Formato</Table.Th><Table.Th ta="right">Precio</Table.Th><Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {lista.length === 0 && <FilaVacia columnas={6}>Ninguna función coincide con el filtro.</FilaVacia>}
                  {lista.map((f) => (
                    <Table.Tr key={f.id} id={`funcion-${f.id}`}
                      bg={String(f.id) === destacada ? "var(--mantine-color-yellow-light)" : undefined}>
                      <Table.Td style={{ whiteSpace: "nowrap" }}>{dia(f.inicio)} <strong>{hora(f.inicio)}</strong></Table.Td>
                      <Table.Td>{f.pelicula.titulo}</Table.Td>
                      <Table.Td style={{ whiteSpace: "nowrap" }}>{f.sala.nombre}</Table.Td>
                      <Table.Td style={{ whiteSpace: "nowrap" }}>{etiqueta(f.proyeccion)} · {etiqueta(f.idioma)}</Table.Td>
                      <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>{precio(f.precio)}</Table.Td>
                      <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>
                        <Anchor component={Link} to={`/funcion/${f.id}`} size="xs" fw={500}>Informes</Anchor>
                        <Anchor component="button" size="xs" c="red" ml="sm"
                          onClick={() => accion(() => api.eliminarFuncion(f.id), "Función borrada")}>Borrar</Anchor>
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
            <Title order={2} size="h4" mb="sm">Programar función</Title>
            <FormularioFuncion datos={datos}
              alProgramar={(campos) => accion(() => api.programarFuncion(campos), "Función programada")} />
          </Paper>
        </Grid.Col>
      </Grid>
    </>
  );
}
