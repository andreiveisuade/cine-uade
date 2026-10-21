import { useState } from "react";
import { Anchor, Badge, Button, Group, Paper, Stack, Text, TextInput } from "@mantine/core";
import { Link, useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { etiqueta } from "@/api/etiquetas.js";
import { fechaHora, precio } from "@/api/formato.js";
import { useAvisar } from "@/componentes/Avisos.jsx";
import { Cargando, EsperaOError, Vacio } from "@/componentes/Estado.jsx";
import { Encabezado } from "@/componentes/Encabezado.jsx";
import { useCargar } from "@/componentes/useCargar.js";
import { useTitulo } from "@/componentes/useTitulo.js";
import { faltantes } from "./compra.jsx";
import { PagarOnline } from "./PagarOnline.jsx";
import { useSesion } from "./sesion.jsx";

export function MisReservas() {
  useTitulo("Mis reservas");
  const { usuario } = useSesion();
  if (usuario === undefined) return <Cargando />;
  return usuario ? <ReservasDelCliente /> : <BuscarPorCodigo />;
}

// Qué se puede hacer con cada una lo dicen cobrable y cancelable: la regla vive en el backend.
function ReservasDelCliente() {
  const avisar = useAvisar();
  const [cancelando, setCancelando] = useState(null);
  const carga = useCargar(() => api.obtenerMisReservas(), []);
  if (!carga.datos) return <EsperaOError carga={carga} />;

  async function cancelar(codigo) {
    setCancelando(codigo);
    try {
      await api.cancelarReservaPorCodigo(codigo);
      avisar("Reserva cancelada, las butacas quedaron libres");
      carga.recargar();
    } catch (e) {
      avisar(e.message, "error");
    }
    setCancelando(null);
  }

  return (
    <div>
      <Encabezado titulo="Mis reservas" />
      {carga.datos.length ? (
        <Stack gap="sm" maw={880}>
          {carga.datos.map((r) => (
            <FilaReserva key={r.id} reserva={r} cancelando={cancelando === r.codigo} cancelar={() => cancelar(r.codigo)} />
          ))}
        </Stack>
      ) : (
        <Vacio accion={<Button component={Link} to="/cartelera" variant="default">Ver la cartelera</Button>}>
          Todavía no tenés reservas.
        </Vacio>
      )}
    </div>
  );
}

const COLOR_ESTADO = { RESERVADA: "ambar", PAGADA: "green", EXPIRADA: "gray", CANCELADA: "gray" };

// Una fila por reserva: la fecha como bloque a la izquierda, lo que es en el medio y lo que se puede hacer a la derecha.
function FilaReserva({ reserva: r, cancelando, cancelar }) {
  const [diaMes, mes] = [r.funcion.inicio.slice(8, 10), r.funcion.inicio.slice(5, 7)];
  return (
    <Paper withBorder p="md" style={{ opacity: ["EXPIRADA", "CANCELADA"].includes(r.estado) ? 0.65 : 1 }}>
      <Group justify="space-between" align="center" gap="md">
        <Group gap="md" wrap="nowrap" align="center">
          <Stack gap={0} align="center" w={56} py={6} bg="var(--cine-superficie-2)" style={{ borderRadius: "var(--mantine-radius-md)", flexShrink: 0 }}>
            <Text className="cine-display cine-cifras" fz={28} fw={850} lh={1}>{diaMes}</Text>
            <Text size="xs" c="dimmed" className="cine-cifras">/{mes}</Text>
          </Stack>
          <Stack gap={4}>
            <Group gap="sm">
              <Text fw={700} fz="lg" lh={1.2}>{r.pelicula.titulo}</Text>
              <Badge color={COLOR_ESTADO[r.estado]} variant={r.estado === "RESERVADA" ? "light" : "filled"}>{etiqueta(r.estado)}</Badge>
            </Group>
            <Text size="sm" c="dimmed" className="cine-cifras">
              {fechaHora(r.funcion.inicio)} · {r.sala.nombre} · {r.cantidadEntradas} entrada{r.cantidadEntradas === 1 ? "" : "s"} · {precio(r.total)}
            </Text>
          </Stack>
        </Group>
        <Group gap="xs">
          <Button component={Link} to={`/ticket/${r.codigo}`} variant="default" size="sm" ff="monospace">{r.codigo}</Button>
          {r.cancelable && (
            <Button variant="subtle" color="red" size="sm" loading={cancelando} onClick={cancelar}>Cancelar</Button>
          )}
        </Group>
      </Group>
      {r.cobrable && <Group mt="md"><PagarOnline codigo={r.codigo} /></Group>}
    </Paper>
  );
}

// Sin sesión el ticket se busca por código: el email no prueba ser el dueño, el código sí.
function BuscarPorCodigo() {
  const navegar = useNavigate();
  const [codigo, setCodigo] = useState("");
  const [faltan, setFaltan] = useState({});

  function buscar(evento) {
    evento.preventDefault();
    const vacios = faltantes({ codigo: { valor: codigo, etiqueta: "el código de tu ticket" } });
    setFaltan(vacios);
    if (vacios.codigo) return;
    navegar(`/ticket/${encodeURIComponent(codigo.replace(/\s/g, "").toUpperCase())}`);
  }

  return (
    <Stack gap={0} maw={560}>
      <Encabezado titulo="Mis reservas">
        <Text c="dimmed">
          Ingresá el código de acceso de tu ticket para verlo. Para pagarla online o cancelarla,{" "}
          <Anchor component={Link} to="/ingresar">ingresá</Anchor> con tu cuenta.
        </Text>
      </Encabezado>
      <Paper withBorder p="lg">
        <form onSubmit={buscar}>
          <Group gap="xs" align="flex-end">
            <TextInput label="Código de acceso" required placeholder="A1B2 C3D4" value={codigo} style={{ flex: 1 }}
              error={faltan.codigo}
              styles={{ input: { fontFamily: "var(--mantine-font-family-monospace)", textTransform: "uppercase", letterSpacing: "0.1em" } }}
              onChange={(e) => setCodigo(e.currentTarget.value)} />
            <Button type="submit" size="sm">Buscar</Button>
          </Group>
        </form>
      </Paper>
    </Stack>
  );
}
