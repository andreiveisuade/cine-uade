import { Anchor, Text } from "@mantine/core";
import { useNavigate } from "react-router";
import * as api from "@/api/api-http.js";
import { useAvisar } from "@/compartido/estado/Avisos.jsx";
import { useSesion } from "@/cuenta/SesionDelCliente.jsx";
import { Enlace } from "./Enlace.jsx";

export function EnlacesDeCuenta({ alElegir }) {
  const { usuario, setUsuario } = useSesion();
  const navegar = useNavigate();
  const avisar = useAvisar();

  async function salir() {
    try {
      await api.salir();
      setUsuario(null);
      alElegir?.();
      navegar("/");
    } catch (e) {
      avisar(e.message, "error");
    }
  }

  // Mientras se pregunta por la sesión no se muestra ninguna de las dos barras: evita el parpadeo.
  if (usuario === undefined) return null;
  if (!usuario) {
    return (
      <Enlace a="/ingresar" alElegir={alElegir}>Ingresar</Enlace>
    );
  }
  return (
    <>
      <Text size="sm" fw={650}>{usuario.nombre}</Text>
      <Enlace a="/mis-reservas" alElegir={alElegir}>Mis reservas</Enlace>
      <Enlace a="/mi-cuenta" alElegir={alElegir}>Mi cuenta</Enlace>
      <Anchor component="button" size="sm" fw={500} c="dimmed" onClick={salir}>Salir</Anchor>
    </>
  );
}
