// La web del cliente: qué vista atiende cada ruta. Sin login — al cliente se lo
// reconoce por su email recién al confirmar la compra.
//
// Las rutas son las mismas que antes de React (#/pelicula/3), así que los links que ya
// circulan siguen andando. Lo que comparten las pantallas de compra —la butaca elegida y
// su tarifa— vive en compra.jsx.

import { useEffect } from "react";
import { Anchor, AppShell, Container, Group } from "@mantine/core";
import { Link, NavLink, Route, Routes, useLocation } from "react-router";
import { BotonTema } from "../componentes/BotonTema.jsx";
import { NoExiste } from "../componentes/NoExiste.jsx";
import { CompraEnCurso } from "./compra.jsx";
import { Cartelera } from "./Cartelera.jsx";
import { Pelicula } from "./Pelicula.jsx";

function Enlace({ a, children }) {
  return (
    <Anchor component={NavLink} to={a} size="sm" c="dimmed"
      style={({ isActive }) => (isActive ? { color: "var(--mantine-color-text)", fontWeight: 600 } : undefined)}>
      {children}
    </Anchor>
  );
}

export function AppCliente() {
  const { pathname } = useLocation();
  // Cada pantalla arranca arriba, como cuando eran páginas.
  useEffect(() => window.scrollTo(0, 0), [pathname]);

  return (
    <AppShell header={{ height: 60 }} padding="md">
      <AppShell.Header>
        <Container size="lg" h="100%">
          <Group h="100%" justify="space-between" wrap="nowrap">
            <Anchor component={Link} to="/" fw={800} fz="lg" c="var(--mantine-color-text)" underline="never">
              CINE UADE
            </Anchor>
            <Group gap="md" wrap="nowrap">
              <Enlace a="/mis-reservas">Mis reservas</Enlace>
              <Enlace a="/registro">Registrarme</Enlace>
              <Anchor href="admin.html" size="sm" c="dimmed" visibleFrom="sm">Acceso encargado</Anchor>
              <BotonTema />
            </Group>
          </Group>
        </Container>
      </AppShell.Header>

      <AppShell.Main>
        <Container size="lg" py="md">
          <CompraEnCurso>
            <Routes>
              <Route index element={<Cartelera />} />
              <Route path="cartelera/:genero?" element={<Cartelera />} />
              <Route path="pelicula/:id" element={<Pelicula />} />
              <Route path="*" element={<NoExiste />} />
            </Routes>
          </CompraEnCurso>
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}
