import { useState } from "react";
import { Button, Group, Paper, Stack, Text, TextInput, Title } from "@mantine/core";
import { Link, useNavigate, useParams } from "react-router";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { dia, fechaHora, hora, precio } from "../api/formato.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { ChipEstado } from "../componentes/Chips.jsx";
import { Cargando, ErrorCaja, Vacio } from "../componentes/Estado.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { clienteRecordado } from "./compra.jsx";

function TarjetaReserva({ reserva: r, alCancelar }) {
  const [cancelando, setCancelando] = useState(false);
  return (
    <Paper withBorder p="md" style={{ opacity: r.estado === "CANCELADA" ? 0.6 : 1 }}>
      <Group justify="space-between" align="flex-start">
        <div>
          <Text fw={600}>{r.pelicula?.titulo || "—"}</Text>
          {r.funcion && (
            <Text size="sm" c="dimmed">
              {dia(r.funcion.inicio)} {hora(r.funcion.inicio)} · {r.sala.nombre} ({etiqueta(r.sala.tipo)}) ·{" "}
              {etiqueta(r.funcion.proyeccion)} · {etiqueta(r.funcion.idioma)}
            </Text>
          )}
        </div>
        <Stack gap={4} align="flex-end">
          <ChipEstado valor={r.estado} />
          <Text fw={600}>{precio(r.total)}</Text>
        </Stack>
      </Group>
      <Text size="sm" mt="xs">
        <Text span c="dimmed">Butacas: </Text>
        <Text span ff="monospace">{r.entradas.map((e) => e.codigo).join(", ")}</Text>
      </Text>
      {r.pago && (
        <Text size="xs" c="dimmed">Pagada con {etiqueta(r.pago.medio)} el {fechaHora(r.pago.fecha)}</Text>
      )}
      <Group gap="xs" mt="sm">
        <Button component={Link} to={`/ticket/${r.id}`} variant="default" size="xs">Ver ticket</Button>
        {r.estado === "RESERVADA" && (
          // Cancelar tarda: deshabilitado mientras tanto, dos clics apurados no son dos pedidos.
          <Button variant="outline" color="red" size="xs" loading={cancelando}
            onClick={async () => { setCancelando(true); await alCancelar(r.id); setCancelando(false); }}>
            Cancelar
          </Button>
        )}
      </Group>
    </Paper>
  );
}

// El cliente no inicia sesión: recupera sus reservas con el email que dejó al comprar.
export function MisReservas() {
  const { email: emailBuscado } = useParams();
  const navegar = useNavigate();
  const avisar = useAvisar();
  // Sin email en la URL, se usa el del navegador: quien ya compró no vuelve a tipearlo.
  const email = emailBuscado || clienteRecordado()?.email || "";
  const [texto, setTexto] = useState(email);
  const carga = useCargar(() => (email ? api.obtenerReservasDe(email) : Promise.resolve(null)), [email]);

  async function cancelar(id) {
    try {
      await api.cancelarReserva(id);
      avisar("Reserva cancelada, las butacas quedaron libres");
    } catch (e) {
      avisar(e.message, "error");
    }
    carga.recargar();
  }

  let resultado = null;
  if (carga.error) resultado = <ErrorCaja>{carga.error}</ErrorCaja>;
  else if (carga.cargando && !carga.datos) resultado = email ? <Cargando /> : null;
  else if (carga.datos && !carga.datos.length) resultado = <Vacio>No hay reservas a nombre de {email}.</Vacio>;
  else if (carga.datos) {
    resultado = (
      <Stack gap="sm">
        {carga.datos.map((r) => <TarjetaReserva key={r.id} reserva={r} alCancelar={cancelar} />)}
      </Stack>
    );
  }

  return (
    <Stack gap="md" maw={760}>
      <div>
        <Title order={1}>Mis reservas</Title>
        <Text size="sm" c="dimmed">Buscá con el email que dejaste al comprar.</Text>
      </div>
      <form onSubmit={(e) => { e.preventDefault(); navegar(`/mis-reservas/${encodeURIComponent(texto.trim())}`); }}>
        <Group gap="xs" align="flex-end">
          <TextInput type="email" required placeholder="tu@email.com" value={texto}
            onChange={(e) => setTexto(e.currentTarget.value)} style={{ flex: 1, minWidth: 220 }} />
          <Button type="submit">Buscar</Button>
        </Group>
      </form>
      {resultado}
    </Stack>
  );
}
