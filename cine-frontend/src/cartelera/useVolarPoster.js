import { useReducedMotion } from "motion/react";
import { useNavigate } from "react-router";

const ESPERA_MAX_MS = 500;

// HashRouter no tiene el `viewTransition` de los data routers: la navegación se envuelve a mano. El nombre se pone
// solo durante el salto, porque dos pósters con el mismo nombre a la vez cortan la transición.
export function useVolarPoster() {
  const navegar = useNavigate();
  const quieto = useReducedMotion();
  return (id, a) => (e) => {
    if (quieto || !document.startViewTransition || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey) return;
    e.preventDefault();
    const nombre = `poster-${id}`;
    const selector = `[data-poster="${id}"]`;
    const desde = e.currentTarget.querySelector(selector) ?? document.querySelector(selector);
    if (desde) desde.style.viewTransitionName = nombre;
    let hasta;
    const salto = document.startViewTransition(async () => {
      if (desde) desde.style.viewTransitionName = "";
      navegar(a, { state: { vuela: true } });
      hasta = await aparece(selector, desde);
      if (hasta) hasta.style.viewTransitionName = nombre;
    });
    salto.finished.finally(() => hasta && (hasta.style.viewTransitionName = ""));
  };
}

// La pantalla nueva carga sus datos: se espera un rato corto a que el póster esté; si no llega, queda el fundido.
// Con setTimeout: mientras dura el callback el navegador no pinta y requestAnimationFrame no corre.
function aparece(selector, desde) {
  const limite = performance.now() + ESPERA_MAX_MS;
  return new Promise((listo) => {
    const mirar = () => {
      const el = [...document.querySelectorAll(selector)].find((x) => x !== desde && x.getClientRects().length);
      if (el || performance.now() > limite) listo(el);
      else setTimeout(mirar, 16);
    };
    mirar();
  });
}
