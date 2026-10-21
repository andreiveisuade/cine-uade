import { useEffect } from "react";
import { AnimatePresence, m } from "motion/react";
import { AppShell } from "@mantine/core";
import { Route, Routes, useLocation } from "react-router";
import { Contenedor } from "../componentes/Contenedor.jsx";
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
import { SesionDelCliente } from "./sesion.jsx";
import { Barra } from "./Barra.jsx";
import { Pie } from "./Pie.jsx";
import { Nosotros } from "./Nosotros.jsx";
import { Promociones } from "./Promociones.jsx";

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
      <AppShell header={{ height: 64 }}>
        <AppShell.Header style={{ borderColor: "var(--cine-borde)" }}>
          <Barra />
        </AppShell.Header>

        <AppShell.Main style={{ display: "flex", flexDirection: "column" }}>
          <Contenedor pt="var(--cine-gutter)" w="100%" style={{ flex: 1 }}>
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
          </Contenedor>
          <Pie />
        </AppShell.Main>
      </AppShell>
    </SesionDelCliente>
  );
}
