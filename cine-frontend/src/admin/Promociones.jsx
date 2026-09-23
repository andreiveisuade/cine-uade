import { useState } from "react";
import { Anchor, Button, Checkbox, Grid, Group, NumberInput, Paper, Select, Stack, Table, TextInput, Title } from "@mantine/core";
import * as api from "../api/api-http.js";
import { DIAS_SEMANA, etiqueta } from "../api/etiquetas.js";
import { hoyISO, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ErrorCaja, EsperaOError } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { Encabezado, FilaVacia, Nota, useAccion } from "./comun.jsx";

function beneficioDe(promocion) {
  if (promocion.tipo === "PORCENTAJE") return `${promocion.porcentaje}% off`;
  if (promocion.tipo === "MONTO_FIJO") return `${precio(promocion.monto)} off`;
  return `${promocion.lleva}x${promocion.paga}`;
}

function condicionesDe(promocion) {
  const partes = [];
  if (promocion.diasSemana?.length) partes.push(promocion.diasSemana.map((d) => etiqueta(d).slice(0, 3)).join(", "));
  if (promocion.horaDesde || promocion.horaHasta) {
    partes.push(`${(promocion.horaDesde || "00:00").slice(0, 5)}–${(promocion.horaHasta || "23:59").slice(0, 5)}`);
  }
  if (promocion.mediosPago?.length) partes.push(promocion.mediosPago.map(etiqueta).join(", "));
  // Sin condiciones no quiere decir "ninguna": quiere decir que corre siempre.
  return partes.length ? partes.join(" · ") : "todos los días, cualquier medio";
}

const NUEVA = {
  nombre: "", tipo: "PORCENTAJE", porcentaje: 30, monto: 2000, lleva: 2, paga: 1,
  vigenciaDesde: hoyISO(), vigenciaHasta: "", diasSemana: [], horaDesde: "", horaHasta: "", mediosPago: [],
};

