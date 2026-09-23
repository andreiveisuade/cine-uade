// El panel del encargado: qué vista atiende cada ruta y quién puede entrar.
//
// Las rutas son las mismas que antes de React (admin.html#/funciones). El menú pasó de una
// barra de trece links a una columna agrupada por tarea, que es como se usa el panel: la
// cartelera se arma de vez en cuando, las ventas y la caja todos los días.

import { useEffect } from "react";
import { Anchor, AppShell, Badge, Burger, Button, Group, NavLink as EnlaceMenu, ScrollArea, Text } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { Navigate, NavLink, Outlet, Route, Routes, useLocation } from "react-router";
import { etiqueta } from "../api/etiquetas.js";
import { BotonTema } from "../componentes/BotonTema.jsx";
import { NoExiste } from "../componentes/NoExiste.jsx";
import { useSesion } from "./sesion.jsx";
import { Login } from "./Login.jsx";
import { Funcion } from "./Funcion.jsx";
import { Funciones } from "./Funciones.jsx";
import { Salas } from "./Salas.jsx";
import { Importador } from "./Importador.jsx";
import { Pendientes } from "./Pendientes.jsx";
import { Peliculas } from "./Peliculas.jsx";

const MENU = [
  ["Cartelera", [["/peliculas", "Películas"], ["/pendientes", "Por revisar"], ["/importador", "Importador"]]],
  ["Programación", [["/salas", "Salas"], ["/funciones", "Funciones"], ["/programaciones", "Grilla"],
                    ["/planificador", "Planificador"], ["/agenda", "Agenda"]]],
  ["Ventas", [["/reservas", "Reservas"], ["/promociones", "Promociones"], ["/candy", "Candy"], ["/caja", "Caja"]]],
];

function Menu({ alElegir }) {
  const { esAdministrador } = useSesion();
  const enlace = ([a, texto]) => (
    <EnlaceMenu key={a} component={NavLink} to={a} label={texto} onClick={alElegir} />
  );
  return (
    <>
      {/* El acomodador solo ve Puerta: el rol no es solo cosmético, además la ruta se cierra. */}
      {esAdministrador && MENU.map(([grupo, enlaces]) => (
        <div key={grupo}>
          <Text size="xs" fw={700} c="dimmed" tt="uppercase" px="sm" mt="md" mb={4}>{grupo}</Text>
          {enlaces.map(enlace)}
        </div>
      ))}
      <Text size="xs" fw={700} c="dimmed" tt="uppercase" px="sm" mt="md" mb={4}>Acceso</Text>
      {enlace(["/puerta", "Puerta"])}
    </>
  );
}

/** Todo lo que no es el login: pide sesión, y el acomodador solo pasa a Puerta. */
function Panel() {
  const { empleado, esAdministrador, cerrar } = useSesion();
  const { pathname } = useLocation();
  const [abierto, { toggle, close }] = useDisclosure();
  useEffect(() => window.scrollTo(0, 0), [pathname]);

  if (!empleado) return <Navigate to="/login" replace />;
  if (!esAdministrador && !pathname.startsWith("/puerta")) return <Navigate to="/puerta" replace />;

  return (
    <AppShell header={{ height: 60 }} navbar={{ width: 220, breakpoint: "sm", collapsed: { mobile: !abierto } }}
      padding="lg">
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger opened={abierto} onClick={toggle} hiddenFrom="sm" size="sm" />
            <Text fw={800} fz="lg">CINE UADE</Text>
            <Badge variant="filled" color="dark">{etiqueta(empleado.rol)}</Badge>
          </Group>
          <Group gap="sm" wrap="nowrap">
            <Text size="sm" c="dimmed" visibleFrom="sm">{empleado.nombre}</Text>
            <Anchor href="index.html" size="sm" c="dimmed" visibleFrom="sm">Ver cartelera</Anchor>
            <Button variant="default" size="xs" onClick={cerrar}>Salir</Button>
            <BotonTema />
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="xs">
        <ScrollArea>
          <Menu alElegir={close} />
        </ScrollArea>
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}

function Inicio() {
  const { esAdministrador } = useSesion();
  return <Navigate to={esAdministrador ? "/peliculas" : "/puerta"} replace />;
}

export function AppAdmin() {
  return (
    <Routes>
      <Route path="login" element={<Login />} />
      <Route element={<Panel />}>
        <Route index element={<Inicio />} />
        <Route path="peliculas" element={<Peliculas />} />
        <Route path="pendientes" element={<Pendientes />} />
        <Route path="importador" element={<Importador />} />
        <Route path="salas/:id?" element={<Salas />} />
        <Route path="funciones/:destacada?" element={<Funciones />} />
        <Route path="funcion/:id" element={<Funcion />} />
        <Route path="*" element={<NoExiste />} />
      </Route>
    </Routes>
  );
}
