import { useEffect } from "react";
import { Anchor, AppShell, Container, Group, Text } from "@mantine/core";
import { Link, NavLink, Route, Routes, useLocation, useNavigate } from "react-router";
import * as api from "../api/api-http.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { BotonTema } from "../componentes/BotonTema.jsx";
import { NoExiste } from "../componentes/NoExiste.jsx";
import { CompraEnCurso } from "./compra.jsx";
import { Cartelera } from "./Cartelera.jsx";
import { Pelicula } from "./Pelicula.jsx";
import { Funcion } from "./Funcion.jsx";
import { Confirmar } from "./Confirmar.jsx";
import { Ticket } from "./Ticket.jsx";
import { MisReservas } from "./MisReservas.jsx";
import { Registro } from "./Registro.jsx";
import { Ingresar } from "./Ingresar.jsx";
import { SesionDelCliente, useSesion } from "./sesion.jsx";
import { Nosotros } from "./Nosotros.jsx";

function Enlace({ a, children }) {
  return (
    <Anchor component={NavLink} to={a} size="sm" c="dimmed"
      style={({ isActive }) => (isActive ? { color: "var(--mantine-color-text)", fontWeight: 600 } : undefined)}>
      {children}
    </Anchor>
  );
}

function EnlacesDeCuenta() {
  const { usuario, setUsuario } = useSesion();
  const navegar = useNavigate();
  const avisar = useAvisar();

  async function salir() {
    try {
      await api.salir();
      setUsuario(null);
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
        <Enlace a="/ingresar">Ingresar</Enlace>
        <Enlace a="/registro">Registrarme</Enlace>
      </>
    );
  }
  return (
    <>
      <Text size="sm" fw={600}>{usuario.nombre}</Text>
      <Enlace a="/mis-reservas">Mis reservas</Enlace>
      <Anchor component="button" size="sm" c="dimmed" onClick={salir}>Salir</Anchor>
    </>
  );
}

export function AppCliente() {
  const { pathname } = useLocation();
  // Cada pantalla arranca arriba, como una página.
  useEffect(() => window.scrollTo(0, 0), [pathname]);

  return (
    <SesionDelCliente>
      <AppShell header={{ height: 60 }} padding="md">
        <AppShell.Header>
          <Container size="lg" h="100%">
            <Group h="100%" justify="space-between" wrap="nowrap">
              <Anchor component={Link} to="/" fw={800} fz="lg" c="var(--mantine-color-text)" underline="never">
                CINE UADE
              </Anchor>
              <Group gap="md" wrap="nowrap">
                <Enlace a="/nosotros">Nosotros</Enlace>
                <EnlacesDeCuenta />
                <BotonTema />
              </Group>
            </Group>
          </Container>
        </AppShell.Header>
  
        <AppShell.Main>
          <Container size="lg" py="md">
            <CompraEnCurso>
              <Routes>
                <Route index element={<Cartelera conHero />} />
                <Route path="cartelera/:genero?" element={<Cartelera />} />
                <Route path="pelicula/:id" element={<Pelicula />} />
                <Route path="funcion/:id" element={<Funcion />} />
                <Route path="confirmar/:id" element={<Confirmar />} />
                <Route path="ticket/:codigo" element={<Ticket />} />
                <Route path="mis-reservas" element={<MisReservas />} />
                <Route path="registro" element={<Registro />} />
                <Route path="ingresar" element={<Ingresar />} />
                <Route path="nosotros" element={<Nosotros />} />
                <Route path="*" element={<NoExiste />} />
              </Routes>
            </CompraEnCurso>
          </Container>
        </AppShell.Main>
      </AppShell>
    </SesionDelCliente>
  );
}
