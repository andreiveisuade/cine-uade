import { useState } from "react";
import { Button, Group, Modal, Text } from "@mantine/core";
import { useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { useEnviar } from "@/compartido/hooks/useEnviar.js";
import { ErrorCaja } from "@/compartido/estado/ErrorCaja.jsx";
import { useSesion } from "../SesionDelCliente.jsx";
import { SeccionDeCuenta } from "./SeccionDeCuenta.jsx";

export function DarDeBaja() {
  const { setUsuario } = useSesion();
  const navegar = useNavigate();
  const avisar = useAvisar();
  const [confirmando, setConfirmando] = useState(false);
  const { enviar, enviando, error } = useEnviar();

  function pedirConfirmacion(evento) {
    evento.preventDefault();
    setConfirmando(true);
  }

  async function darDeBaja() {
    await enviar(async () => {
      try {
        await api.darDeBaja();
      } catch (e) {
        setConfirmando(false);
        throw e;
      }
      setUsuario(null);
      avisar("Cerramos tu cuenta");
      navegar("/");
    });
  }

  return (
    <SeccionDeCuenta titulo="Dar de baja la cuenta" alEnviar={pedirConfirmacion}>
      <Text size="sm" c="dimmed">Tus reservas siguen valiendo: las encontrás con el código de cada entrada.</Text>
      <Button type="submit" color="red" variant="light">Dar de baja</Button>
      {error && <ErrorCaja>{error}</ErrorCaja>}
      <Modal opened={confirmando} onClose={() => setConfirmando(false)} title="¿Dar de baja tu cuenta?">
        <Text size="sm">Se cierra tu sesión. Si volvés a entrar con Google, la cuenta se abre de nuevo con tus reservas.</Text>
        <Group justify="flex-end" mt="lg">
          <Button variant="default" onClick={() => setConfirmando(false)}>Volver</Button>
          <Button color="red" loading={enviando} onClick={darDeBaja}>Sí, dar de baja</Button>
        </Group>
      </Modal>
    </SeccionDeCuenta>
  );
}
