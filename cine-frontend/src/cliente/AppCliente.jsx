import { useEffect } from "react";
import { AnimatePresence, m } from "motion/react";
import { Anchor, AppShell, Container, Group, Text } from "@mantine/core";
import { Link, NavLink, Route, Routes, useLocation, useNavigate } from "react-router";
import * as api from "../api/api-http.js";
import { useAvisar } from "../componentes/Avisos.jsx";
import { BotonTema } from "../componentes/BotonTema.jsx";
import { ErrorDePantalla } from "../componentes/ErrorDePantalla.jsx";
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
import { Promociones } from "./Promociones.jsx";

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

// Corta y sin desplazamiento grande: acompaña el cambio de pantalla sin demorarlo.
const PANTALLA = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.2, ease: "easeOut" } },
  exit: { opacity: 0, transition: { duration: 0.12 } },
};

export function AppCliente() {
  const ubicacion = useLocation();
  const { pathname } = ubicacion;
  // La clave es la sección, no la ruta entera: cambiar de género en la cartelera no la remonta y deja reordenar.
  const seccion = pathname.split("/")[1];
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
                <Enlace a="/promociones">Promos</Enlace>
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
              <ErrorDePantalla>
                <AnimatePresence mode="wait" initial={false}>
                  <m.div key={seccion} {...PANTALLA}>
                    <Routes location={ubicacion}>
                      <Route index element={<Cartelera conHero />} />
                      <Route path="cartelera/:genero?" element={<Cartelera />} />
                      <Route path="pelicula/:id" element={<Pelicula />} />
                      <Route path="funcion/:id" element={<Funcion />} />
                      <Route path="confirmar/:id" element={<Confirmar />} />
                      <Route path="ticket/:codigo" element={<Ticket />} />
                      <Route path="mis-reservas" element={<MisReservas />} />
                      <Route path="registro" element={<Registro />} />
                      <Route path="ingresar" element={<Ingresar />} />
                      <Route path="promociones" element={<Promociones />} />
                      <Route path="nosotros" element={<Nosotros />} />
                      <Route path="*" element={<NoExiste />} />
                    </Routes>
                  </m.div>
                </AnimatePresence>
              </ErrorDePantalla>
            </CompraEnCurso>
          </Container>
        </AppShell.Main>
      </AppShell>
    </SesionDelCliente>
  );
}
