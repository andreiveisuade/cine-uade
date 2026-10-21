import { Anchor, Box, Burger, Drawer, Group, Stack, Text, UnstyledButton } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { Link, NavLink, useNavigate } from "react-router";
import * as api from "../api/api-http.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { BotonTema } from "../componentes/BotonTema.jsx";
import { Contenedor } from "../componentes/Contenedor.jsx";
import { useSesion } from "./sesion.jsx";

function Enlace({ a, children, alElegir }) {
  return (
    <Anchor component={NavLink} to={a} size="sm" fw={500} c="dimmed" underline="never" onClick={alElegir}
      style={({ isActive }) => (isActive ? { color: "var(--mantine-color-text)" } : undefined)}>
      {children}
    </Anchor>
  );
}

function EnlacesDeCuenta({ alElegir }) {
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
      <>
        <Enlace a="/ingresar" alElegir={alElegir}>Ingresar</Enlace>
        <Enlace a="/registro" alElegir={alElegir}>Registrarme</Enlace>
      </>
    );
  }
  return (
    <>
      <Text size="sm" fw={650}>{usuario.nombre}</Text>
      <Enlace a="/mis-reservas" alElegir={alElegir}>Mis reservas</Enlace>
      <Anchor component="button" size="sm" fw={500} c="dimmed" onClick={salir}>Salir</Anchor>
    </>
  );
}

function Enlaces({ alElegir }) {
  return (
    <>
      <Enlace a="/cartelera" alElegir={alElegir}>Cartelera</Enlace>
      <Enlace a="/promociones" alElegir={alElegir}>Promos</Enlace>
      <Enlace a="/nosotros" alElegir={alElegir}>Nosotros</Enlace>
    </>
  );
}

export function Marca() {
  return (
    <UnstyledButton component={Link} to="/" className="cine-display" fz={26} fw={850} lh={1} aria-label="Cine UADE, inicio">
      CINE <Box component="span" c="var(--mantine-color-ambar-text)">UADE</Box>
    </UnstyledButton>
  );
}

// En celular los enlaces no entran en una fila: van a un panel lateral que se cierra al elegir.
export function Barra() {
  const [abierto, { toggle, close }] = useDisclosure(false);
  return (
    <Contenedor h="100%">
      <Group h="100%" justify="space-between" wrap="nowrap">
        <Group gap="xl" wrap="nowrap">
          <Marca />
          <Group gap="lg" wrap="nowrap" visibleFrom="sm"><Enlaces /></Group>
        </Group>
        <Group gap="lg" wrap="nowrap">
          <Group gap="lg" wrap="nowrap" visibleFrom="sm"><EnlacesDeCuenta /></Group>
          <BotonTema />
          <Burger opened={abierto} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Abrir el menú" />
        </Group>
      </Group>
      <Drawer opened={abierto} onClose={close} position="right" size="xs" title={<Marca />} hiddenFrom="sm">
        <Stack gap="lg" pt="md" fz="lg">
          <Enlaces alElegir={close} />
          <EnlacesDeCuenta alElegir={close} />
        </Stack>
      </Drawer>
    </Contenedor>
  );
}
