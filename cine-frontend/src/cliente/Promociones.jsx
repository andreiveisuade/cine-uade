import { useState } from "react";
import { Chip, Group, Skeleton, Stack, Text } from "@mantine/core";
import { AnimatePresence, m } from "motion/react";
import * as api from "../api/api-http.js";
import { etiqueta } from "../api/etiquetas.js";
import { Destacados } from "../componentes/Destacados.jsx";
import { Encabezado } from "../componentes/Encabezado.jsx";
import { EsperaOError, Vacio } from "../componentes/Estado.jsx";
import { Grilla } from "../componentes/Grilla.jsx";
import { Seccion } from "../componentes/Seccion.jsx";
import { corre, DIAS, TarjetaPromo } from "../componentes/TarjetaPromo.jsx";
import { TarjetaCombo } from "../componentes/TarjetaCombo.jsx";
import { TarjetaProducto } from "../componentes/TarjetaProducto.jsx";
import { useCargar } from "../componentes/useCargar.js";
import { useTitulo } from "../componentes/useTitulo.js";

// Entran escalonadas una sola vez; al filtrar, las que quedan se corren con `layout`.
const TARJETA = {
  oculta: { opacity: 0, y: 12 },
  visible: (orden) => ({ opacity: 1, y: 0, transition: { duration: 0.3, ease: "easeOut", delay: Math.min(orden, 8) * 0.05 } }),
  saliente: { opacity: 0, transition: { duration: 0.15 } },
};

function FiltroDias({ dia, setDia }) {
  return (
    <Chip.Group value={dia} onChange={setDia}>
      <Group gap={6} aria-label="Filtrar por día">
        <Chip value="TODOS" size="sm">Todos</Chip>
        {DIAS.map((d) => <Chip key={d} value={d} size="sm">{etiqueta(d).slice(0, 3)}</Chip>)}
      </Group>
    </Chip.Group>
  );
}

function Promos({ promos, dia }) {
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

function Candy({ productos }) {
  const combos = productos.filter((p) => p.esCombo);
  const sueltos = productos.filter((p) => !p.esCombo);
  if (productos.length === 0) return <Vacio>La carta del candy está vacía por ahora.</Vacio>;
  return (
    <Stack gap="xl">
      <Destacados items={combos} clave={(c) => c.id} min={360}
        render={(combo, destacada) => <TarjetaCombo combo={combo} destacada={destacada} />} />
      {sueltos.length > 0 && (
        <Grilla min="clamp(150px, 15vw, 220px)" gap="var(--mantine-spacing-md)">
          {sueltos.map((p) => <TarjetaProducto key={p.id} producto={p} />)}
        </Grilla>
      )}
    </Stack>
  );
}

export function Promociones() {
  useTitulo("Promos");
  const [dia, setDia] = useState("TODOS");
  const promos = useCargar(() => api.obtenerPromocionesVigentes(), []);
  const carta = useCargar(() => api.obtenerProductosCandy(), []);

  return (
    <Stack gap="var(--cine-seccion)">
      <div>
        <Encabezado titulo="Promos" bajada="No se acumulan: va la que más te conviene." />
        <Stack gap="lg" mt="-md">
          <FiltroDias dia={dia} setDia={setDia} />
          {promos.datos
            ? <Promos promos={promos.datos} dia={dia} />
            : <EsperaOError carga={promos} esqueleto={<Skeleton h={360} radius="lg" />} />}
        </Stack>
      </div>
      <Seccion titulo="Candy" bajada="Se compra en el mostrador antes de entrar a la sala.">
        {carta.datos
          ? <Candy productos={carta.datos} />
          : <EsperaOError carga={carta} esqueleto={<Skeleton h={260} radius="lg" />} />}
      </Seccion>
    </Stack>
  );
}
