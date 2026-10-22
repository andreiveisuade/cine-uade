import { useState } from "react";
import { Button, Group, Modal, PasswordInput, Text } from "@mantine/core";
import { useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { faltantes } from "@/compartido/formularios/faltantes.js";
import { useSesion } from "../SesionDelCliente.jsx";
import { SeccionDeCuenta } from "./SeccionDeCuenta.jsx";

export function DarDeBaja() {
  const { setUsuario } = useSesion();
  const navegar = useNavigate();
  const avisar = useAvisar();
  const [actual, setActual] = useState("");
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState(null);
  const [faltan, setFaltan] = useState({});
  const [enviando, setEnviando] = useState(false);

  function pedirConfirmacion(evento) {
    evento.preventDefault();
    const vacios = faltantes({ actual: { valor: actual, etiqueta: "tu contraseña" } });
    setFaltan(vacios);
    if (!Object.keys(vacios).length) setConfirmando(true);
  }

  async function darDeBaja() {
    setError(null);
    setEnviando(true);
    try {
      await api.darDeBaja({ actual });
      setUsuario(null);
      avisar("Cerramos tu cuenta");
      navegar("/");
    } catch (e) {
      setError(e.message);
      setEnviando(false);
      setConfirmando(false);
    }
  }

  return (
    <SeccionDeCuenta titulo="Dar de baja la cuenta" alEnviar={pedirConfirmacion}>
      <Text size="sm" c="dimmed">Tus reservas siguen valiendo: las encontrás con el código de cada entrada.</Text>
      <PasswordInput label="Contraseña" autoComplete="current-password" required value={actual}
        error={faltan.actual} onChange={(e) => setActual(e.currentTarget.value)} />
      <Button type="submit" color="red" variant="light">Dar de baja</Button>
      {error && <ErrorCaja>{error}</ErrorCaja>}
      <Modal opened={confirmando} onClose={() => setConfirmando(false)} title="¿Dar de baja tu cuenta?">
        <Text size="sm">No vas a poder volver a entrar con ella.</Text>
        <Group justify="flex-end" mt="lg">
          <Button variant="default" onClick={() => setConfirmando(false)}>Volver</Button>
          <Button color="red" loading={enviando} onClick={darDeBaja}>Sí, dar de baja</Button>
        </Group>
      </Modal>
    </SeccionDeCuenta>
  );
}
