import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

// jsdom no trae lo que Mantine consulta al montar (tema del sistema, tamaño de los contenedores).
window.matchMedia = (query) => ({
  matches: false,
  media: query,
  onchange: null,
  addEventListener: () => {},
  removeEventListener: () => {},
  addListener: () => {},
  removeListener: () => {},
  dispatchEvent: () => false,
});

window.ResizeObserver = class {
  observe() {}
  unobserve() {}
  disconnect() {}
};

window.scrollTo = () => {};

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  sessionStorage.clear();
  localStorage.clear();
});
