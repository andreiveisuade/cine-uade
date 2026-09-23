import { useState } from "react";
import { Anchor, Badge, Button, Checkbox, Grid, Group, NumberInput, Paper, Select, SimpleGrid, Stack, Table, Text,
         Textarea, TextInput, Title, UnstyledButton } from "@mantine/core";
import { useDebouncedValue } from "@mantine/hooks";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { duracion } from "../api/formato.js";
import { Chip, ChipClasificacion } from "../componentes/Chips.jsx";
import { EsperaOError } from "../componentes/Estado.jsx";
import { Poster } from "../componentes/Poster.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { BarraFiltros, Encabezado, FilaVacia, opcionesDe, useAccion } from "./comun.jsx";

const SIN_FILTROS = { q: "", genero: "", publicada: "" };

/** El mismo formulario sirve para alta y edición: sin `editando` es alta. */
function FormularioPelicula({ editando, generos, clasificaciones, alGuardar, alCancelar }) {
  const [campos, setCampos] = useState({
    titulo: editando?.titulo || "",
    duracionMinutos: editando?.duracionMinutos || "",
    anio: editando?.anio || "",
    clasificacion: editando?.clasificacion || clasificaciones[0]?.nombre,
    director: editando?.director || "",
    idiomaOriginal: editando?.idiomaOriginal || "",
    sinopsis: editando?.sinopsis || "",
    posterUrl: editando?.posterUrl || "",
    generos: editando?.generos || [],
    enCartelera: editando ? editando.enCartelera : true,
  });
  const cambiar = (clave) => (valor) => setCampos((c) => ({ ...c, [clave]: valor }));
  const texto = (clave) => (e) => cambiar(clave)(e.currentTarget.value);

  return (
    <form onSubmit={(e) => { e.preventDefault(); alGuardar({ ...campos, duracionMinutos: Number(campos.duracionMinutos) }); }}>
      <Stack gap="sm">
        <TextInput label="Título" required value={campos.titulo} onChange={texto("titulo")} />
        <Group grow>
          <NumberInput label="Duración (min)" required min={1} value={campos.duracionMinutos} onChange={cambiar("duracionMinutos")} />
          <NumberInput label="Año" min={1888} value={campos.anio} onChange={cambiar("anio")} />
        </Group>
        <Select label="Clasificación" allowDeselect={false} value={campos.clasificacion} onChange={cambiar("clasificacion")}
          data={clasificaciones.map((c) => ({
            value: c.nombre,
            label: `${etiqueta(c.nombre)}${c.edadMinima ? ` — desde ${c.edadMinima} años` : " — todo público"}`,
          }))} />
        <TextInput label="Dirección" value={campos.director} onChange={texto("director")} />
        <TextInput label="Idioma original" description="El de la película, no el de la función."
          value={campos.idiomaOriginal} onChange={texto("idiomaOriginal")} />
        <Textarea label="Sinopsis" autosize minRows={3} value={campos.sinopsis} onChange={texto("sinopsis")} />
        <TextInput label="Poster (URL)" placeholder="https://…" description="Opcional. Sin poster se muestra la inicial del título."
          value={campos.posterUrl} onChange={texto("posterUrl")} />
        <Checkbox.Group label="Géneros (al menos uno)" value={campos.generos} onChange={cambiar("generos")}>
          <SimpleGrid cols={2} spacing={6} mt={6}>
            {generos.map((g) => <Checkbox key={g} value={g} label={etiqueta(g)} size="xs" />)}
          </SimpleGrid>
        </Checkbox.Group>
        <Checkbox label="Publicada" checked={campos.enCartelera}
          onChange={(e) => cambiar("enCartelera")(e.currentTarget.checked)} />
        <Group grow>
          <Button type="submit">{editando ? "Guardar cambios" : "Agregar"}</Button>
          {editando && <Button variant="default" onClick={alCancelar}>Cancelar</Button>}
        </Group>
      </Stack>
    </form>
  );
}

