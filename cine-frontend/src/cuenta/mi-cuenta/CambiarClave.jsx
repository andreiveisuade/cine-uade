import { useState } from "react";
import { Button, PasswordInput } from "@mantine/core";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { enfocarPrimero, faltantes } from "@/compartido/formularios/faltantes.js";
import { useSesion } from "../SesionDelCliente.jsx";
import { SeccionDeCuenta } from "./SeccionDeCuenta.jsx";

export function CambiarClave() {
  const { usuario, setUsuario } = useSesion();
  const avisar = useAvisar();
  const [actual, setActual] = useState("");
  const [nueva, setNueva] = useState("");
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function cambiar(evento) {
    evento.preventDefault();
    const vacios = faltantes({
      actual: { valor: actual, etiqueta: "tu contraseña actual" },
      nueva: { valor: nueva, etiqueta: "la contraseña nueva" },
    });
    setFaltan(vacios);
    enfocarPrimero(evento.currentTarget, vacios);
    if (Object.keys(vacios).length) return;
    setError(null);
    setEnviando(true);
    try {
      await api.cambiarClave({ actual, nueva });
      setActual("");
      setNueva("");
      avisar("Cambiaste tu contraseña");
      // Se le vuelve a preguntar al backend: así se apaga el aviso de clave temporal.
      if (usuario.debeCambiarClave) setUsuario(await api.sesionActual());
    } catch (e) {
      setError(e.message);
    }
    setEnviando(false);
  }

  return (
    <SeccionDeCuenta titulo="Cambiar contraseña" alEnviar={cambiar}>
      <PasswordInput label="Contraseña actual" autoComplete="current-password" required value={actual}
        name="actual" error={faltan.actual} onChange={(e) => setActual(e.currentTarget.value)} />
      <PasswordInput label="Contraseña nueva" autoComplete="new-password" required value={nueva}
        name="nueva" error={faltan.nueva} onChange={(e) => setNueva(e.currentTarget.value)} />
      <Button type="submit" loading={enviando}>Cambiar contraseña</Button>
      {error && <ErrorCaja>{error}</ErrorCaja>}
    </SeccionDeCuenta>
  );
}
