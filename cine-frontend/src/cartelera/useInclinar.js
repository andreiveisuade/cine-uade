import { useMotionTemplate, useMotionValue, useReducedMotion, useSpring, useTransform } from "motion/react";

const MAX_GRADOS = 6;
const RESORTE = { stiffness: 200, damping: 20 };

// Inclinación leve hacia el cursor y un reflejo que lo sigue. Solo con mouse: en táctil no hay hover que deshacer.
export function useInclinar() {
  const quieto = useReducedMotion();
  const x = useMotionValue(0.5);
  const y = useMotionValue(0.5);
  const rotateY = useSpring(useTransform(x, [0, 1], [-MAX_GRADOS, MAX_GRADOS]), RESORTE);
  const rotateX = useSpring(useTransform(y, [0, 1], [MAX_GRADOS, -MAX_GRADOS]), RESORTE);
  const brillo = useMotionValue(0);
  const reflejo = useMotionTemplate`radial-gradient(circle at ${useTransform(x, (v) => v * 100)}% ${useTransform(y, (v) => v * 100)}%, rgb(255 255 255 / ${brillo}), transparent 60%)`;
  const conMouse = (e) => !quieto && e.pointerType === "mouse" && matchMedia("(hover: hover) and (pointer: fine)").matches;
  return {
    estilo: { rotateX, rotateY, transformPerspective: 800 },
    reflejo,
    eventos: {
      onPointerMove: (e) => {
        if (!conMouse(e)) return;
        const r = e.currentTarget.getBoundingClientRect();
        x.set((e.clientX - r.left) / r.width);
        y.set((e.clientY - r.top) / r.height);
        brillo.set(0.12);
      },
      onPointerLeave: () => { x.set(0.5); y.set(0.5); brillo.set(0); },
    },
  };
}
