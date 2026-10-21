import { AnimatePresence, m } from "motion/react";
import { Route, Routes, useLocation } from "react-router";
import { Cartelera } from "@/cartelera/Cartelera.jsx";
import { Pelicula } from "@/cartelera/pelicula/Pelicula.jsx";
import { Funcion } from "@/compra/funcion/Funcion.jsx";
import { Confirmar } from "@/compra/confirmar/Confirmar.jsx";
import { Ticket } from "@/ticket/Ticket.jsx";
import { MisReservas } from "@/cuenta/reservas/MisReservas.jsx";
import { Registro } from "@/cuenta/Registro.jsx";
import { Ingresar } from "@/cuenta/Ingresar.jsx";
import { Nosotros } from "@/cliente/Nosotros.jsx";
import { Promociones } from "@/cliente/Promociones.jsx";
import { NoExiste } from "./NoExiste.jsx";

// Corta y sin desplazamiento grande: acompaña el cambio de pantalla sin demorarlo.
const PANTALLA = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.2, ease: "easeOut" } },
  exit: { opacity: 0, transition: { duration: 0.12 } },
};

// Una URL, una pantalla.
export function Rutas() {
  const ubicacion = useLocation();
  // La clave es la sección, no la ruta entera: cambiar de género en la cartelera no la remonta y deja reordenar.
  const seccion = ubicacion.pathname.split("/")[1];
  return (
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
  );
}