function FormularioPromocion({ mediosPago, alCrear }) {
  const avisar = useAvisar();
  const [campos, setCampos] = useState(NUEVA);
  const [error, setError] = useState(null);
  const cambiar = (clave) => (valor) => setCampos((c) => ({ ...c, [clave]: valor }));
  const texto = (clave) => (e) => cambiar(clave)(e.currentTarget.value);
  const { tipo } = campos;

  async function crear(evento) {
    evento.preventDefault();
    try {
      await api.crearPromocion({
        nombre: campos.nombre, tipo,
        porcentaje: tipo === "PORCENTAJE" ? Number(campos.porcentaje) : null,
        monto: tipo === "MONTO_FIJO" ? Number(campos.monto) : null,
        lleva: tipo === "NXM" ? Number(campos.lleva) : null,
        paga: tipo === "NXM" ? Number(campos.paga) : null,
        vigenciaDesde: campos.vigenciaDesde, vigenciaHasta: campos.vigenciaHasta,
        diasSemana: campos.diasSemana,
        horaDesde: campos.horaDesde || null, horaHasta: campos.horaHasta || null,
        mediosPago: campos.mediosPago,
      });
      avisar("Promoción creada");
      setCampos(NUEVA);
      setError(null);
      alCrear();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <form onSubmit={crear}>
      <Stack gap="sm">
        <TextInput label="Nombre" required placeholder="Miércoles 2x1" value={campos.nombre} onChange={texto("nombre")} />
        <Select label="Tipo" allowDeselect={false} value={tipo} onChange={cambiar("tipo")}
          data={[{ value: "PORCENTAJE", label: "Porcentaje" }, { value: "MONTO_FIJO", label: "Monto fijo" }, { value: "NXM", label: "NxM (2x1)" }]} />
        {tipo === "PORCENTAJE" && (
          <NumberInput label="Porcentaje de descuento" min={1} max={99} value={campos.porcentaje} onChange={cambiar("porcentaje")} />
        )}
        {tipo === "MONTO_FIJO" && <NumberInput label="Monto a descontar" min={1} value={campos.monto} onChange={cambiar("monto")} />}
        {tipo === "NXM" && (
          <Group grow>
            <NumberInput label="Lleva" min={2} value={campos.lleva} onChange={cambiar("lleva")} />
            <NumberInput label="Paga" min={1} value={campos.paga} onChange={cambiar("paga")} />
          </Group>
        )}
        <Group grow>
          <TextInput label="Desde" type="date" required value={campos.vigenciaDesde} onChange={texto("vigenciaDesde")} />
          <TextInput label="Hasta" type="date" required value={campos.vigenciaHasta} onChange={texto("vigenciaHasta")} />
        </Group>
        <Checkbox.Group label="Días (ninguno = todos)" value={campos.diasSemana} onChange={cambiar("diasSemana")}>
          <Group gap="xs" mt={6}>
            {DIAS_SEMANA.map((d) => <Checkbox key={d} value={d} label={etiqueta(d).slice(0, 3)} size="xs" />)}
          </Group>
        </Checkbox.Group>
        <Group grow>
          <TextInput label="Desde hora" type="time" value={campos.horaDesde} onChange={texto("horaDesde")} />
          <TextInput label="Hasta hora" type="time" value={campos.horaHasta} onChange={texto("horaHasta")} />
        </Group>
        <Checkbox.Group label="Medios (ninguno = cualquiera)" value={campos.mediosPago} onChange={cambiar("mediosPago")}>
          <Group gap="xs" mt={6}>
            {mediosPago.map((m) => <Checkbox key={m.nombre} value={m.nombre} label={etiqueta(m.nombre)} size="xs" />)}
          </Group>
        </Checkbox.Group>
        <Button type="submit">Crear promoción</Button>
        {error && <ErrorCaja>{error}</ErrorCaja>}
      </Stack>
    </form>
  );
}

export function Promociones() {
  const carga = useCargar(() => Promise.all([api.obtenerPromociones(), api.obtenerMediosPago()]), []);
  const accion = useAccion(carga.recargar);
  if (!carga.datos) return <EsperaOError carga={carga} />;
  const [promociones, mediosPago] = carga.datos;

  return (
    <>
      <Encabezado titulo="Promociones">
        {promociones.filter((p) => p.activa).length} activas de {promociones.length}. No se acumulan: en cada cobro se aplica
        la que más descuenta.
      </Encabezado>
      <Grid gap="md" align="flex-start">
        <Grid.Col span={{ base: 12, lg: 8 }}>
          <Paper withBorder>
            <Table.ScrollContainer minWidth={600}>
              <Table verticalSpacing="xs">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Nombre</Table.Th><Table.Th>Beneficio</Table.Th><Table.Th>Vigencia</Table.Th>
                    <Table.Th>Cuándo</Table.Th><Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {promociones.length === 0 && <FilaVacia columnas={5}>Todavía no hay promociones.</FilaVacia>}
                  {promociones.map((p) => (
                    <Table.Tr key={p.id} style={{ opacity: p.activa ? 1 : 0.55 }}>
                      <Table.Td fw={500}>{p.nombre}</Table.Td>
                      <Table.Td fw={600} style={{ whiteSpace: "nowrap" }}>{beneficioDe(p)}</Table.Td>
                      <Table.Td fz="xs" style={{ whiteSpace: "nowrap" }}>{p.vigenciaDesde} al {p.vigenciaHasta}</Table.Td>
                      <Table.Td fz="xs">{condicionesDe(p)}</Table.Td>
                      <Table.Td ta="right" style={{ whiteSpace: "nowrap" }}>
                        <Anchor component="button" size="xs" c={p.activa ? "red" : "green"}
                          onClick={() => accion(() => (p.activa ? api.darDeBajaPromocion(p.id) : api.darDeAltaPromocion(p.id)))}>
                          {p.activa ? "Dar de baja" : "Reactivar"}
                        </Anchor>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
            <Nota p="sm" style={{ borderTop: "1px solid var(--mantine-color-default-border)" }}>
              Las promociones no se borran: se dan de baja. Una que ya se usó en un cobro tiene que seguir existiendo para poder
              explicar por qué se cobró ese monto.
            </Nota>
          </Paper>
        </Grid.Col>
        <Grid.Col span={{ base: 12, lg: 4 }}>
          <Paper withBorder p="md">
            <Title order={2} size="h4" mb="sm">Nueva promoción</Title>
            <FormularioPromocion mediosPago={mediosPago} alCrear={carga.recargar} />
            <Nota mt="md">
              Las condiciones se evalúan contra el horario de la <strong>función</strong>, no contra el momento de la compra:
              un 2x1 de los miércoles vale para la función del miércoles aunque las entradas se compren el lunes.
            </Nota>
          </Paper>
        </Grid.Col>
      </Grid>
    </>
  );
}
