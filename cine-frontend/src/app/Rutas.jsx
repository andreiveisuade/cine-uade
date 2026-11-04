import { m } from "motion/react";
import { Route, Routes, useLocation } from "react-router";
import { Cartelera } from "@/cartelera/Cartelera.jsx";
import { Pelicula } from "@/cartelera/pelicula/Pelicula.jsx";
import { Funcion } from "@/compra/funcion/Funcion.jsx";
import { TuCompra } from "@/compra/tu-compra/TuCompra.jsx";
import { Ticket } from "@/ticket/Ticket.jsx";
import { MisReservas } from "@/cuenta/reservas/MisReservas.jsx";
import { Ingresar } from "@/cuenta/Ingresar.jsx";
import { MiCuenta } from "@/cuenta/MiCuenta.jsx";
import { Nosotros } from "@/institucional/Nosotros.jsx";
import { Promociones } from "@/promos/Promociones.jsx";
import { PasarelaEmulada } from "@/pasarela/PasarelaEmulada.jsx";
import { NoExiste } from "./NoExiste.jsx";

// Solo entrada: con una salida en AnimatePresence, volver a la sección mientras se iba la dejaba en opacidad 0.
const PANTALLA = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.2, ease: "easeOut" } },
};

// Una URL, una pantalla.
export function Rutas() {
  const ubicacion = useLocation();
  // La clave es la sección, no la ruta entera: cambiar de género en la cartelera no la remonta y deja reordenar.
  const seccion = ubicacion.pathname.split("/")[1];
  const pantalla = (
    <Routes location={ubicacion}>
      <Route index element={<Cartelera conHero />} />
      <Route path="cartelera/:genero?" element={<Cartelera />} />
      <Route path="pelicula/:id" element={<Pelicula />} />
      <Route path="funcion/:id" element={<Funcion />} />
      <Route path="compra/:codigo" element={<TuCompra />} />
      <Route path="ticket/:codigo" element={<Ticket />} />
      <Route path="mis-reservas" element={<MisReservas />} />
      <Route path="ingresar" element={<Ingresar />} />
      <Route path="mi-cuenta" element={<MiCuenta />} />
      <Route path="promociones" element={<Promociones />} />
      <Route path="nosotros" element={<Nosotros />} />
      <Route path="pasarela-emulada/:id" element={<PasarelaEmulada />} />
      <Route path="*" element={<NoExiste />} />
    </Routes>
  );
  // Si vuela un póster, la pantalla cambia de golpe: el fundido lo hace la View Transition.
  if (ubicacion.state?.vuela) return <div key={seccion}>{pantalla}</div>;
  return <m.div key={seccion} {...PANTALLA}>{pantalla}</m.div>;
}
