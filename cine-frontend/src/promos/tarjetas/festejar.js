export function festejar(evento) {
  const { left, top, width, height } = evento.currentTarget.getBoundingClientRect();
  // Se baja recién cuando alguien toca: casi nadie la usa.
  import("canvas-confetti").then(({ default: lanzar }) => lanzar({
    particleCount: 80, spread: 70, startVelocity: 35, disableForReducedMotion: true,
    origin: { x: (left + width / 2) / window.innerWidth, y: (top + height / 2) / window.innerHeight },
  }));
}
