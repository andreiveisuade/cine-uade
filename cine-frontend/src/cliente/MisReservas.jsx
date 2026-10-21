import { useState } from "react";
import { Anchor, Button, Group, Paper, Stack, Text, TextInput, Title } from "@mantine/core";
import { Link, useNavigate } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { fechaHora, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { Cargando, EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { useTitulo } from "../componentes/useTitulo.js";
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
    <Stack gap="md" maw={720}>
      <Title order={1}>Mis reservas</Title>
      {carga.datos.length ? (
        carga.datos.map((r) => (
          <Paper key={r.id} withBorder p="md">
            <Group justify="space-between" align="flex-start">
              <div>
                <Text fw={600}>{r.pelicula.titulo}</Text>
                <Text size="sm" c="dimmed">
                  {fechaHora(r.funcion.inicio)} · {r.sala.nombre} · {r.cantidadEntradas} entrada
                  {r.cantidadEntradas === 1 ? "" : "s"} · {precio(r.total)}
                </Text>
                <Text size="sm">
                  {etiqueta(r.estado)} · <Anchor component={Link} to={`/ticket/${r.codigo}`} size="sm"
                    ff="monospace">{r.codigo}</Anchor>
                </Text>
              </div>
              {r.cancelable && (
                <Button variant="outline" color="red" size="xs" loading={cancelando === r.codigo}
                  onClick={() => cancelar(r.codigo)}>
                  Cancelar
                </Button>
              )}
            </Group>
            {r.cobrable && <Group mt="sm"><PagarOnline codigo={r.codigo} /></Group>}
          </Paper>
        ))
      ) : (
        <Vacio>Todavía no tenés reservas.</Vacio>
      )}
    </Stack>
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
    <Stack gap="md" maw={520}>
      <div>
        <Title order={1}>Mis reservas</Title>
        <Text size="sm" c="dimmed">
          Ingresá el código de acceso de tu ticket para verlo. Para pagarla online o cancelarla,{" "}
          <Anchor component={Link} to="/ingresar" size="sm">ingresá</Anchor> con tu cuenta.
        </Text>
      </div>
      <Paper withBorder p="md">
        <form onSubmit={buscar}>
          <Group gap="xs" align="flex-end">
            <TextInput label="Código de acceso" required placeholder="A1B2 C3D4" value={codigo} style={{ flex: 1 }}
              error={faltan.codigo}
              styles={{ input: { fontFamily: "var(--mantine-font-family-monospace)", textTransform: "uppercase", letterSpacing: "0.1em" } }}
              onChange={(e) => setCodigo(e.currentTarget.value)} />
            <Button type="submit">Buscar</Button>
          </Group>
        </form>
      </Paper>
    </Stack>
  );
}
