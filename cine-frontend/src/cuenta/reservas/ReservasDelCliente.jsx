import { useState } from "react";
import { Button, Stack } from "@mantine/core";
import { Link } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { EsperaOError } from "@/compartido/estado/EsperaOError.jsx";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { useCargar } from "@/compartido/hooks/useCargar.js";
import { Encabezado } from "@/compartido/layout/Encabezado.jsx";
import { FilaReserva } from "./FilaReserva.jsx";

// Qué se puede hacer con cada una lo dicen cobrable y cancelable: la regla vive en el backend.
export function ReservasDelCliente() {
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
            <FilaReserva key={r.id} reserva={r} cancelando={cancelando === r.codigo} alCancelar={() => cancelar(r.codigo)} />
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
