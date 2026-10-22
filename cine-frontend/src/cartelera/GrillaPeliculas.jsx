import { AnimatePresence, m } from "motion/react";
import { Grilla } from "@/compartido/layout/Grilla.jsx";
import { PreviaTrailer } from "./PreviaTrailer.jsx";
import { TarjetaPelicula } from "./TarjetaPelicula.jsx";
import { usePrevia } from "./usePrevia.js";

// Entran escalonadas al aparecer en pantalla; al filtrar, las que quedan se corren a su lugar nuevo con `layout`.
const TARJETA = {
  oculta: { opacity: 0, y: 12 },
  visible: (orden) => ({ opacity: 1, y: 0, transition: { duration: 0.25, delay: Math.min(orden, 8) * 0.04 } }),
  saliente: { opacity: 0, scale: 0.95, transition: { duration: 0.15 } },
};

export function GrillaPeliculas({ peliculas }) {
  const previa = usePrevia();
  return (
    <Grilla min="clamp(150px, 20vw, 260px)" gap="var(--mantine-spacing-xl) var(--mantine-spacing-lg)">
      <AnimatePresence mode="popLayout" initial={false}>
        {peliculas.map((p, orden) => (
          <m.div key={p.id} layout custom={orden} variants={TARJETA} initial="oculta" whileInView="visible" viewport={{ once: true }} exit="saliente"
            whileHover={{ y: -4 }} transition={{ layout: { duration: 0.25 }, y: { duration: 0.15 } }}
            onMouseEnter={() => previa.entrar(p)} onMouseLeave={previa.salir}>
            <TarjetaPelicula pelicula={p} previa={previa.actual === p.id && <PreviaTrailer id={p.trailerYoutube} />} />
          </m.div>
        ))}
      </AnimatePresence>
    </Grilla>
  );
}
