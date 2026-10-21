import { Text } from "@mantine/core";
import { AnimatePresence, m } from "motion/react";
import { Vacio } from "@/compartido/estado/Vacio.jsx";
import { Destacados } from "@/compartido/layout/Destacados.jsx";
import { corre } from "./diasPromo.js";
import { TarjetaPromo } from "@/promos/tarjetas/TarjetaPromo.jsx";

// Entran escalonadas una sola vez; al filtrar, las que quedan se corren con `layout`.
const TARJETA = {
  oculta: { opacity: 0, y: 12 },
  visible: (orden) => ({ opacity: 1, y: 0, transition: { duration: 0.3, ease: "easeOut", delay: Math.min(orden, 8) * 0.05 } }),
  saliente: { opacity: 0, transition: { duration: 0.15 } },
};

export function ListaPromos({ promos, dia }) {
  const visibles = dia === "TODOS" ? promos : promos.filter((p) => corre(p, dia));
  if (promos.length === 0) return <Vacio accion={<Text size="sm" c="dimmed">Volvé pronto. Mientras, mirá los combos.</Text>}>Por ahora no hay promos</Vacio>;
  if (visibles.length === 0) return <Vacio>Ese día no hay promos.</Vacio>;
  return (
    <AnimatePresence mode="popLayout">
      <Destacados items={visibles} clave={(p) => p.nombre} min={360} render={(promo, destacada) => (
        <m.div key={promo.nombre} layout variants={TARJETA} custom={visibles.indexOf(promo)} initial="oculta" animate="visible"
          exit="saliente" style={{ height: "100%" }}>
          <TarjetaPromo promo={promo} destacada={destacada} />
        </m.div>
      )} />
    </AnimatePresence>
  );
}