export function Peliculas() {
  const [filtros, setFiltros] = useState(SIN_FILTROS);
  // El texto espera a que se deje de tipear: sin eso, "Matrix" son seis pedidos.
  const [q] = useDebouncedValue(filtros.q, 200);
  const [editandoId, setEditandoId] = useState(null);
  const base = useCargar(() => Promise.all([api.obtenerPeliculas(), api.obtenerGeneros(), api.obtenerClasificaciones()]), []);
  const visibles = useCargar(() => api.obtenerPeliculas({ ...filtros, q }), [q, filtros.genero, filtros.publicada]);
  const recargar = () => { base.recargar(); visibles.recargar(); };
  const accion = useAccion(recargar);

  if (!base.datos) return <EsperaOError carga={base} />;
  const [peliculas, generos, clasificaciones] = base.datos;
  const editando = peliculas.find((p) => p.id === editandoId) || null;
  const lista = visibles.datos || peliculas;

  async function guardar(campos) {
    const hecho = editando
      ? await accion(() => api.actualizarPelicula(editando.id, campos), "Cambios guardados")
      : await accion(() => api.crearPelicula(campos), "Película agregada");
    if (hecho !== undefined) setEditandoId(null);
  }

  return (
    <>
      <Encabezado titulo="Películas">
        {peliculas.length} cargadas · {peliculas.filter((p) => p.enCartelera).length} publicadas. Una película llega a la
        cartelera cuando tiene funciones por delante; despublicarla la baja aunque las tenga.
      </Encabezado>

      <BarraFiltros visibles={lista.length} total={peliculas.length} alLimpiar={() => setFiltros(SIN_FILTROS)}>
        <TextInput label="Buscar" type="search" placeholder="título" w={240} value={filtros.q}
          onChange={(e) => setFiltros({ ...filtros, q: e.currentTarget.value })} />
        <Select label="Género" w={180} value={filtros.genero} onChange={(v) => setFiltros({ ...filtros, genero: v || "" })}
          data={[{ value: "", label: "Todos" }, ...opcionesDe(generos, etiqueta)]} />
        <Select label="Estado" w={170} value={filtros.publicada} onChange={(v) => setFiltros({ ...filtros, publicada: v || "" })}
          data={[{ value: "", label: "Todas" }, { value: "true", label: "Publicadas" }, { value: "false", label: "Despublicadas" }]} />
      </BarraFiltros>

      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 8 }}>
          <Paper withBorder>
            <Table.ScrollContainer minWidth={640}>
              <Table verticalSpacing="xs" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th w={48} /><Table.Th>Título</Table.Th><Table.Th>Duración</Table.Th><Table.Th>Edad</Table.Th>
                    <Table.Th>Géneros</Table.Th><Table.Th>Estado</Table.Th><Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {lista.length === 0 && <FilaVacia columnas={7}>Ninguna película coincide con el filtro.</FilaVacia>}
                  {lista.map((p) => (
                    <Table.Tr key={p.id} bg={p.id === editandoId ? "var(--mantine-color-yellow-light)" : undefined}>
                      <Table.Td><Poster pelicula={p} w={32} /></Table.Td>
                      <Table.Td>
                        <Text size="sm" fw={500}>{p.titulo}</Text>
                        <Text size="xs" c="dimmed">{[p.anio || null, p.director || null].filter(Boolean).join(" · ")}</Text>
                      </Table.Td>
                      <Table.Td style={{ whiteSpace: "nowrap" }}>{duracion(p.duracionMinutos)}</Table.Td>
                      <Table.Td><ChipClasificacion valor={p.clasificacion} /></Table.Td>
                      <Table.Td><Group gap={4}>{p.generos.map((g) => <Chip key={g} valor={g} />)}</Group></Table.Td>
                      <Table.Td>
                        <UnstyledButton
                          title={p.enCartelera ? "Publicada: aparece en la cartelera si tiene funciones por delante"
                                               : "Despublicada: no aparece aunque tenga funciones"}
                          onClick={() => accion(() => api.actualizarPelicula(p.id, { enCartelera: !p.enCartelera }),
                                                p.enCartelera ? "Despublicada" : "Publicada")}>
                          <Badge variant="light" color={p.enCartelera ? "green" : "gray"} tt="none" miw="max-content" style={{ cursor: "pointer" }}>
                            {p.enCartelera ? "Publicada" : "Despublicada"}
                          </Badge>
                        </UnstyledButton>
                      </Table.Td>
                      <Table.Td style={{ whiteSpace: "nowrap" }} ta="right">
                        <Anchor component="button" size="xs" onClick={() => setEditandoId(p.id)}>Editar</Anchor>
                        <Anchor component="button" size="xs" c="red" ml="sm"
                          onClick={() => accion(() => api.eliminarPelicula(p.id), "Película borrada")}>Borrar</Anchor>
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
            <Title order={2} size="h4" mb="sm">{editando ? `Editar ${editando.titulo}` : "Nueva película"}</Title>
            <FormularioPelicula key={editandoId ?? "nueva"} editando={editando} generos={generos}
              clasificaciones={clasificaciones} alGuardar={guardar} alCancelar={() => setEditandoId(null)} />
          </Paper>
        </Grid.Col>
      </Grid>
    </>
  );
}
